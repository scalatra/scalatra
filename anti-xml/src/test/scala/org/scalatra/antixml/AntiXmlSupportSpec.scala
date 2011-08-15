package org.scalatra
package antixml

import specs2.ScalatraSpec
import com.codecommit.antixml._

class AntiXmlSupportTest extends ScalatraSpec { def is =
  "In an app with AntiXmlSupport"                      ^
                                                       p^
    "when an action returns an Elem"                   ^
      "should infer a content type of 'text/html'"     ! inferContentType^
      "should serialize the element as its body"       ! responseBody^
                                                       p^
    "when an action returns a non-Elem"                ^
      "should pass through to the default pipeline"    ! passThrough^
                                                       end

  val servletHolder = addServlet(classOf[AntiXmlSupportTestServlet], "/*")

  def inferContentType =
    get("/anti-xml") {
      response.mediaType must_== (Some("text/html"))
    }

  def responseBody =
    get("/anti-xml") {
      response.body must_== """<foo bar="baz"/>"""
    }

  def passThrough =
    get("/pass-through") {
      response.body must_== "pass through"
    }
}


class AntiXmlSupportTestServlet extends ScalatraServlet with AntiXmlSupport {
  get("/anti-xml") {
    XML.fromString("""<foo bar="baz" />""")
  }

  get("/pass-through") {
    "pass through"
  }
}
