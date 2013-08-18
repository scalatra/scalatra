package org.scalatra
package swagger

import test.specs2.ScalatraSpec
import org.specs2._
import matcher.{MatchResult, JsonMatchers}
import org.json4s._
import jackson.JsonMethods
import JsonDSL._
import org.json4s.native.JsonParser
import org.scalatra.json.{JValueResult, NativeJsonSupport}
import scala.io.Source
import java.net.ServerSocket
import org.scalatra.swagger.annotations.ApiModelProperty
import scala.collection.mutable
import org.joda.time.DateTime

class SwaggerSpec extends ScalatraSpec with JsonMatchers { def is = sequential ^
  "Swagger integration should"                                  ^
    "list resources"                       ! listResources       ^
//    "list operations"                      ! listOperations     ^
  end
  val apiInfo = ApiInfo(
      title = "Swagger Sample App",
      description = "This is a sample server Petstore server.  You can find out more about Swagger \n    at <a href=\"http://swagger.wordnik.com\">http://swagger.wordnik.com</a> or on irc.freenode.net, #swagger.",
      termsOfServiceUrl = "http://helloreverb.com/terms/",
      contact = "apiteam@wordnik.com",
      license = "Apache 2.0",
      licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html"
    )
  val swagger = new Swagger("1.2", "1.0.0", apiInfo)
  swagger.addAuthorization(ApiKey("apiKey"))
  swagger.addAuthorization(OAuth(
    List("PUBLIC"),
    List(
      ImplicitGrant(LoginEndpoint("http://localhost:8002/oauth/dialog"), "access_code"),
      AuthorizationCodeGrant(
        TokenRequestEndpoint("http://localhost:8002/oauth/requestToken", "client_id", "client_secret"),
        TokenEndpoint("http://localhost:8002/oauth/token", "access_code"))
    )
  ))
  val testServlet = new SwaggerTestServlet(swagger)

  addServlet(testServlet, "/pet/*")
  addServlet(new StoreApi(swagger), "/store/*")
  addServlet(new UserApi(swagger), "/user/*")
  addServlet(new SwaggerResourcesServlet(swagger), "/api-docs/*")
  implicit val formats = DefaultFormats


  /**
   * Sets the port to listen on.  0 means listen on any available port.
   */
  override lazy val port: Int = { val s = new ServerSocket(0); try { s.getLocalPort } finally { s.close() } }//58468

  val listResourceJValue = readJson("api-docs.json")// merge (("basePath" -> ("http://localhost:" + port)):JValue)

  val listOperationsJValue = readJson("pet.json") merge (("basePath" -> ("http://localhost:" + port)):JValue)

  private def readJson(file: String) = {
    val f = if ( file startsWith "/" ) file else "/"+file
    val rdr = Source.fromInputStream(getClass.getResourceAsStream(f)).bufferedReader()
    JsonParser.parse(rdr)
  }


  def listResources = get("/api-docs") {
    val bd = JsonParser.parseOpt(body)
    bd must beSome[JValue] and {
      val j = bd.get
      (j \ "apiVersion" must_== listResourceJValue \ "apiVersion") and
      (j \ "swaggerVersion" must_== listResourceJValue \ "swaggerVersion") and
      verifyInfo(j \ "info") and
      verifyApis(j \ "apis") and
      verifyAuthorizations(j \ "authorizations")

    }
  }

  def verifyApis(j: JValue) = {
    val JArray(apis)  = j
    val expectations = mutable.HashMap("/pet" -> "Operations about pets", "/store" -> "Operations about store", "/user" -> "Operations about user")
    apis map { api =>
      val JString(path) = api \ "path"
      val desc = expectations(path)
      expectations -= path
      JString(desc) must_== api \ "description"
    } reduce (_ and _) and (expectations must beEmpty)
  }

