package org.scalatra
package json

import test.specs2.MutableScalatraSpec
import org.json4s._
import org.json4s.jackson.JsonMethods._

case class NamedThing(name: String = "tom")
class JValueResultSpec extends MutableScalatraSpec {

  val jValue = JObject(JField("name", JString("tom")) :: Nil)
  addServlet(new ScalatraServlet with JacksonJsonSupport {

    implicit protected def jsonFormats: Formats = DefaultFormats

    notFound {
      status = 404
      "the custom not found"
    }

    get("/jvalue") {
      jValue
    }
    get("/actionresult") {
      Created(jValue)
    }
    get("/unit") {
      response.writer.println("printed")
    }
    get("/namedthing") {
      contentType = formats("json")
      NamedThing()
    }
    get("/empty-not-found") {
      NotFound()
    }
    get("/halted-not-found") {
      halt(NotFound())
    }
    get("/null-value.:format") {
      null
    }
    get("/null-value") {
      ""
    }
    get("/empty-halt") {
      halt(400, null)
    }

    error {
      case t: Throwable =>
        t.printStackTrace()
    }
  }, "/*")

  "The JValueResult trait" should {
    "render a JValue result" in {
      get("/jvalue") {
        parse(body) must_== jValue
      }
    }
    "render an ActionResult result" in {
      get("/actionresult") {
        status must_== 201
        parse(body) must_== jValue
      }
    }
    "render a Unit result" in {
      get("/unit") {
        body must_== "printed\n"
      }
    }
    "render a NamedThing result" in {
      get("/namedthing") { parse(body) must_== jValue }
    }
    "render an empty not found" in {
      get("/empty-not-found") {
        status must_== 404
        body must_== "the custom not found"
      }
    }
    "render a null value" in {
      get("/null-value.json") {
         body must_== ""
      }
    }
    "render a null value" in {
      get("/null-value.html") {
        body must_== ""
      }
    }
    "render a halted empty not found" in {
      get("/halted-not-found") {
        status must_== 404
        body must_== "the custom not found"
      }
    }
  }

}