package org.scalatra
package swagger

import java.net.ServerSocket
import java.time.OffsetDateTime

import org.json4s.*
import org.json4s.jackson.JsonMethods
import org.json4s.native.JsonParser
import org.scalatra.json.NativeJsonSupport
import org.scalatra.swagger.annotations.*
import org.scalatra.test.specs2.ScalatraSpec
import org.specs2.matcher.{JsonMatchers, MatchResult}

import scala.collection.mutable
import scala.io.Source

/** TestCase for Swagger 2.0 support
  */
class SwaggerSpec2 extends ScalatraSpec with JsonMatchers {
  def is = sequential ^
    "Swagger 2.0 integration should" ^
    "generate api definitions" ! generateApiDefinitions ^ end

  val extraSwagger: JValue = JsonMethods.parse("""{
                                                 |  "definitions": {
                                                 |    "Dog": {
                                                 |      "properties": {
                                                 |        "name": {"type":  "string"}
                                                 |      }
                                                 |    }
                                                 |  }
                                                 | }""".stripMargin)

  val swagger = new Swagger("2.0", "1.0.0", TestFixtures.apiInfo, extraSwaggerDefinition = Some(extraSwagger))
  swagger.addAuthorization(BasicAuth("basicAuth"))
  swagger.addAuthorization(ApiKey("apiKey"))
  swagger.addAuthorization(ApiKey("Authorization1", "query", "you must register your app to receive an apikey"))
  swagger.addAuthorization(
    OAuth(
      List("PUBLIC"),
      List(
        ImplicitGrant(LoginEndpoint("http://localhost:8002/oauth/dialog"), "access_code"),
        AuthorizationCodeGrant(
          TokenRequestEndpoint("http://localhost:8002/oauth/requestToken", "client_id", "client_secret"),
          TokenEndpoint("http://localhost:8002/oauth/token", "access_code")
        ),
        ApplicationGrant(TokenEndpoint("http://localhost:8002/oauth/token", "access_code"))
      )
    )
  )
  swagger.addAuthorization(
    OAuth(
      List("PUBLIC"),
      List(
        ImplicitGrant(LoginEndpoint("http://localhost:8002/oauth/dialog"), "access_code"),
        AuthorizationCodeGrant(
          TokenRequestEndpoint("http://localhost:8002/oauth/requestToken", "client_id", "client_secret"),
          TokenEndpoint("http://localhost:8002/oauth/token", "access_code")
        )
      ),
      "AuthorizationN",
      "obtain limited access to service"
    )
  )
  val testServlet = new SwaggerTestServlet(swagger)

  addServlet(testServlet, "/pet/*")
  addServlet(new StoreApi(swagger), "/store/*")
  addServlet(new UserApi(swagger), "/user/*")
  addServlet(new SwaggerResourcesServlet(swagger), "/api-docs/*")
  implicit val formats: Formats = DefaultFormats

  /** Sets the port to listen on. 0 means listen on any available port.
    */
  override lazy val port: Int = {
    val s = new ServerSocket(0);
    try { s.getLocalPort }
    finally { s.close() }
  } // 58468

  val swaggerJsonJValue = readJson("swagger.json")

  private def readJson(file: String) = {
    val f   = if (file startsWith "/") file else "/" + file
    val rdr = Source.fromInputStream(getClass.getResourceAsStream(f)).bufferedReader()
    JsonParser.parse(rdr)
  }

  def generateApiDefinitions = {
    get("/api-docs/swagger.json") {
      val bd = JsonParser.parseOpt(body)
      bd must beSome[JValue] and {
        val j = bd.get
        (j \ "version" must_== swaggerJsonJValue \ "version") and
          verifyInfo(j \ "info") and
          verifyPaths(j \ "paths") and
          verifyDefinitions(j \ "definitions") and
          verifySecurityDefinitions(j \ "securityDefinitions")
      }
    }
  }

  def parseInt(i: String): Option[Int] =
    try {
      Some(Integer.parseInt(i))
    } catch {
      case _: Throwable => None
    }

