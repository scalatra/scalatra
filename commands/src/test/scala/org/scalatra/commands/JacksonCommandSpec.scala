package org.scalatra
package commands

//import jackson.JacksonSupport
import org.scalatra.test.specs2.MutableScalatraSpec

//
//class JacksonTestForm extends JacksonCommand with JsonTestFields
//
//class JacksonCommandSpecServlet extends ScalatraServlet with JacksonSupport with CommandSupport with JacksonParsing {
//
//  post("/valid") {
//    val cmd = command[JacksonTestForm]
//    cmd.name.value.toOption.get + ":" + cmd.quantity.value.toOption.get
//  }
//
//  post("/invalid") {
//    val cmd = command[JacksonTestForm]
//    if (cmd.isInvalid) "OK"
//    else "FAIL"
//  }
//
//}
//
//
//class JacksonCommandSpec extends JsonCommandSpec("Jackson", new JacksonCommandSpecServlet)
abstract class JsonCommandSpec(jsonTestTitle: String, servletUnderTest: => ScalatraServlet) extends MutableScalatraSpec {

  val validJson = """{"name":"ihavemorethan5chars","quantity":5}"""
  val validXml = "<line><name>ihavemorethan5chars</name><quantity>5</quantity></line>"
  val invalidJson = """{"name":"4cha","quantity":2}"""
  val invalidXml = "<line><name>4cha</name><quantity>2</quantity></line>"
  addServlet(servletUnderTest, "/*")

  (jsonTestTitle + " command support") should {

    "read valid json" in {
      post("/valid", body = validJson, headers = Map("Content-Type" -> "application/json")) {
        status must_== 200
        body must_== "ihavemorethan5chars:5"
      }
    }

    "read valid params" in {
      post("/valid", "name" -> "ihavemorethan5chars", "quantity" -> "5") {
        status must_== 200
        body must_== "ihavemorethan5chars:5"
      }
    }

    "read valid xml" in {
      post("/valid", body = validXml, headers = Map("Content-Type" -> "application/xml")) {
        status must_== 200
        body must_== "ihavemorethan5chars:5"
      }
    }

    "read invalid json" in {
      post("/invalid", body = invalidJson, headers = Map("Content-Type" -> "application/json")) {
        status must_== 200
        body must_== "OK"
      }
    }

    "read invalid params" in {
      post("/invalid", "name" -> "4cha", "quantity" -> "2") {
        status must_== 200
        body must_== "OK"
      }
    }

    "read invalid xml" in {
      post("/invalid", body = invalidXml, headers = Map("Content-Type" -> "application/xml")) {
        status must_== 200
        body must_== "OK"
      }

    }
  }

}
