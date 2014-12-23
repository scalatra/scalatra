package org.scalatra
package json

import org.json4s._
import org.json4s.jackson.JsonMethods.parse
import org.scalatra.test.specs2.MutableScalatraSpec

case class NamedThing(name: String = "tom")

class Bottle(val of: String)

class BottleSerializer extends CustomSerializer[Bottle](implicit formats => ({
  case json: JValue =>
    val b = for {
      of <- (json \ "of").extractOpt[String]
    } yield (new Bottle(of))
    b.get
}, {
  case a: Bottle => JObject(JField("of", JString(a.of)))
})) {}

class JValueResultSpec extends MutableScalatraSpec {

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
        body must_== "printed" + System.lineSeparator
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

    val bottleRum = JObject(JField("of", JString("rum")))
    val bottleSoda = JObject(JField("of", JString("soda")))

    "render a class" in {
      get("/class") {
        parse(body) must_== bottleRum
      }
    }

    "render a class list" in {
      get("/class-list") {
        parse(body) must_== JArray(List(bottleRum, bottleSoda))
      }
    }

    "render a mixed list" in {
      get("/mixed-list") {
        parse(body) must_== JArray(List(bottleRum, jValue))
      }
    }

    "render a map" in {
      get("/map") {
        parse(body) must_== JObject(List(JField("rum", bottleRum), JField("thing", jValue)))
      }
    }
  }

}