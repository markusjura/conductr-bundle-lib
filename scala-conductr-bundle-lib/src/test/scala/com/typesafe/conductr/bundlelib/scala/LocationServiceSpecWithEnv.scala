/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.scala

import akka.http.Http
import akka.http.model.headers.{ CacheDirectives, `Cache-Control`, Location }
import akka.http.model.{ HttpEntity, Uri, HttpResponse, StatusCodes }
import akka.http.server.Directives._
import akka.stream.FlowMaterializer
import akka.testkit.TestProbe
import com.typesafe.conductr.AkkaUnitTest
import java.net.{ URI, URL, InetSocketAddress }
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

class LocationServiceSpecWithEnv extends AkkaUnitTest("LocationServiceSpecWithEnv", "akka.loglevel = INFO") {

  "The LocationService functionality in the library" should {
    "be able to look up a named service" in {
      import system.dispatcher
      val serviceUri = "http://service_interface:4711/known"
      withServerWithKnownService(serviceUri) {
        val service = LocationService.lookup("/known")
        Await.result(service, timeout.duration) shouldBe Some(new URI(serviceUri) -> None)
      }
    }

    "be able to look up a named service and return maxAge" in {
      import system.dispatcher
      val serviceUri = "http://service_interface:4711/known"
      withServerWithKnownService(serviceUri, Some(10)) {
        val service = LocationService.lookup("/known")
        Await.result(service, timeout.duration) should be(Some(new URI(serviceUri) -> Some(10.seconds)))
      }
    }

    "get back None for an unknown service" in {
      import system.dispatcher
      val serviceUrl = "http://service_interface:4711/known"
      withServerWithKnownService(serviceUrl) {
        val service = LocationService.lookup("/unknown")
        Await.result(service, timeout.duration) shouldBe None
      }
    }

    "Conveniently map to an Option[URI]" in {
      import system.dispatcher
      val serviceUri = "http://service_interface:4711/known"
      withServerWithKnownService(serviceUri, Some(10)) {
        val service = LocationService.lookup("/known").map(LocationService.toUri)
        Await.result(service, timeout.duration) should be(Some(new URI(serviceUri)))
      }
    }
  }

  def withServerWithKnownService(serviceUrl: String, maxAge: Option[Int] = None)(thunk: => Unit): Unit = {
    import system.dispatcher
    implicit val materializer = FlowMaterializer()

    val probe = new TestProbe(system)

    val url = new URL(Env.serviceLocator.get)
    val server = Http(system).bind(url.getHost, url.getPort, settings = None)
    val mm = server.startHandlingWith(
      path("services" / Rest) { serviceName =>
        get {
          complete {
            serviceName match {
              case "known" =>
                val uri = Uri(serviceUrl)
                val headers = Location(uri) :: (maxAge match {
                  case Some(maxAgeSecs) =>
                    `Cache-Control`(
                      CacheDirectives.`private`(Location.name),
                      CacheDirectives.`max-age`(maxAgeSecs)) :: Nil
                  case None =>
                    Nil
                })
                HttpResponse(StatusCodes.TemporaryRedirect, headers, HttpEntity(s"Located at $uri"))
              case _ =>
                HttpResponse(StatusCodes.NotFound)
            }
          }
        }
      })

    try {
      server.localAddress(mm).onComplete {
        case Success(localAddress) => probe.ref ! localAddress
        case Failure(e)            => probe.ref ! e
      }

      val address = probe.expectMsgType[InetSocketAddress]
      address.getHostString should be(url.getHost)
      address.getPort should be(url.getPort)

      thunk
    } finally {
      server.unbind(mm)
    }

  }
}
