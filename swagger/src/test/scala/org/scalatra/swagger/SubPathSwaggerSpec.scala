package org.scalatra
package swagger

import org.json4s._
import org.scalatra.json.JacksonJsonSupport
import org.scalatra.test.specs2.ScalatraSpec
import org.specs2.matcher.JsonMatchers

class SubPathSwaggerSpec extends ScalatraSpec with JsonMatchers {
  val is = s2"""
  Swagger integration should
    list resources $listResources
    list hacker operations $listHackerOperations
    list model elements in order $checkModelOrder
  """
  val apiInfo = ApiInfo(
    title = "Swagger Hackers Sample App",
    description = "This is a sample hackers app.  You can find out more about Swagger \n    at <a href=\"http://swagger.wordnik.com\">http://swagger.wordnik.com</a> or on irc.freenode.net, #swagger.",
    termsOfServiceUrl = "http://helloreverb.com/terms/",
    contact = "apiteam@wordnik.com",
    license = "Apache 2.0",
    licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html"
  )
  val swagger = new Swagger("1.2", "1.0.0", apiInfo)

  addServlet(new ApiController()(swagger), "/api/hackers", "api/hackers")
  addServlet(new HackersSwagger()(swagger), "/api-docs")
  implicit val formats: Formats = DefaultFormats

  def listResources = get("/api-docs") {
    jackson.parseJson(body) \ "apis" \\ "path" must_== JString("/api/hackers")
  }
  def listHackerOperations = get("/api-docs/api/hackers") {
    val json = jackson.parseJson(body)
    json \ "apis" \\ "path" must_== JObject("path" -> JString("/api/hackers/") :: "path" -> JString("/api/hackers/{id}") :: Nil)
  }
  def checkModelOrder = pending

}

case class Hacker(id: Long, firstName: String, lastName: String, motto: String, birthYear: Int)
class HackersSwagger(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase
class ApiController()(implicit val swagger: Swagger) extends ScalatraServlet with JacksonJsonSupport with SwaggerSupport {
  override implicit protected def jsonFormats: Formats = DefaultFormats

  protected val applicationDescription: String =
    """The Hacker Tracker API. Exposes operations for adding hackers and retrieving lists of hackers."""

  val listHackers = (apiOperation[List[Hacker]]("listHackers")
    summary "Show all hackers"
    notes "Shows all available hackers.")

  /**
   * List all hackers.
   */
  get("/", operation(listHackers)) {
    List.empty[Hacker]
  }

  val getHacker = (apiOperation[Hacker]("getHacker")
    summary "Retrieve a single hacker by id"
    notes "Foo"
    parameters Parameter("id", DataType.Int, Some("The hacker's database id"), None, ParamType.Path, required = true))

  /**
   * Retrieve a specific hacker.
   */
  get("/:id", operation(getHacker)) {
    null
  }

  val createHacker = (apiOperation[Hacker]("createHacker")
    summary "Create a new hacker"
    notes "firstname, lastname, motto, and year of birth are required"
    parameters (
      Parameter("firstname", DataType.String, Some("The hacker's first name"), None, ParamType.Body, required = true),
      Parameter("lastname", DataType.String, Some("The hacker's last name"), None, ParamType.Body, required = true),
      Parameter("motto", DataType.String, Some("A phrase associated with this hacker"), None, ParamType.Body, required = true),
      Parameter("birthyear", DataType.Int, Some("A four-digit number, the year that the user was born in"),
        Some("A four-digit number"), ParamType.Body, required = true))
  )

  /**
   * Create a new hacker in the database.
   */
  post("/", operation(createHacker)) {
    null
  }

  delete("/:id") {
    null
  }

}