  def verifyPaths(j: JValue) = {
    val JObject(paths) = j
    val expectations   = mutable.HashMap(
      ("/pet/findByTags", "get")           -> "findPetsByTags",
      ("/pet/{petId}", "delete")           -> "deletePet",
      ("/pet/{petId}", "get")              -> "getPetById",
      ("/pet/findByStatus", "get")         -> "findPetsByStatus",
      ("/pet/", "post")                    -> "addPet",
      ("/pet/", "put")                     -> "updatePet",
      ("/store/order", "post")             -> "placeOrder",
      ("/store/order/{orderId}", "delete") -> "deleteOrder",
      ("/store/order/{orderId}", "get")    -> "getOrderById",
      ("/user/", "post")                   -> "createUser"
    )
    paths flatMap { case (path, JObject(x)) =>
      x map { case (method, operation) =>
        val operationId = expectations((path, method))
        expectations -= ((path, method))
        (JString(operationId) must_== operation \ "operationId") and
          verifyOperation(operation, swaggerJsonJValue \ "paths" \ path \ method, operationId)
      }
    } reduce (_ and _) and (expectations must beEmpty)
  }

  def verifyInfo(j: JValue) = {
    val info = swaggerJsonJValue \ "info"
    (j \ "title" must_== info \ "title") and
      (j \ "version" must_== info \ "version") and
      (j \ "description" must_== info \ "description") and
      (j \ "termsOfService" must_== info \ "termsOfService") and
      (j \ "contact" \ "name" must_== info \ "contact" \ "name") and
      (j \ "contact" \ "url" must_== info \ "contact" \ "url") and
      (j \ "contact" \ "email" must_== info \ "contact" \ "email") and
      (j \ "license" \ "name" must_== info \ "license" \ "name") and
      (j \ "license" \ "url" must_== info \ "license" \ "url")
  }

  def verifyDefinitions(j: JValue) = {
    val JObject(definitions) = j
    definitions
      .flatMap { case (modelName, model) =>
        val JObject(properties) = model \ "properties"
        properties.map { case (propertyName, property) =>
          verifyProperty(
            property,
            swaggerJsonJValue \ "definitions" \ modelName \ "properties" \ propertyName,
            propertyName
          )
        }
      }
      .reduce(_ and _)
  }

  def verifySecurityDefinitions(j: JValue) = {
    val JObject(definitions) = j
    definitions
      .map { case (name, definition) =>
        verifyFields(
          definition,
          swaggerJsonJValue \ "securityDefinitions" \ name,
          "type",
          "name",
          "description",
          "in",
          "flow",
          "authorizationUrl",
          "scopes"
        )
      }
      .reduce(_ and _)
  }

  def verifyProperty(actual: JValue, expected: JValue, propertyName: String) = {
    val m = verifyFields(
      actual,
      expected,
      "type",
      "format",
      "$ref",
      "items",
      "description",
      "minimum",
      "maximum",
      "enum",
      "default",
      "example"
    )
    m setMessage (m.message + " of the property " + propertyName)
  }

  def verifyOperation(actual: JValue, expected: JValue, operationId: String) = {
    val m = verifyFields(
      actual,
      expected,
      "operationId",
      "summary",
      "schemes",
      "consumes",
      "produces",
      "deprecated",
      "parameters",
      "responses",
      "security",
      "tags"
    )
    m setMessage (m.message + " of the operation " + operationId)
  }

  def verifyFields(actual: JValue, expected: JValue, fields: String*): MatchResult[Any] = {
    def verifyField(act: JValue, exp: JValue, fn: String): MatchResult[Any] = {
      fn match {
        case "additionalProperties" =>
          verifyFields(act \ fn, exp \ fn, "type", "$ref")
        case "schema" =>
          verifyFields(act \ fn, exp \ fn, "type", "items", "$ref", "additionalProperties")
        case "items" =>
          verifyFields(act \ fn, exp \ fn, "type", "$ref")
        case "responses" =>
          val af = act \ fn match {
            case JObject(res) => res
            case _            => Nil
          }
          val JObject(ef) = exp \ fn
          val r           = af map { case (af_code, af_value) =>
            val mm = verifyFields(
              af_value,
              ef collectFirst { case (ef_code, ef_value) if ef_code == af_code => ef_value } getOrElse JNothing,
              "schema",
              "description"
            )
            mm setMessage (mm.message + " in response messages collection")
          }
          def countsmatch = (af.size must_== ef.size).setMessage("The count for the responseMessages is different")
          if (r.nonEmpty) { countsmatch and (r reduce (_ and _)) }
          else countsmatch
        case "parameters" =>
          val JArray(af) = act \ fn
          val JArray(ef) = exp \ fn
          val r          = af map { v =>
            val mm = verifyFields(
              v,
              ef.find(_ \ "name" == v \ "name").get,
              "allowableValues",
              "type",
              "$ref",
              "items",
              "paramType",
              "defaultValue",
              "description",
              "name",
              "required",
              "paramAccess",
              "example",
              "minimumValue",
              "maximumValue"
            )
            mm setMessage (mm.message + " in parameter " + (v \ "name").extractOrElse("N/A"))
          }

          if (r.nonEmpty) r reduce (_ and _) else 1.must_==(1)
        case _ =>
          val m = act \ fn must_== exp \ fn
          m setMessage (JsonMethods.compact(JsonMethods.render(act \ fn)) + " does not match\n" + JsonMethods.compact(
            JsonMethods.render(exp \ fn)
          ) + " for field " + fn)
      }
    }

    fields map (verifyField(actual, expected, _)) reduce (_ and _)
  }

}