  def verifyAuthorizations(j: JValue) = {
    val auth = listResourceJValue \ "authorizations"
    j \ "oauth2" \ "type" must_== auth \ "oauth2" \ "type" and
    (j \ "oauth2" \ "scopes" must_== auth \ "oauth2" \ "scopes") and
    (j \ "oauth2" \ "grantTypes" \ "implicit" \ "loginEndpoint" must_== auth \ "oauth2" \ "grantTypes" \ "implicit" \ "loginEndpoint") and
    (j \ "oauth2" \ "grantTypes" \ "implicit" \ "tokenName" must_== auth \ "oauth2" \ "grantTypes" \ "implicit" \ "tokenName") and
    (j \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenRequestEndpoint" \ "url" must_== auth \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenRequestEndpoint" \ "url") and
    (j \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenRequestEndpoint" \ "clientIdName" must_== auth \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenRequestEndpoint" \ "clientIdName") and
    (j \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenRequestEndpoint" \ "clientSecretName" must_== auth \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenRequestEndpoint" \ "clientSecretName") and
    (j \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenEndpoint" \ "url" must_== auth \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenEndpoint" \ "url") and
    (j \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenEndpoint" \ "tokenName" must_== auth \ "oauth2" \ "grantTypes" \ "authorization_code" \ "tokenEndpoint" \ "tokenName") and
    (j \ "apiKey" \ "type" must_== auth \ "apiKey" \ "type") and
    (j \ "apiKey" \ "passAs" must_== auth \ "apiKey" \ "passAs")
  }

  def verifyInfo(j: JValue) = {
    val info = listResourceJValue \ "info"
    (j \ "title" must_== info \ "title") and
    (j \ "description" must_== info \ "description") and
    (j \ "termsOfServiceUrl" must_== info \ "termsOfServiceUrl") and
    (j \ "contact" must_== info \ "contact") and
    (j \ "license" must_== info \ "license") and
    (j \ "licenseUrl" must_== info \ "licenseUrl")
  }

  val operations = "allPets" :: "updatePet" :: "addPet" :: "findByTags" :: "findPetsByStatus" :: "findById" :: Nil
//  val operations = "allPets" :: Nil
  def listOperations = {
    get("/pet.json") {
      val bo = JsonParser.parseOpt(body)
      bo must beSome[JValue] and
        verifyCommon(bo.get) and
        operations.map(verifyOperation(bo.get, _)).reduce(_ and _) and
        verifyPetModel(bo.get)
    }
  }

  def verifyCommon(jv: JValue): MatchResult[Any] = {
    (jv \ "apiVersion" must_== listOperationsJValue \ "apiVersion") and
    (jv \ "swaggerVersion" must_== listOperationsJValue \ "swaggerVersion") and
    (jv \ "basePath" must_== listOperationsJValue \ "basePath") and
    (jv \ "description" must_== listOperationsJValue \ "description") and
    (jv \ "resourcePath" must_== listOperationsJValue \ "resourcePath") and {
      val ja = jv \ "apis" \ "path" \\ classOf[JString]
      (ja.size must_== 4) and
        (ja must haveTheSameElementsAs(List("/pet/{id}", "/pet/findByTags", "/pet/findByStatus", "/pet/")))
    }
  }

  def verifyOperation(jv: JValue, name: String) = {
    val op = findOperation(jv, name)
    val exp = findOperation(listOperationsJValue, name)
    (op must beSome[JValue]).setMessage("Couldn't find extractOperation: " + name) and {
      val m = verifyFields(op.get, exp.get, "httpMethod", "nickname", "responseClass", "summary", "parameters", "notes", "errorResponses")
      m setMessage (m.message + " of the operation " + name)
    }
  }

  def verifyPetModel(actualPetJson: JValue) = {
    def petProperties(jv: JValue) = jv \ "models" \ "Pet" \ "properties"
    val actualPetProps = petProperties(actualPetJson)
    val expectedPetProps = petProperties(listOperationsJValue)

    val m = verifyFields(actualPetProps, expectedPetProps, "category", "id", "name", "status", "tags", "urls")
    m setMessage (m.message + " of the pet model")
  }

  def verifyFields(actual: JValue, expected: JValue, fields: String*): MatchResult[Any] = {
    def verifyField(act: JValue, exp: JValue, fn: String): MatchResult[Any] = {
      fn match {
        case "errorResponses" =>
          val JArray(af) = act \ fn
          val JArray(ef) = exp \ fn
          val r = af map { v =>
            val mm = verifyFields(
              v,
              ef find (_ \ "code" == v \ "code") getOrElse JNothing,
              "code", "reason")
            mm setMessage (mm.message + " in error responses collection")
          }
          if (r.nonEmpty) r reduce (_ and _) else 1.must_==(1)
        case "parameters" =>
          val JArray(af) = act \ fn
          val JArray(ef) = exp \ fn
          val r = af map { v =>
            val mm = verifyFields(
              v,
              ef find (_ \ "name" == v \ "name") get,
              "allowableValues", "dataType", "paramType", "allowMultiple", "defaultValue", "description", "name", "required")
            mm setMessage (mm.message + " for parameter " + ( v \ "name" ).extractOrElse("N/A"))
          }

          if (r.nonEmpty) r reduce (_ and _) else 1.must_==(1)
        case _ =>
          val m = act \ fn must_== exp \ fn
          m setMessage (JsonMethods.compact(JsonMethods.render(act \ fn)) + " does not match\n" + JsonMethods.compact(JsonMethods.render(exp \ fn)) + " for field " + fn)
      }
    }

    (fields map (verifyField(actual, expected, _)) reduce (_ and _))
  }

  def findOperation(jv: JValue, name: String) = {
    val JArray(ja) = jv \ "apis"
    ja find { jn =>
      val JArray(ops) = jn \ "operations"
      ops.exists(_ \ "nickname" == JString(name))
    } flatMap { api =>
      (api \ "operations").find(_ \ "nickname" == JString(name))
    }
  }
}

class SwaggerTestServlet(protected val swagger:Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {

  protected val applicationDescription = "Operations about pets"
  override protected val applicationName = Some("pet")
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val StringFormat = DefaultJsonFormats.GenericFormat(DefaultReaders.StringReader, DefaultWriters.StringWriter)

  val data = new PetData

  get("/undocumented") {
    BadRequest("This should not show up")
  }

  val rootOperation =
    (apiOperation[List[Pet]]("allPets")
      summary "Show all pets"
      notes "shows all the pets in the data store")

  get("/", operation(rootOperation)) {
    data.pets
  }

  val getPet =
    (apiOperation[Pet]("findById")
      summary "Find by ID"
      notes "Returns a pet when ID < 10. ID > 10 or nonintegers will simulate API error conditions"
      responseMessages (StringResponseMessage(400, "Invalid ID supplied"), StringResponseMessage(404, "Pet not found"))
      parameter pathParam[String]("id").description("ID of pet that needs to be fetched"))

  get("/:id", operation(getPet)) {
    data.getPetbyId(params.getAs[Long]("petId").getOrElse(0))
  }

  val createPet =
    (apiOperation[Unit]("addPet")
      summary "Add a new pet to the store"
      responseMessage StringResponseMessage(400, "Invalid pet data supplied")
      parameter bodyParam[Pet].description("Pet object that needs to be added to the store"))

  post("/", operation(createPet)) {
    ApiResponse(ApiResponseType.OK, "pet added to store")
  }

  val updatePet =
    (apiOperation[Unit]("updatePet")
      summary "Update an existing pet"
      responseMessage StringResponseMessage(404, "Pet not found")
      parameter bodyParam[Pet].description("Pet object that needs to be updated in the store"))

  put("/", operation(updatePet)) {
    ApiResponse(ApiResponseType.OK, "pet updated")
  }

  val findByStatus =
    (apiOperation[List[Pet]]("findPetsByStatus")
      summary "Finds Pets by status"
      notes "Multiple status values can be provided with comma separated strings"
      parameter (queryParam[String]("status").required
                  description "Status values that need to be considered for filter"
                  defaultValue "available"
                  allowableValues ("available", "pending", "sold")))

  get("/findByStatus", operation(findByStatus)) {
    data.findPetsByStatus(params("status"))
  }

  val findByTags =
      (apiOperation[List[Pet]]("findByTags")
        summary "Finds Pets by tags"
        notes "Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing."
        parameter queryParam[String]("tags").description("Tags to filter by"))

  get("/findByTags", operation(findByTags)) {
    data.findPetsByTags(params("tags"))
  }
}

class StoreApi(val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {
  protected val applicationDescription = "Operations about store"
  override protected val applicationName = Some("store")
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val StringFormat = DefaultJsonFormats.GenericFormat(DefaultReaders.StringReader, DefaultWriters.StringWriter)

  val getOrderOperation = apiOperation[Order]("getOrderById")
  get("/order/:orderId", operation(getOrderOperation)) {
    ""
  }

  val deleteOrderOperation = apiOperation[Unit]("deleteOrder")
  delete("/order/:orderId", operation(deleteOrderOperation)) {
    NoContent()
  }

  val placeOrderOperation = apiOperation[Unit]("placeOrder")
  post("/order", operation(placeOrderOperation)) {
    ""
  }

}


class UserApi(val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {
  protected val applicationDescription = "Operations about user"
  override protected val applicationName = Some("user")
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val StringFormat = DefaultJsonFormats.GenericFormat(DefaultReaders.StringReader, DefaultWriters.StringWriter)

  val createUserOperation = apiOperation[User]("createUser")
  post("/", operation(createUserOperation)) {
    ""
  }
}

class SwaggerResourcesServlet(val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

case class Order(id: Long, status: String, petId: Long, quantity: Int, shipDate: DateTime)
case class User(id: Long, username: String, password: String, email: String, firstName: String, lastName: String, phone: String, userStatus: Int)
case class Pet(id: Long, category: Category, name: String, urls: List[String], tags: List[Tag],
               @ApiModelProperty(value = "pet availability", allowableValues = "available,sold") status: String)
case class Tag(id: Long, name: String)
case class Category(id: Long, name: String)

case class ApiResponse(code: String, msg: String)

object ApiResponseType {
  val ERROR = "error"
  val WARNING = "warning"
  val INFO = "info"
  val OK = "ok"
  val TOO_BUSY = "too busy"
}

class PetData {
  var categories = List(
    Category(1, "Dogs"),
    Category(2, "Cats"),
    Category(3, "Rabbits"),
    Category(4, "Lions"))

  var pets = List(
    Pet(1, categories(1), "Cat 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(2, categories(1), "Cat 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(3, categories(1), "Cat 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),

    Pet(4, categories(0), "Dog 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(5, categories(0), "Dog 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(6, categories(0), "Dog 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),

    Pet(7, categories(3), "Lion 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(8, categories(3), "Lion 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
    Pet(9, categories(3), "Lion 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),

    Pet(10, categories(2), "Rabbit 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"))

  def getPetbyId(id: Long): Option[Pet] = pets.find(_.id == id)

  def findPetsByStatus(status: String): List[Pet] = {
    val statuses = status.split(",").toSet
    pets.filter(pet => statuses.contains(pet.status))
  }

  def findPetsByTags(tag: String): List[Pet] = {
    val tags = tag.split(",").toSet
    pets.filter(pet => (tags & pet.tags.map(_.name).toSet).nonEmpty)
  }

  def addPet(pet: Pet) = {
    // remove any pets with same id
    pets = List(pet) ++ pets.filter(p => p.id == pet.id)
  }
}