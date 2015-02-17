/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved. No information contained herein may be reproduced or
 * transmitted in any form or by any means without the express written permission of Typesafe, Inc.
 */

import Tests._

name := "akka-conductr-bundle-lib"

libraryDependencies ++= List(
  Library.akkaCluster,
  Library.akkaTestkit     % "test",
  Library.scalaTest       % "test"
)

fork in Test := true

def groupByFirst(tests: Seq[TestDefinition]) =
  tests
    .groupBy(t => t.name.drop(t.name.indexOf("WithEnv")))
    .map {
      case ("WithEnvForHost", t) =>
        new Group("WithEnvForHost", t, SubProcess(ForkOptions(envVars = Map(
          "BUNDLE_HOST_IP" -> "10.0.1.10",
          "BUNDLE_SYSTEM" -> "some-system",
          "AKKA_REMOTE_HOST_PROTOCOL" -> "tcp",
          "AKKA_REMOTE_HOST_PORT" -> "10000",
          "AKKA_REMOTE_OTHER_PROTOCOLS" -> "",
          "AKKA_REMOTE_OTHER_IPS" -> "",
          "AKKA_REMOTE_OTHER_PORTS" -> ""))))
      case ("WithEnvForOneOther", t) =>
        new Group("WithEnvForOneOther", t, SubProcess(ForkOptions(envVars = Map(
          "BUNDLE_HOST_IP" -> "10.0.1.10",
          "BUNDLE_SYSTEM" -> "some-system",
          "AKKA_REMOTE_HOST_PROTOCOL" -> "tcp",
          "AKKA_REMOTE_HOST_PORT" -> "10000",
          "AKKA_REMOTE_OTHER_PROTOCOLS" -> "udp",
          "AKKA_REMOTE_OTHER_IPS" -> "10.0.1.11",
          "AKKA_REMOTE_OTHER_PORTS" -> "10001"))))
      case ("WithEnvForOthers", t) =>
        new Group("WithEnvForOthers", t, SubProcess(ForkOptions(envVars = Map(
          "BUNDLE_HOST_IP" -> "10.0.1.10",
          "BUNDLE_SYSTEM" -> "some-system",
          "AKKA_REMOTE_HOST_PROTOCOL" -> "tcp",
          "AKKA_REMOTE_HOST_PORT" -> "10000",
          "AKKA_REMOTE_OTHER_PROTOCOLS" -> "udp:tcp",
          "AKKA_REMOTE_OTHER_IPS" -> "10.0.1.11:10.0.1.12",
          "AKKA_REMOTE_OTHER_PORTS" -> "10001:10000"))))
      case (x, t) =>
        new Group("WithoutEnv", t, SubProcess(ForkOptions()))
    }.toSeq

testGrouping in Test <<= (definedTests in Test).map(groupByFirst)