/** TestCase for Swagger 2.0 support when no extra definition are passed
  */
class SwaggerSpecWithoutCustom2 extends ScalatraSpec with JsonMatchers {
  def is = sequential ^
    "Swagger 2.0 integration should" ^
    "generate api definitions" ! generateApiDefinitions ^ end

  val apiInfo = TestFixtures.apiInfo

  val swagger = new Swagger("2.0", "1.0.0", apiInfo)
  addServlet(new SwaggerResourcesServlet(swagger), "/api-docs/*")
  implicit val formats: Formats = DefaultFormats

  /** Sets the port to listen on. 0 means listen on any available port.
    */
  override lazy val port: Int = {
    val s = new ServerSocket(0);
    try { s.getLocalPort }
    finally { s.close() }
  } // 58468

  val swaggerJsonJValue = readJson("swagger.json")

  private def readJson(file: String) = {
    val f   = if (file startsWith "/") file else "/" + file
    val rdr = Source.fromInputStream(getClass.getResourceAsStream(f)).bufferedReader()
    JsonParser.parse(rdr)
  }

  def generateApiDefinitions = {
    get("/api-docs/swagger.json") {
      val bd = JsonParser.parseOpt(body)
      bd must beSome[JValue] and {
        val j = bd.get
        (j \ "version" must_== swaggerJsonJValue \ "version") and
          verifyInfo(j \ "info")
      }
    }
  }

  def parseInt(i: String): Option[Int] =
    try {
      Some(Integer.parseInt(i))
    } catch {
      case _: Throwable => None
    }

  def verifyInfo(j: JValue) = {
    val info = swaggerJsonJValue \ "info"
    (j \ "title" must_== info \ "title") and
      (j \ "version" must_== info \ "version") and
      (j \ "description" must_== info \ "description") and
      (j \ "termsOfService" must_== info \ "termsOfService") and
      (j \ "contact" \ "name" must_== info \ "contact" \ "name") and
      (j \ "contact" \ "url" must_== info \ "contact" \ "url") and
      (j \ "contact" \ "email" must_== info \ "contact" \ "email") and
      (j \ "license" \ "name" must_== info \ "license" \ "name") and
      (j \ "license" \ "url" must_== info \ "license" \ "url")
  }
}

