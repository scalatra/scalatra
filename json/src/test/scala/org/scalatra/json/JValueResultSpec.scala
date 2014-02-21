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
  case a: Bottle => ("of", a.of)
}))

class SensorMessage(val subject: String = "foo-probe") {
  val version = 1.0

  val value1 = 3.1415
  val valueN = 2.7182

  def done = {
    false
  }
}

class JValueResultSpec extends MutableScalatraSpec {

  implicit def createJValueWriter[T <% JValue] = {
    new Writer[T] {
      override def write(obj: T): JValue = obj
    }
  }

  val jValue = JObject(JField("name", JString("tom")) :: Nil)
  val bottleRum = ("of", "rum").asJValue
  val bottleSoda = ("of", "soda").asJValue
  val message = ("version", 1.0) ~
    ("subject", "foo-probe") ~
    ("value1", 3.1415) ~
    ("valueN", 2.7182)

  addServlet(new ScalatraServlet with JacksonJsonSupport {

    implicit protected def jsonFormats: Formats = DefaultFormats +
      new BottleSerializer +
      FieldSerializer[SensorMessage]()

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

    get("/custom-serializer") {
      contentType = formats("json")
      new Bottle("rum")
    }

    get("/field-serializer") {
      contentType = formats("json")
      new SensorMessage("foo-probe")
    }

    get("/traversable") {
      contentType = formats("json")
      List(new Bottle("rum"), new Bottle("soda"))
    }

    get("/mixed-traversable") {
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

    "render a class using a custom serializer" in {
      get("/custom-serializer") {
        parse(body) must_== bottleRum
      }
    }

    "render a class using a field serializer" in {
      get("/field-serializer") {
        println(body)
        println(parse(body))
        parse(body) must_== message
      }
    }

    "render a traversable" in {
      get("/traversable") {
        parse(body) must_== List(bottleRum, bottleSoda).asJValue
      }
    }

    "render a mixed traversable" in {
      get("/mixed-traversable") {
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