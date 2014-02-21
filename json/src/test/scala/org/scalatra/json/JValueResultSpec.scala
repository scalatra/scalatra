package org.scalatra
package json

import test.specs2.MutableScalatraSpec

import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods.parse

case class NamedThing(name: String = "tom")

class Bottle(val of: String)

class BottleSerializer extends CustomSerializer[Bottle](implicit formats => ({
  case json: JValue =>
    val of = (json \ "of").extract[String]
    new Bottle(of)
}, {
  case a: Bottle => JObject(JField("of", JString(a.of)))
}))

class JValueResultSpec extends MutableScalatraSpec {

  implicit def createJValueWriter[T <% JValue] = {
    new Writer[T] {
      override def write(obj: T): JValue = obj
    }
  }

  val jValue = JObject(JField("name", JString("tom")) :: Nil)

  addServlet(new ScalatraServlet with JacksonJsonSupport {

    implicit protected def jsonFormats: Formats = DefaultFormats + new BottleSerializer

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

    get("/class") {
      contentType = formats("json")
      new Bottle("rum")
    }

    get("/class-list") {
      contentType = formats("json")
      List(new Bottle("rum"), new Bottle("soda"))
    }

    get("/mixed-list") {
      contentType = formats("json")
      List(new Bottle("rum"), NamedThing())
    }

    get("/map") {
      contentType = formats("json")
      Map("rum" -> new Bottle("rum"), "thing" -> NamedThing())
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

    val bottleRum = ("of", "rum").asJValue
    val bottleSoda = ("of", "soda").asJValue

    "render a class" in {
      get("/class") {
        parse(body) must_== bottleRum
      }
    }

    "render a class list" in {
      get("/class-list") {
        parse(body) must_== List(bottleRum, bottleSoda).asJValue
      }
    }

    "render a mixed list" in {
      get("/mixed-list") {
        parse(body) must_== List(bottleRum, jValue).asJValue
      }
    }

    "render a map" in {
      get("/map") {
        parse(body) must_== ("rum", bottleRum) ~ ("thing", jValue)
      }
    }
  }

}