package org.scalatra
package auth
package strategy

import test.specs2._

import org.specs2.mock.Mockito
import test.NettyBackend

abstract class BasicAuthStrategySpec extends MutableScalatraSpec with Mockito {
  "params on a request with no auth headers" should {
    val httpRequest = mock[HttpRequest]
    val basicAuthRequest = new BasicAuthStrategy.BasicAuthRequest(httpRequest)
    "return None" in { // https://github.com/scalatra/scalatra/issues/143
      basicAuthRequest.params must_== None
    }
  }
}

class NettyBasicAuthStrategySpec extends BasicAuthStrategySpec with NettyBackend
