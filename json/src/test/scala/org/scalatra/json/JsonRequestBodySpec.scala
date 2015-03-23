package org.scalatra
package json

import org.json4s._
import org.scalatra.test.specs2.MutableScalatraSpec

import scala.text.Document

trait JsonSupportServlet[T] extends ScalatraBase with JsonSupport[T] {

  def withBigDecimal: Boolean

  protected implicit val jsonFormats: Formats = if (withBigDecimal) DefaultFormats.withBigDecimal else DefaultFormats

  post("/json") {
    parsedBody match {
      case JNothing ⇒ halt(400, "invalid json")
      case json: JObject ⇒ {
        (json \ "name").extract[String]
      }
      case _ ⇒ halt(400, "unknown json")
    }
  }

  post("/decimal") {
    (parsedBody \ "number").extract[BigDecimal]
  }

}

trait JsonRequestSpec extends MutableScalatraSpec {
  "The JsonSupport" should {

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

    "parse number as double" in {
      val rbody = """{"number":3.14159265358979323846}"""
      post("/decimal", headers = Map("Accept" -> "application/json", "Content-Type" -> "application/json"), body = rbody) {
        status must_== 200
        body must_== "3.141592653589793"
      }
    }

  }
}

trait BigDecimalJsonRequestSpec extends MutableScalatraSpec {
  "The JsonSupport" should {

    "parse number as bigdecimal" in {
      val rbody = """{"number":3.14159265358979323846}"""
      post("/decimal", headers = Map("Accept" -> "application/json", "Content-Type" -> "application/json"), body = rbody) {
        status must_== 200
        body must_== "3.14159265358979323846"
      }
    }

  }
}

// servlets
class NativeJsonSupportServlet(val withBigDecimal: Boolean)
  extends ScalatraServlet with JsonSupportServlet[Document] with NativeJsonSupport

class JacksonSupportServlet(val withBigDecimal: Boolean)
  extends ScalatraServlet with JsonSupportServlet[JValue] with JacksonJsonSupport

// specs
class JacksonRequestBodySpec extends JsonRequestSpec {
  addServlet(new JacksonSupportServlet(false), "/*")
}

class NativeJsonRequestBodySpec extends JsonRequestSpec {
  addServlet(new NativeJsonSupportServlet(false), "/*")
}

class JacksonBigDecimalRequestBodySpec extends BigDecimalJsonRequestSpec {
  addServlet(new JacksonSupportServlet(true), "/*")
}

class NativeBigDecimalJsonRequestBodySpec extends BigDecimalJsonRequestSpec {
  addServlet(new NativeJsonSupportServlet(true), "/*")
}

