package org.scalatra
package swagger

import test.specs2.ScalatraSpec
import org.specs2._
import matcher.JsonMatchers
import org.json4s._
import JsonDSL._

class SwaggerSpec extends ScalatraSpec with JsonMatchers with native.JsonMethods { def is =
  "Swagger integration should"                                  ^
    "list resources"                       ! listResoures       ^
    "list operations"                      ! listOperations     ^
  end

  val swagger = new Swagger("1.0", "1")
  val testServlet = new SwaggerTestServlet
  swagger register("test", "/test", "Test", testServlet)
  addServlet(testServlet, "/test/*")
  addServlet(new SwaggerResourcesServlet(swagger), "/*")

  def listResoures = get("/resources.json") {
    parseOpt(body) must beSome.like {
      case x: JObject => (x \ "basePath" must be_==(JString("http://localhost/"))) and
        (x \ "apiVersion" must be_== (JString("1"))) and
        (x \ "swaggerVersion" must be_== (JString("1.0"))) and
        (x \ "apis" must be_== (JArray((("path" -> "/test.{format}") ~ ("description" -> "Test")) :: Nil)))
    }
  }

  def listOperations = get("/test.json") {
    parseOpt(body) must beSome.like {
      case x: JObject => (x \ "basePath" must be_==(JString("http://localhost/"))) and
        (x \ "resourcePath" must be_== (JString("/test"))) and
        (x \ "apiVersion" must be_== (JString("1"))) and
        (x \ "swaggerVersion" must be_== (JString("1.0"))) and
        (x \ "description" must be_== (JString("Test"))) and
        (x \ "apis" must beLike {
          case JArray(testEp :: findByIdEp :: Nil) => (testEp \ "path" must be_== (JString("/test/"))) and
            (testEp \ "description" must be_== (JString(""))) and
            (testEp \ "operations" must beLike {
              case JArray(testOp :: Nil) => (testOp \ "httpMethod" must be_== (JString("GET"))) and
                (testOp \ "nickname" must be_== (JString("test"))) and
                (testOp \ "summary" must be_== (JString("Test")))
                (testOp \ "responseClass" must be_== (JString("void"))) and
                (testOp \ "errorResponses" must be_== (JArray(Nil))) and
                (testOp \ "notes" must be_== (JNothing)) and
                (testOp \ "parameters" must be_== (JArray(Nil)))
            }) and
            (findByIdEp \ "path" must be_== (JString("/test/{id}"))) and
            (findByIdEp \ "description" must be_== (JString(""))) and
            (findByIdEp \ "operations" must beLike {
              case JArray(op :: Nil) => (op \ "httpMethod" must be_== (JString("GET"))) and
                (op \ "nickname" must be_== (JString("findById"))) and
                (op \ "summary" must be_== (JString("Find by ID"))) and
                (op \ "responseClass" must be_== (JString("Book"))) and
                (op \ "errorResponses" must be_== (JArray(Nil))) and
                (op \ "notes" must be_== (JNothing)) and
                (op \ "parameters" must beLike {
                  case JArray(param :: Nil) => (param \ "name" must be_== (JString("id"))) and
                    (param \ "description" must be_== (JString("ID"))) and
                    (param \ "paramType" must be_== (JString("path"))) and
                    (param \ "dataType" must be_== (JString("string"))) and
                    (param \ "allowMultiple" must be_== (JBool(false))) and
                    (param \ "required" must be_== (JBool(true)))
                })
            })
        })
    }
  }
}

class SwaggerTestServlet extends ScalatraServlet with SwaggerSupport {
  get("/", summary("Test"), nickname("test")) {}
  get("/:id", summary("Find by ID"), nickname("findById"), responseClass("Book"), endpoint("{id}"), parameters(
    Parameter("id", "ID", DataType.String, paramType = ParamType.Path)
  )) {}
}

class SwaggerResourcesServlet(val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase {
  protected def buildFullUrl(path: String) = "http://localhost/%s" format path
}