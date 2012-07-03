package org.scalatra
package liftjson

import net.liftweb.json._
import test.specs.ScalatraSpecification
import test.NettyBackend

class LiftJsonSupportApp extends ScalatraApp with LiftJsonSupport {

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

abstract class LiftJsonRequestBodySpec extends ScalatraSpecification {

  mount(new LiftJsonSupportApp)

  "The LiftJsonSupport" should {

    "parse the json body of a request" in {
      val rbody = """{"name": "hello world"}"""
      post("/json", headers = Map("Accept" -> "application/json", "Content-Type" -> "application/json"), body = rbody) {
        status.code must_== 200
        body must_== "hello world"
      }
    }

    "parse the xml body of a request" in {
      val rbody = """<name>hello world</name>"""
      post("/json", headers = Map("Accept" -> "application/xml", "Content-Type" -> "application/xml"), body = rbody) {
        status.code must_== 200
        body must_== "hello world"
      }
    }

  }
}

class NettyLiftJsonRequestBodySpec extends LiftJsonRequestBodySpec with NettyBackend