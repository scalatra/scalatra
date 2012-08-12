package org.scalatra
package jackson

import org.scalatra.test.specs.ScalatraSpecification
import com.fasterxml.jackson.databind.node.{ObjectNode, MissingNode}

class JacksonSupportServlet extends ScalatraServlet with JacksonSupport {

  post("/json") {
    contentType = "text/plain"
    parsedBody match {
      case _: MissingNode ⇒ halt(400, "invalid json")
      case json: ObjectNode ⇒ {
        json.get("name").asText()
      }
      case _ ⇒ halt(400, "unknown json")
    }
  }

  error {
    case e: Throwable => e.printStackTrace()
  }
}

class JacksonRequestBodySpec extends ScalatraSpecification {

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
