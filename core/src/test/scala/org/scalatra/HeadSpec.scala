//package org.scalatra
//
//import test.specs2.ScalatraSpec
//
//class HeadSpec extends ScalatraSpec { def is =
//  "A HEAD request should"                              ^
//    "return no body"                                   ! noBody^
//    "preserve headers"                                 ! preserveHeaders^
//                                                       end
//
//  val servletHolder = addServlet(classOf[HeadSpecServlet], "/*")
//
//  def noBody = head("/") { response.body must_== "" }
//
//  def preserveHeaders = head("/") {
//    header("X-Powered-By") must_== "caffeine"
//  }
//}
//
//class HeadSpecServlet extends ScalatraServlet {
//  get("/") {
//    response.addHeader("X-Powered-By", "caffeine")
//    "poof -- watch me disappear"
//  }
//}