class SwaggerTestServlet(protected val swagger: Swagger)
    extends ScalatraServlet
    with NativeJsonSupport
    with SwaggerSupport {

  protected val applicationDescription          = "Operations about pets"
  protected implicit val jsonFormats: Formats   = DefaultFormats
  implicit val StringFormat: JsonFormat[String] =
    JsonFormat.GenericFormat(using DefaultReaders.StringReader, DefaultWriters.StringWriter)

  protected override val swaggerProduces: List[String] =
    "application/json" :: "application/xml" :: "text/plain" :: "text/html" :: Nil

  protected override val swaggerConsumes: List[String] = Nil

  override protected def swaggerTag: Option[String] = Some("Pet")

  val data = new PetData

  get("/undocumented") {
    BadRequest("This should not show up")
  }

  val getPet =
    (apiOperation[Pet]("getPetById")
      summary "Find pet by ID"
      description "Returns a pet based on ID"
      responseMessages (ResponseMessage(400, "Invalid ID supplied").model[Error], ResponseMessage(404, "Pet not found"))
      parameter pathParam[String]("petId").description("ID of pet that needs to be fetched")
      produces ("application/json", "application/xml")
      authorizations ("oauth2"))

  get("/:petId", operation(getPet)) {
    data.getPetbyId(params.getAs[Long]("petId").getOrElse(0))
  }

  val createPet =
    (apiOperation[Unit]("addPet")
      summary "Add a new pet to the store"
      responseMessage ResponseMessage(405, "Invalid input")
      parameter bodyParam[Pet].description("Pet object that needs to be added to the store")
      authorizations ("basicAuth"))

  post("/", operation(createPet)) {
    ApiResponse(ApiResponseType.OK, "pet added to store")
  }

  val updatePet =
    (apiOperation[Unit]("updatePet")
      summary "Update an existing pet"
      responseMessage ResponseMessage(400, "Invalid ID supplied")
      responseMessage ResponseMessage(404, "Pet not found")
      responseMessage ResponseMessage(405, "Validation exception")
      parameter bodyParam[Pet].description("Pet object that needs to be updated in the store"))

  put("/", operation(updatePet)) {
    ApiResponse(ApiResponseType.OK, "pet updated")
  }

  val deletePet =
    (apiOperation[Unit]("deletePet")
      summary "Deletes a pet"
      authorizations ("Authorization1")
      responseMessage ResponseMessage(400, "Invalid pet value")
      parameter pathParam[String]("petId").description("Pet id to delete"))

  delete("/:petId", operation(deletePet)) {
    ApiResponse(ApiResponseType.OK, "pet deleted")
  }

  val findByStatus =
    (apiOperation[List[Pet]]("findPetsByStatus").deprecate
      summary "Finds Pets by status"
      description "Multiple status values can be provided with comma separated strings"
      produces ("application/json", "application/xml")
      responseMessage ResponseMessage(400, "Invalid status value")
      authorizations ("apiKey")
      parameter (queryParam[String]("status").required.multiValued
        description "Status values that need to be considered for filter"
        defaultValue "available"
        allowableValues ("available", "pending", "sold")))

  get("/findByStatus", operation(findByStatus)) {
    data.findPetsByStatus(params("status"))
  }

  val findByTags =
    (apiOperation[Map[String, Pet]]("findPetsByTags").deprecate
      summary "Finds Pets by tags"
      description "Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing."
      produces ("application/json", "application/xml")
      authorizations ("AuthorizationN")
      responseMessage ResponseMessage(400, "Invalid tag value")
      parameter queryParam[String]("tags").description("Tags to filter by").multiValued)

  get("/findByTags", operation(findByTags)) {
    data.findPetsByTags(params("tags"))
  }
}

