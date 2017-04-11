package org.scalatra
package swagger

import javax.servlet.http.{ HttpServletRequest, HttpServletResponse }

import org.json4s._
import org.json4s.native.JsonMethods._
import org.scalatra.auth.{ ScentryConfig, ScentryStrategy, ScentrySupport }
import org.scalatra.json.NativeJsonSupport
import org.scalatra.servlet.ServletApiImplicits._
import org.scalatra.test.specs2.MutableScalatraSpec
import org.scalatra.util.RicherString._

object SwaggerAuthSpec2 {
  case class User(login: String, token: String = "the_token")

  val Users = List(User("tom", "token1"), User("kate", "token2"), User("john"))

  val apiInfo = ApiInfo(
    title = "Swagger Sample App",
    description = "This is a sample server Petstore server.  You can find out more about Swagger \n    at <a href=\"http://swagger.wordnik.com\">http://swagger.wordnik.com</a> or on irc.freenode.net, #swagger.",
    termsOfServiceUrl = "http://helloreverb.com/terms/",
    contact = "apiteam@wordnik.com",
    license = "Apache 2.0",
    licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html"
  )
  class SpecSwagger extends SwaggerWithAuth("2.0", "1.0.0", apiInfo)

  class HeaderOrQueryToken(protected val app: ScalatraBase) extends ScentryStrategy[User] {
    override def name = "header_or_query_token"
    private def token(implicit request: HttpServletRequest) = (app.request.header("API-TOKEN") orElse app.params.get("api_token")).flatMap(_.blankOption)
    override def isValid(implicit request: HttpServletRequest) = token.isDefined
    def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse) = {
      token match {
        case Some("token1") => Option(Users(0))
        case Some("token2") => Option(Users(1))
        case Some("the_token") => Option(Users(2))
        case _ => None
      }
    }
  }

  trait AuthenticatedBase extends ScalatraServlet with NativeJsonSupport with ScentrySupport[User] {
    type ScentryConfiguration = ScentryConfig
    protected val scentryConfig: ScentryConfiguration = new ScentryConfig {}

    protected val fromSession: PartialFunction[String, User] = {
      case s: String => Users.find(_.login == s).get
    }
    protected val toSession: PartialFunction[User, String] = {
      case u: User => u.login
    }
    override def configureScentry = {
      //      scentry.store = new CookieAuthStore(this)
    }
    override def registerAuthStrategies = {
      scentry.register(new HeaderOrQueryToken(this))
    }

    error {
      case t: Throwable => t.printStackTrace()
      case t => t.printStackTrace()
    }
  }

  class ResourcesApp(implicit protected val swagger: SwaggerWithAuth)
      extends AuthenticatedBase
      with NativeJsonSupport
      with CorsSupport
      with SwaggerAuthBase[User] {

    error {
      case t: Throwable => t.printStackTrace()
    }

    protected val userManifest = manifest[User]

  }

  class PetsApi(implicit protected val swagger: SwaggerWithAuth) extends AuthenticatedBase with NativeJsonSupport with SwaggerAuthSupport[User] {

    implicit protected def jsonFormats: Formats = DefaultFormats

    protected val applicationDescription = "The pets api"
    override protected val applicationName = Some("pets")

    private val allowsTom = (u: Option[User]) => {
      u.map(_.login) == Some("tom")
    }

    private val allowsAuthenticated = (u: Option[User]) => {
      u.isDefined
    }

    private val noJohn = (u: Option[User]) => {
      val uu = u.map(_.login)
      uu.isDefined && uu != Some("john")
    }

    get("/", operation(apiOperation[Unit]("getPets"))) {
      "OK"
    }

    get("/authenticated", operation(apiOperation[Unit]("authenticated").allows(allowsAuthenticated))) {
      "OK"
    }

    get("/admin", operation(apiOperation[Unit]("admin").allows(allowsTom))) {
      "OK"
    }

    get("/kate-and-tom", operation(apiOperation[Unit]("getKateAndTom").allows(noJohn))) {
      "OK"
    }
  }

  class AdminApi(implicit protected val swagger: SwaggerWithAuth) extends AuthenticatedBase with NativeJsonSupport with SwaggerAuthSupport[User] {
    protected val applicationDescription = "The admin api"
    override protected val applicationName = Some("admin")

    implicit protected def jsonFormats: Formats = DefaultFormats

    private[this] def isAllowed(u: Option[User]) = {
      u.map(_.login) == Some("tom")
    }

    get("/", operation(apiOperation[Unit]("adminlist").allows(isAllowed))) {
      println("executing / ")
      "OK"
    }

    get("/blah", operation(apiOperation[Unit]("blah").allows(isAllowed))) {
      "OK"
    }

    post("/blah", operation(apiOperation[Unit]("createBlah").allows(isAllowed))) {
      "OK"
    }
  }
}
class SwaggerAuthSpec2 extends MutableScalatraSpec {

  import org.scalatra.swagger.SwaggerAuthSpec2._
  implicit val swagger: SwaggerWithAuth = new SpecSwagger

  addServlet(new PetsApi, "/pets/*")

  addServlet(new AdminApi, "/admin/*")

  addServlet(new ResourcesApp, "/api-docs/*")

  private def paths(jv: JValue): List[JValue] = (jv \ "paths").children
  private def operationId(jv: JValue, path: String, method: String): String = {
    (jsonBody \ "paths" \ path \ method \ "operationId").asInstanceOf[JString].values
  }
  private def jsonBody = {
    val b = body
    //    println("json body")
    val j = parse(b)
    //    println(pretty(render(j)))
    j
  }

  "SwaggerAuth for Swagger 2.0" should {

    "only render accessible endpoints in resources index" in {
      get("/api-docs/swagger.json") {
        status must_== 200
        paths(jsonBody).size must_== 1
        operationId(jsonBody, "/pets/", "get") must_== "getPets"
      }
    }

    "don't render inaccessible resource for non-admin user" in {
      get("/api-docs/swagger.json", "api_token" -> "the_token") {
        status must_== 200
        paths(jsonBody).size must_== 2
        operationId(jsonBody, "/pets/", "get") must_== "getPets"
        operationId(jsonBody, "/pets/authenticated", "get") must_== "authenticated"
      }
    }

    "render all resources for admin user" in {
      get("/api-docs/swagger.json", "api_token" -> "token1") {
        status must_== 200
        paths(jsonBody).size must_== 6
        operationId(jsonBody, "/pets/", "get") must_== "getPets"
        operationId(jsonBody, "/pets/authenticated", "get") must_== "authenticated"
        operationId(jsonBody, "/pets/kate-and-tom", "get") must_== "getKateAndTom"
        operationId(jsonBody, "/admin/", "get") must_== "adminlist"
        operationId(jsonBody, "/admin/blah", "get") must_== "blah"
        operationId(jsonBody, "/admin/blah", "post") must_== "createBlah"
      }
    }

  }

}
