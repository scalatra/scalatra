package org.scalatra
package json

import test.specs2.MutableScalatraSpec
import org.json4s._

class NativeJsonSupportServlet extends ScalatraServlet with NativeJsonSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  post("/json") {
    parsedBody match {
      case JNothing ⇒ halt(400, "invalid json")
      case json: JObject ⇒ {
        (json \ "name").extract[String]
      }
      case _ ⇒ halt(400, "unknown json")
    }
  }

}

class NativeJsonRequestBodySpec extends MutableScalatraSpec {

  addServlet(new NativeJsonSupportServlet, "/*")

  "The LiftJsonSupport" should {

    "parse the json body of a request" in {
      val rbody = """{"name": "hello world"}"""
      post("/json", headers = Map("Accept" -> "application/json", "Content-Type" -> "application/json"), body = rbody) {
        status must_== 200
        body must_== "hello world"
      }
    }

    "parse the xml body of a request" in {
      val rbody = """<req><name>hello world</name></req>"""
      post("/json", headers = Map("Accept" -> "application/xml", "Content-Type" -> "application/xml"), body = rbody) {
        status must_== 200
        body must_== "hello world"
      }
    }

    "parse the xml body which attempts XXE attacks" in {
      // see also: http://blog.goodstuff.im/lift_xxe_vulnerability
      val rbody = """<?xml version="1.0"?>
<!DOCTYPE str [
<!ENTITY pass SYSTEM "/etc/passwd">
]>
<req><name>&pass;</name></req>"""
      post("/json", headers = Map("Accept" -> "application/xml", "Content-Type" -> "application/xml"), body = rbody) {
        status must_== 200
        body must_== ""
      }
    }
  }
}

class JacksonSupportServlet extends ScalatraServlet with JacksonJsonSupport {

  protected implicit val jsonFormats: Formats = DefaultFormats

  post("/json") {
    contentType = "text/plain"
    parsedBody match {
      case JNothing ⇒ halt(400, "invalid json")
      case json: JObject ⇒ {
        (json \ "name").extract[String]
      }
      case _ ⇒ halt(400, "unknown json")
    }
  }

  error {
    case e: Throwable =>
      e.printStackTrace()
  }
}

class JacksonRequestBodySpec extends MutableScalatraSpec {

  addServlet(new JacksonSupportServlet, "/*")

  "The JacksonSupport" should {

    "parse the json body of a request" in {
      val rbody = """{"name": "hello world"}"""
      post("/json", headers = Map("Accept" -> "application/json", "Content-Type" -> "application/json"), body = rbody) {
        status must_== 200
        body must_== "hello world"
      }
    }

    "parse the xml body of a request" in {
      val rbody = """<req><name>hello world</name></req>"""
      post("/json", headers = Map("Accept" -> "application/xml", "Content-Type" -> "application/xml"), body = rbody) {
        status must_== 200
        body must_== "hello world"
      }
    }

  }
}

