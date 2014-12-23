package org.scalatra
package swagger

import java.net.ServerSocket

import org.joda.time.DateTime
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.JsonMethods
import org.json4s.native.JsonParser
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.annotations._
import org.scalatra.test.specs2.ScalatraSpec
import org.specs2.matcher.{ JsonMatchers, MatchResult }

import scala.collection.mutable
import scala.io.Source

class SwaggerSpec extends ScalatraSpec with JsonMatchers {
  def is = sequential ^
    "Swagger integration should" ^
    "list resources" ! listResources ^
    "list pet operations" ! listPetOperations ^
    "list store operations" ! listStoreOperations ^
    "list user operations" ! listUserOperations ^
    "list model elements in order" ! checkModelOrder ^
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
  override lazy val port: Int = { val s = new ServerSocket(0); try { s.getLocalPort } finally { s.close() } } //58468

  val listResourceJValue = readJson("api-docs.json") // merge (("basePath" -> ("http://localhost:" + port)):JValue)

  val petOperationsJValue = readJson("pet.json") merge (("basePath" -> ("http://localhost:" + port)): JValue)
  val storeOperationsJValue = readJson("store.json") merge (("basePath" -> ("http://localhost:" + port)): JValue)
  val userOperationsJValue = readJson("user.json") merge (("basePath" -> ("http://localhost:" + port)): JValue)

