/*
 * Copyright © 2014-2015 Typesafe, Inc. All rights reserved.
 * No information contained herein may be reproduced or transmitted in any form
 * or by any means without the express written permission of Typesafe, Inc.
 */

package com.typesafe.conductr.bundlelib.akka

import com.typesafe.conductr.AkkaUnitTest

class ClusterPropertiesSpec extends AkkaUnitTest("ClusterPropertiesSpec", "akka.loglevel = INFO") {

  ClusterProperties.initialize()

  "The ClusterProperties functionality in the library" should {
    "return no seed properties when running in development mode" in {
      sys.props.get("akka.cluster.seed-nodes.0") shouldBe None
      sys.props.get("akka.remote.netty.tcp.port") shouldBe None
    }
  }
}
