package org.scalatra
package swagger

import test.specs2.ScalatraSpec
import org.specs2._
import execute.Result
import matcher.{MatchResult, JsonMatchers}
import org.json4s._
import jackson.JsonMethods
import JsonDSL._
import org.json4s.native.JsonParser
import org.scalatra.json.{JValueResult, NativeJsonSupport}
import scala.io.Source
import java.net.ServerSocket

class SwaggerSpec extends ScalatraSpec with JsonMatchers { def is =
  "Swagger integration should"                                  ^
    "list resources"                       ! listResources       ^
    "list operations"                      ! listOperations     ^
  end

  val swagger = new Swagger("1.1", "1")
  val testServlet = new SwaggerTestServlet(swagger)

  addServlet(testServlet, "/pet/*")
  addServlet(new SwaggerResourcesServlet(swagger), "/*")
  implicit val formats = DefaultFormats

  /**
   * Sets the port to listen on.  0 means listen on any available port.
   */
  override lazy val port: Int = { val s = new ServerSocket(0); try { s.getLocalPort } finally { s.close() } }//58468

  val listResourceJValue = readJson("resources.json") merge (("basePath" -> ("http://localhost:" + port)):JValue)

  val listOperationsJValue = readJson("pet.json") merge (("basePath" -> ("http://localhost:" + port)):JValue)
  println(JsonMethods.pretty(JsonMethods.render(listOperationsJValue)))

  private def readJson(file: String) = {
    val f = if ( file startsWith "/" ) file else "/"+file
    val rdr = Source.fromInputStream(getClass.getResourceAsStream(f)).bufferedReader()
    JsonParser.parse(rdr)
  }


  def listResources = get("/resources.json") {
    JsonParser.parseOpt(body) must beSome(listResourceJValue)
  }

  val operations = "allPets" :: "updatePet" :: "addPet" :: "findByTags" :: "findPetsByStatus" :: "findById" :: Nil
//  val operations = "allPets" :: Nil
  def listOperations = {
    get("/pet.json") {
      val bo = JsonParser.parseOpt(body)
      bo must beSome[JValue] and
        verifyCommon(bo.get) and
        operations.map(verifyOperation(bo.get, _)).reduce(_ and _)
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
    (op must beSome[JValue]).setMessage("Couldn't find operation: " + name) and {
      val m = verifyFields(op.get, exp.get, "httpMethod", "nickname", "responseClass", "summary", "parameters", "notes", "errorResponses")
      m setMessage (m.message + " of the operation " + name)
    }
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
              ef find (_ \ "code" == v \ "code") get,
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

class SwaggerTestServlet(protected val swagger:Swagger) extends ScalatraServlet with NativeJsonSupport with JValueResult with SwaggerSupport {

  protected val applicationDescription = "Operations about pets"
  override protected val applicationName = Some("pet")
  protected implicit val jsonFormats: Formats = DefaultFormats

  val data = new PetData

  models = Map(classOf[Pet], classOf[Tag], classOf[Category])

  get("/",
      summary("Show all pets"),
      nickname("allPets"),
      responseClass[List[Pet]],
      endpoint(""),
      notes("shows all the pets in the data store")) {
    data.pets
  }

  get("/:id",
    summary("Find by ID"),
    nickname("findById"),
    responseClass[Pet],
    endpoint("{id}"),
    notes("Returns a pet when ID < 10. ID > 10 or nonintegers will simulate API error conditions"),
    errors(Error(400, "Invalid ID supplied"), Error(404, "Pet not found")),
    parameters(
      Parameter("id", "ID of pet that needs to be fetched", DataType.String, paramType = ParamType.Path))) {
      data.getPetbyId(params.getAs[Long]("petId").getOrElse(0))
    }

  post("/",
    summary("Add a new pet to the store"),
    nickname("addPet"),
    responseClass[Unit],
    errors(Error(400, "Invalid pet data supplied")),
    endpoint(""),
    parameters(
      Parameter("body", "Pet object that needs to be added to the store",
        DataType[Pet],
        paramType = ParamType.Body))) {
      ApiResponse(ApiResponseType.OK, "pet added to store")
    }

  put("/",
    summary("Update an existing pet"),
    nickname("updatePet"),
    responseClass[Unit],
    endpoint(""),
    errors(Error(404, "Pet not found")),
    parameters(
      Parameter("body", "Pet object that needs to be updated in the store",
        DataType[Pet],
        paramType = ParamType.Body))) {
      ApiResponse(ApiResponseType.OK, "pet updated")
    }

  get("/findByStatus",
    summary("Finds Pets by status"),
    nickname("findPetsByStatus"),
    responseClass[List[Pet]],
    endpoint("findByStatus"),
    notes("Multiple status values can be provided with comma separated strings"),
    parameters(
      Parameter("status",
        "Status values that need to be considered for filter",
        DataType.String,
        paramType = ParamType.Query,
        defaultValue = Some("available"),
        allowableValues = AllowableValues("available", "pending", "sold")))) {
      data.findPetsByStatus(params("status"))
    }

  get("/findByTags",
    summary("Finds Pets by tags"),
    nickname("findByTags"),
    responseClass[List[Pet]],
    endpoint("findByTags"),
    notes("Muliple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing."),
    parameters(
      Parameter("tags",
        "Tags to filter by",
        DataType.String,
        paramType = ParamType.Query))) {
      data.findPetsByTags(params("tags"))
    }
}

class SwaggerResourcesServlet(val swagger: Swagger) extends ScalatraServlet with NativeSwaggerBase 

case class Pet(id: Long, category: Category, name: String, urls: List[String], tags: List[Tag], status: String)
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