package org.scalatra
package swagger

import test.specs2.ScalatraSpec
import org.specs2._
import matcher.JsonMatchers
import org.json4s._
import JsonDSL._
import org.json4s.native.JsonParser
import org.scalatra.json.{JValueResult, NativeJsonSupport}
import scala.io.Source

class SwaggerSpec extends ScalatraSpec with JsonMatchers { def is =
  "Swagger integration should"                                  ^
    "list resources"                       ! listResources       ^
    "list operations"                      ! listOperations     ^
  end

  val swagger = new Swagger("1.1", "1")
  val testServlet = new SwaggerTestServlet(swagger)

//  swagger register("test", "/test", "Test", testServlet)
  addServlet(testServlet, "/pet/*")
  addServlet(new SwaggerResourcesServlet(swagger), "/*")

  /**
   * Sets the port to listen on.  0 means listen on any available port.
   */
  override def port: Int = 58468

  val listResourceJValue = readJson("resources.json") merge (("basePath" -> ("http://localhost:" + port + "/")):JValue)

  val listOperationsJValue = readJson("pet.json")

  private def readJson(file: String) = {
    val f = if (file.startsWith("/")) file else "/"+file
    val rdr = Source.fromInputStream(getClass.getResourceAsStream(f)).bufferedReader()
    JsonParser.parse(rdr)
  }


  def listResources = get("/resources.json") {
    JsonParser.parseOpt(body) must beSome(listResourceJValue)
  }

  def listOperations = {
    pending
//    get("/pet.json") {
//      parseOpt(body) must beSome(listOperationsJValue)
//    }
  }
}

class SwaggerTestServlet(protected val swagger:Swagger) extends ScalatraServlet with TypedParamSupport with NativeJsonSupport with JValueResult with SwaggerSupport {

  protected val applicationDescription = "The pets api"
  override protected val applicationName = Some("pet")
  protected implicit val jsonFormats: Formats = DefaultFormats

  val data = new PetData

  models = Map(classOf[Pet])

  get("/",
      summary("Show all pets"),
      nickname("allPets"),
      responseClass("Pet"),
      endpoint(""),
      notes("shows all the pets in the data store")) {
    data.pets
  }
  get("/:id",
    summary("Find by ID"),
    nickname("findById"),
    responseClass("Pet"),
    endpoint("{id}"),
    notes("Returns a pet when ID < 10. ID > 10 or nonintegers will simulate API error conditions"),
    parameters(
      Parameter("id", "ID of pet that needs to be fetched",
        DataType.String,
        paramType = ParamType.Path))) {
      data.getPetbyId(params.getAs[Long]("id").getOrElse(0))
    }

  post("/",
    summary("Add a new pet to the store"),
    nickname("addPet"),
    responseClass("void"),
    endpoint(""), // TODO shouldn't this be from the first param?  also missing the .{format}
    parameters(
      Parameter("body", "Pet object that needs to be added to the store",
        DataType[Pet],
        paramType = ParamType.Body))) {
      ApiResponse(ApiResponseType.OK, "pet added to store")
    }

  put("/",
    summary("Update an existing pet"),
    nickname("updatePet"),
    responseClass("void"),
    endpoint(""),
    parameters(
      Parameter("body", "Pet object that needs to be updated in the store",
        DataType[Pet],
        paramType = ParamType.Body))) {
      ApiResponse(ApiResponseType.OK, "pet updated")
    }

  get("/findByStatus",
    summary("Finds Pets by status"),
    nickname("findPetsByStatus"),
    responseClass("Pet"), // TODO: missing multi-valued response?
    endpoint("findByStatus"),
    notes("Multiple status values can be provided with comma seperated strings"),
    parameters(
      Parameter("status",
        "Status values that need to be considered for filter",
        DataType.String,
        paramType = ParamType.Query,
        defaultValue = Some("available"),
        allowableValues = AllowableValues("available", "unavailable", "none")))) { // TODO: set allowable values
      data.findPetsByStatus(params("status"))
    }

  get("/findByTags",
    summary("Finds Pets by tags"),
    nickname("findByTags"),
    responseClass[Pet], // TODO: missing multi-valued response?
    endpoint("findByTags"),
    notes("Muliple tags can be provided with comma seperated strings. Use tag1, tag2, tag3 for testing."),
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

  def getPetbyId(id: Long): Option[Pet] = {
    pets.filter(pet => pet.id == id) match {
      case pets: List[Pet] if pets.size > 0 => Some(pets.head)
      case _ => None
    }
  }

  def findPetsByStatus(status: String): List[Pet] = {
    val statuses = status.split(",").toSet
    pets.filter(pet => {
      if((statuses.contains(pet.status))) true
      else false
    })
  }

  def findPetsByTags(tag: String): List[Pet] = {
    val tags = tag.split(",").toSet
    pets.filter(pet => {
      if((tags & pet.tags.map(f=>f.name).toSet).size > 0) true
      else false
    })
  }

  def addPet(pet: Pet) = {
    // remove any pets with same id
    pets = List(pet) ++ pets.filter(p => p.id == pet.id)
  }
}