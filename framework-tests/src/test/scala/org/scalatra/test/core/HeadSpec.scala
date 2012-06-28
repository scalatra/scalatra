package org.scalatra

import test.NettyBackend
import test.specs2.ScalatraSpec

abstract class HeadSpec extends ScalatraSpec { def is =
  "A HEAD request should"                              ^
    "return no body"                                   ! noBody^
    "preserve headers"                                 ! preserveHeaders^
                                                       end

  mount(new HeadSpecApp)

  def noBody = head("/") { response.body must_== "" }

  def preserveHeaders = head("/") {
    headers("X-Powered-By") must_== "caffeine"
  }
}

class HeadSpecApp extends ScalatraApp {
  get("/") {
    response.addHeader("X-Powered-By", "caffeine")
    "poof -- watch me disappear"
  }
}

class NettyHeadSpec extends HeadSpec with NettyBackend
