package org.scalatra
package json

import test.specs2.MutableScalatraSpec
import org.json4s._
import org.json4s.jackson.JsonMethods.parse

case class NamedThing(name: String = "tom")
class JValueResultSpec extends MutableScalatraSpec {

  val jValue = JObject(JField("name", JString("tom")) :: Nil)
  addServlet(new ScalatraServlet with JacksonJsonSupport with JValueResult {

    implicit protected def jsonFormats: Formats = DefaultFormats

    override val defaultFormat: Symbol = 'json

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
      NamedThing()
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
  }

}