class StoreApi(val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {
  protected val applicationDescription          = "Operations about store"
  protected implicit val jsonFormats: Formats   = DefaultFormats
  implicit val StringFormat: JsonFormat[String] =
    JsonFormat.GenericFormat(using DefaultReaders.StringReader, DefaultWriters.StringWriter)
  protected override val swaggerProduces: List[String] = "application/json" :: "application/xml" :: Nil

  protected override val swaggerConsumes: List[String] = Nil

  val getOrderOperation =
    (apiOperation[Order]("getOrderById")
      summary "Find purchase order by ID"
      description "For valid response try integer IDs with value <= 5. Anything above 5 or nonintegers will generate API errors"
      produces ("application/json", "application/xml")
      tags ("store")
      parameter pathParam[String]("orderId").description("ID of pet that needs to be fetched").required.example("1")
      parameter queryParam[String]("showCoolStuff").hidden
      responseMessages (ResponseMessage(400, "Invalid ID supplied"), ResponseMessage(404, "Order not found")))

  get("/order/:orderId", operation(getOrderOperation)) {
    ""
  }

  val deleteOrderOperation =
    (apiOperation[Unit]("deleteOrder")
      summary "Delete purchase order by ID"
      description "For valid response try integer IDs with value < 1000. Anything above 1000 or nonintegers will generate API errors"
      tags ("store")
      responseMessages (ResponseMessage(400, "Invalid ID supplied"), ResponseMessage(404, "Order not found")))

  delete("/order/:orderId", operation(deleteOrderOperation)) {
    NoContent()
  }

  val placeOrderOperation =
    (apiOperation[Order]("placeOrder")
      summary "Place an order for a pet"
      tags ("store")
      responseMessage ResponseMessage(400, "Invalid order")
      responseMessages (ResponseMessage(201, "Created", Some(Order.toString())), ResponseMessage(400, "Invalid order"))
      parameter bodyParam[Order].description("order placed for purchasing the pet"))

  post("/order", operation(placeOrderOperation)) {
    ""
  }
}

class UserApi(val swagger: Swagger) extends ScalatraServlet with NativeJsonSupport with SwaggerSupport {
  protected val applicationDescription          = "Operations about user"
  protected implicit val jsonFormats: Formats   = DefaultFormats
  implicit val StringFormat: JsonFormat[String] =
    JsonFormat.GenericFormat(using DefaultReaders.StringReader, DefaultWriters.StringWriter)

  override protected def swaggerTag: Option[String] = Some("User")

  val createUserOperation = apiOperation[User]("createUser")
  post("/", operation(createUserOperation)) {
    ""
  }
}

class SwaggerResourcesServlet(val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase

case class Order(
    @ApiModelProperty(position = 1) id: Long,
    @ApiModelProperty(
      position = 2,
      description = "Order Status",
      allowableValues = "placed,approved,delivered"
    ) status: String,
    @ApiModelProperty(position = 3) petId: Long,
    @ApiModelProperty(position = 4, allowableValues = "range[0,10]", defaultValue = "1", example = "1") quantity: Int,
    @ApiModelProperty(position = 5) shipDate: OffsetDateTime,
    @ApiModelProperty(hidden = true, required = true) shipped: Boolean,
    @ApiModelProperty(position = 6, minimumValue = 0, defaultValue = "1", example = "2.99") price: Double
)
case class User(
    id: Long,
    username: String,
    password: String,
    email: String,
    firstName: String,
    lastName: String,
    phone: String,
    userStatus: Int
)
case class Pet(
    @ApiModelProperty(position = 3) id: Long,
    @ApiModelProperty(position = 1) category: Category,
    @ApiModelProperty(position = 2) name: String,
    @ApiModelProperty(position = 6, defaultValue = """["a","b","c"]""", example = """["a"]""") photoUrls: List[String],
    @ApiModelProperty(position = 4) tags: List[Tag],
    @ApiModelProperty(
      position = 5,
      description = "pet status in the store",
      allowableValues = "available,pending,sold"
    ) status: String,
    @ApiModelProperty(
      position = 7,
      description = "Define if the animal is vegetarian",
      required = false,
      defaultValue = "true",
      example = "true"
    ) isVegetarian: Option[Boolean] = None
)

case class Tag(id: Long, name: String)
case class Category(id: Long, name: String)
case class Error(message: String)

case class ApiResponse(code: String, msg: String)

object ApiResponseType {
  val ERROR    = "error"
  val WARNING  = "warning"
  val INFO     = "info"
  val OK       = "ok"
  val TOO_BUSY = "too busy"
}

class PetData {
  var categories = List(Category(1, "Dogs"), Category(2, "Cats"), Category(3, "Rabbits"), Category(4, "Lions"))

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
    Pet(
      10,
      categories(2),
      "Rabbit 1",
      List("url1", "url2"),
      List(Tag(1, "tag1"), Tag(2, "tag2")),
      "available",
      Some(false)
    )
  )

  def getPetbyId(id: Long): Option[Pet] = pets.find(_.id == id)

  def findPetsByStatus(status: String): List[Pet] = {
    val statuses = status.split(",").toSet
    pets.filter(pet => statuses.contains(pet.status))
  }

  def findPetsByTags(tag: String): Map[String, Pet] = {
    val tags = tag.split(",").toSet
    pets.collect { case pet if (tags & pet.tags.map(_.name).toSet).nonEmpty => pet.name -> pet }.toMap
  }

  def addPet(pet: Pet) = {
    // remove any pets with same id
    pets = List(pet) ++ pets.filter(p => p.id == pet.id)
  }
}

object TestFixtures {
  val apiInfo = ApiInfo(
    title = "Swagger Sample App",
    description =
      "This is a sample server Petstore server.  You can find out more about Swagger \n    at <a href=\"http://swagger.wordnik.com\">http://swagger.wordnik.com</a> or on irc.freenode.net, #swagger.",
    termsOfServiceUrl = "http://helloreverb.com/terms/",
    contact = ContactInfo(name = "helloreverb apiteam", url = "http://helloreverb.com/", email = "apiteam@wordnik.com"),
    license = LicenseInfo(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0.html")
  )
}
