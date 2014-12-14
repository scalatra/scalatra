package org.scalatra
package swagger

import org.scalatra.json.JacksonJsonSupport
import org.json4s._
import org.scalatra.test.specs2.ScalatraSpec
import org.specs2.matcher.JsonMatchers

class SwaggerApiLookupSpec extends ScalatraSpec with JsonMatchers {
  val is = s2"""
  Swagger integration should
    list resources using Servlet path to generate listing path $listResources
    host the API listing of a Servlet without a custom name $listFooOperations
    host the API listing of a Servlet with a custom name $listBarOperations
  """
  val apiInfo = ApiInfo(
    title = "The title",
    description = "The description",
    termsOfServiceUrl = "http://helloreverb.com/terms/",
    contact = "apiteam@wordnik.com",
    license = "Apache 2.0",
    licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html"
  )
  val swagger = new Swagger("1.2", "1.0.0", apiInfo)

  addServlet(new ApiController1()(swagger), "/api/unnamed")
  addServlet(new ApiController2()(swagger), "/api/custom-name", "MyServletName")
  addServlet(new ApiDocs()(swagger), "/api-docs")
  implicit val formats: Formats = DefaultFormats

  def listResources = get("/api-docs") {
    status must_== 200
    jackson.parseJson(body) \ "apis" must_== JArray(List(
      JObject("path" -> JString("/api/unnamed"), "description" -> JString("The first API")),
      JObject("path" -> JString("/api/custom-name"), "description" -> JString("The second API"))
    ))
  }

  def listFooOperations = get("/api-docs/api/unnamed") {
    status must_== 200
    val json = jackson.parseJson(body)
    json \ "resourcePath" must_== JString("/api/unnamed")
    json \ "apis" \\ "path" must_== JObject("path" -> JString("/api/unnamed/"), "path" -> JString("/api/unnamed/{id}"))
  }

  def listBarOperations = get("/api-docs/api/custom-name") {
    status must_== 200
    val json = jackson.parseJson(body)
    json \ "resourcePath" must_== JString("/api/custom-name")
    json \ "apis" \\ "path" must_== JObject("path" -> JString("/api/custom-name/"), "path" -> JString("/api/custom-name/{id}"))
  }

}

class ApiDocs(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase

class ApiController1()(implicit val swagger: Swagger) extends ScalatraServlet with JacksonJsonSupport with SwaggerSupport {
  override implicit protected def jsonFormats: Formats = DefaultFormats

  protected val applicationDescription: String = "The first API"

  val listFoos = (apiOperation[List[String]]("listFoos")
    summary "Show all foos"
    notes "Shows all available foos.")

  get("/", operation(listFoos)) {
    List.empty[String]
  }

  val getFoo = (apiOperation[String]("getFoo")
    summary "Retrieve a single foo by id"
    notes "Foo"
    parameters Parameter("id", DataType.Int, Some("The id"), None, ParamType.Path, required = true))

  get("/:id", operation(getFoo)) {
    "Foo!"
  }

}

class ApiController2()(implicit val swagger: Swagger) extends ScalatraServlet with JacksonJsonSupport with SwaggerSupport {
  override implicit protected def jsonFormats: Formats = DefaultFormats

  protected val applicationDescription: String = "The second API"

  val listFoos = (apiOperation[List[String]]("listBars")
    summary "Show all bars"
    notes "Shows all available bars.")

  get("/", operation(listFoos)) {
    List.empty[String]
  }

  val getBar = (apiOperation[String]("getBar")
    summary "Retrieve a single bar by id"
    notes "Bar"
    parameters Parameter("id", DataType.Int, Some("The id"), None, ParamType.Path, required = true))

  get("/:id", operation(getBar)) {
    "Bar!"
  }

}