  private def readJson(file: String) = {
    val f = if (file startsWith "/") file else "/" + file
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

  def parseInt(i: String): Option[Int] =
    try {
      Some(Integer.parseInt(i))
    } catch {
      case _: Throwable ⇒ None
    }

  val propOrder = "category" :: "name" :: "id" :: "tags" :: "status" :: "photoUrls" :: Nil
  def checkModelOrder = {
    get("/api-docs/pet") {
      val bd = JsonParser.parseOpt(body)
      bd must beSome[JValue] and {
        val j = bd.get
        val props = (j \ "models" \ "Pet" \ "properties").asInstanceOf[JObject].values.map { case (x, y) ⇒ x → y.asInstanceOf[Map[String, BigInt]].get("position").flatMap(x ⇒ parseInt(x.toString)).getOrElse(0) }.toList sortBy (_._2) map (_._1)
        props must_== propOrder
      }
    }
  }

  def verifyApis(j: JValue) = {
    val JArray(apis) = j
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

  val petOperations = "updatePet" :: "addPet" :: "deletePet" :: "findPetsByTags" :: "findPetsByStatus" :: "getPetById" :: Nil
  val storeOperations = "placeOrder" :: "deleteOrder" :: "getOrderById" :: Nil
  //  val operations = "allPets" :: Nil
  def listPetOperations = {
    get("/api-docs/pet") {
      val bo = JsonParser.parseOpt(body)
      bo must beSome[JValue] and
        verifyCommon(bo.get, petOperationsJValue, List("/pet/{petId}", "/pet/findByTags", "/pet/findByStatus", "/pet/")) and
        petOperations.map(verifyOperation(bo.get, petOperationsJValue, _)).reduce(_ and _) and
        verifyPetModel(bo.get)
    }
  }

  def listStoreOperations = {
    get("/api-docs/store") {
      val bo = JsonParser.parseOpt(body)
      bo must beSome[JValue] and
        verifyCommon(bo.get, storeOperationsJValue, List("/store/order/{orderId}", "/store/order")) and
        storeOperations.map(verifyOperation(bo.get, storeOperationsJValue, _)).reduce(_ and _) and
        verifyStoreModel(bo.get)
    }
  }

  def listUserOperations = {
    pending
    //    get("/api-docs/pet") {
    //      val bo = JsonParser.parseOpt(body)
    //      bo must beSome[JValue] and
    //        verifyCommon(bo.get) and
    //        operations.map(verifyOperation(bo.get, _)).reduce(_ and _) and
    //        verifyPetModel(bo.get)
    //    }
  }

  def verifyCommon(actual: JValue, expected: JValue, operationPaths: List[String]): MatchResult[Any] = {
    (actual \ "apiVersion" must_== expected \ "apiVersion") and
      (actual \ "swaggerVersion" must_== expected \ "swaggerVersion") and
      (actual \ "basePath" must_== expected \ "basePath") and
      (actual \ "description" must_== expected \ "description") and
      (actual \ "consumes" must_== expected \ "consumes") and
      (actual \ "produces" must_== expected \ "produces") and
      (actual \ "resourcePath" must_== expected \ "resourcePath") and {
        val ja = actual \ "apis" \ "path" \\ classOf[JString]
        (ja must haveSize(operationPaths.size)) and
          (ja must containTheSameElementsAs(operationPaths))
      }
  }

  def verifyOperation(actual: JValue, expected: JValue, name: String) = {
    val op = findOperation(actual, name)
    val exp = findOperation(expected, name)
    (op must beSome[JValue]).setMessage("Couldn't find operation: " + name) and {
      val m = verifyFields(op.get, exp.get, "method", "nickname", "type", "$ref", "items", "summary", "parameters", "notes", "responseMessages", "consumes", "produces", "protocols", "authorizations")
      m setMessage (m.message + " of the operation " + name)
    }
  }

  def verifyPetModel(actualPetJson: JValue) = {
    def petProperties(jv: JValue) = jv \ "models" \ "Pet" \ "properties"
    //    println("pet model " + jackson.prettyJson(actualPetJson))
    val actualPetProps = petProperties(actualPetJson)
    val expectedPetProps = petProperties(petOperationsJValue)

    val m = verifyFields(actualPetProps, expectedPetProps, "category", "id", "name", "status", "tags", "urls")
    m setMessage (m.message + " of the pet model")
  }

  def verifyStoreModel(actualStoreJson: JValue) = {
    def petProperties(jv: JValue) = jv \ "models" \ "Order" \ "properties"
    val actualOrderProps = petProperties(actualStoreJson)
    val expectedOrderProps = petProperties(storeOperationsJValue)

    val m = verifyFields(actualOrderProps, expectedOrderProps, "id", "status", "petId", "quantity", "shipDate")
    m setMessage (m.message + " of the order model")
  }

  def verifyFields(actual: JValue, expected: JValue, fields: String*): MatchResult[Any] = {
    def verifyField(act: JValue, exp: JValue, fn: String): MatchResult[Any] = {
      fn match {
        case "responseMessages" =>
          val af = act \ fn match {
            case JArray(res) => res
            case _ => Nil
          }
          val JArray(ef) = exp \ fn
          val r = af map { v =>
            val mm = verifyFields(
              v,
              ef find (_ \ "code" == v \ "code") getOrElse JNothing,
              "code", "message")
            mm setMessage (mm.message + " in response messages collection")
          }
          def countsmatch = (af.size must_== ef.size).setMessage("The count for the responseMessages is different")
          if (r.nonEmpty) { countsmatch and (r reduce (_ and _)) } else countsmatch
        case "parameters" =>
          val JArray(af) = act \ fn
          val JArray(ef) = exp \ fn
          val r = af map { v =>
            val mm = verifyFields(
              v,
              ef find (_ \ "name" == v \ "name") get,
              "allowableValues", "type", "$ref", "items", "paramType", "defaultValue", "description", "name", "required", "paramAccess")
            mm setMessage (mm.message + " in parameter " + (v \ "name").extractOrElse("N/A"))
          }

          if (r.nonEmpty) r reduce (_ and _) else 1.must_==(1)
        case _ =>
          val m = act \ fn must_== exp \ fn
          m setMessage (JsonMethods.compact(JsonMethods.render(act \ fn)) + " does not match\n" + JsonMethods.compact(JsonMethods.render(exp \ fn)) + " for field " + fn)
      }
    }

    fields map (verifyField(actual, expected, _)) reduce (_ and _)
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

class SwaggerTestServlet(protected val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {

  protected val applicationDescription = "Operations about pets"
  override protected val applicationName = Some("pet")
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val StringFormat = DefaultJsonFormats.GenericFormat(DefaultReaders.StringReader, DefaultWriters.StringWriter)

  protected override val swaggerProduces: List[String] = "application/json" :: "application/xml" :: "text/plain" :: "text/html" :: Nil

  protected override val swaggerConsumes: List[String] = Nil

  val data = new PetData

  get("/undocumented") {
    BadRequest("This should not show up")
  }
  //
  //  val rootOperation =
  //    (apiOperation[List[Pet]]("allPets")
  //      summary "Show all pets"
  //      notes "shows all the pets in the data store")
  //
  //  get("/", operation(rootOperation)) {
  //    data.pets
  //  }

  val getPet =
    (apiOperation[Pet]("getPetById")
      summary "Find pet by ID"
      notes "Returns a pet based on ID"
      responseMessages (StringResponseMessage(400, "Invalid ID supplied"), StringResponseMessage(404, "Pet not found"))
      parameter pathParam[String]("petId").description("ID of pet that needs to be fetched")
      produces ("application/json", "application/xml")
      authorizations ("oauth2"))

  get("/:petId", operation(getPet)) {
    data.getPetbyId(params.getAs[Long]("petId").getOrElse(0))
  }

  val createPet =
    (apiOperation[Unit]("addPet")
      summary "Add a new pet to the store"
      responseMessage StringResponseMessage(405, "Invalid input")
      parameter bodyParam[Pet].description("Pet object that needs to be added to the store"))

  post("/", operation(createPet)) {
    ApiResponse(ApiResponseType.OK, "pet added to store")
  }

  val updatePet =
    (apiOperation[Unit]("updatePet")
      summary "Update an existing pet"
      responseMessage StringResponseMessage(400, "Invalid ID supplied")
      responseMessage StringResponseMessage(404, "Pet not found")
      responseMessage StringResponseMessage(405, "Validation exception")
      parameter bodyParam[Pet].description("Pet object that needs to be updated in the store"))

  put("/", operation(updatePet)) {
    ApiResponse(ApiResponseType.OK, "pet updated")
  }

  val deletePet =
    (apiOperation[Unit]("deletePet")
      summary "Deletes a pet"
      responseMessage StringResponseMessage(400, "Invalid pet value")
      parameter pathParam[String]("petId").description("Pet id to delete"))

  delete("/:petId", operation(deletePet)) {
    ApiResponse(ApiResponseType.OK, "pet deleted")
  }

  val findByStatus =
    (apiOperation[List[Pet]]("findPetsByStatus").deprecate
      summary "Finds Pets by status"
      notes "Multiple status values can be provided with comma separated strings"
      produces ("application/json", "application/xml")
      responseMessage StringResponseMessage(400, "Invalid status value")
      parameter (queryParam[String]("status").required.multiValued
        description "Status values that need to be considered for filter"
        defaultValue "available"
        allowableValues ("available", "pending", "sold")))

  get("/findByStatus", operation(findByStatus)) {
    data.findPetsByStatus(params("status"))
  }

  val findByTags =
    (apiOperation[List[Pet]]("findPetsByTags").deprecate
      summary "Finds Pets by tags"
      notes "Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing."
      produces ("application/json", "application/xml")
      responseMessage StringResponseMessage(400, "Invalid tag value")
      parameter queryParam[String]("tags").description("Tags to filter by").multiValued)

  get("/findByTags", operation(findByTags)) {
    data.findPetsByTags(params("tags"))
  }
}

class StoreApi(val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {
  protected val applicationDescription = "Operations about store"
  override protected val applicationName = Some("store")
  protected implicit val jsonFormats: Formats = DefaultFormats
  implicit val StringFormat = DefaultJsonFormats.GenericFormat(DefaultReaders.StringReader, DefaultWriters.StringWriter)
  protected override val swaggerProduces: List[String] = "application/json" :: "application/xml" :: Nil

  protected override val swaggerConsumes: List[String] = Nil

  val getOrderOperation =
    (apiOperation[Order]("getOrderById")
      summary "Find purchase order by ID"
      notes "For valid response try integer IDs with value <= 5. Anything above 5 or nonintegers will generate API errors"
      produces ("application/json", "application/xml")
      parameter pathParam[String]("orderId").description("ID of pet that needs to be fetched").required
      responseMessages (
        StringResponseMessage(400, "Invalid ID supplied"),
        StringResponseMessage(404, "Order not found")
      ))

  get("/order/:orderId", operation(getOrderOperation)) {
    ""
  }

  val deleteOrderOperation =
    (apiOperation[Unit]("deleteOrder")
      summary "Delete purchase order by ID"
      notes "For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors"
      responseMessages (
        StringResponseMessage(400, "Invalid ID supplied"),
        StringResponseMessage(404, "Order not found")
      ))

  delete("/order/:orderId", operation(deleteOrderOperation)) {
    NoContent()
  }

  val placeOrderOperation =
    (apiOperation[Unit]("placeOrder")
      summary "Place an order for a pet"
      responseMessage StringResponseMessage(400, "Invalid order")
      parameter bodyParam[Order].description("order placed for purchasing the pet"))
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

case class Order(@ApiModelProperty(position = 1) id: Long,
  @ApiModelProperty(position = 2, description = "Order Status", allowableValues = "placed,approved,delivered") status: String,
  @ApiModelProperty(position = 3) petId: Long,
  @ApiModelProperty(position = 4) quantity: Int,
  @ApiModelProperty(position = 5) shipDate: DateTime)
case class User(id: Long, username: String, password: String, email: String, firstName: String, lastName: String, phone: String, userStatus: Int)
case class Pet(@ApiModelProperty(position = 3) id: Long,
  @ApiModelProperty(position = 1) category: Category,
  @ApiModelProperty(position = 2) name: String,
  @ApiModelProperty(position = 6) photoUrls: List[String],
  @ApiModelProperty(position = 4) tags: List[Tag],
  @ApiModelProperty(position = 5, description = "pet status in the store", allowableValues = "available,pending,sold") status: String)

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
