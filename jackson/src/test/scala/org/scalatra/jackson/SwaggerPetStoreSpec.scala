package org.scalatra
package jackson

import test.specs2.MutableScalatraSpec
import collection.mutable.ListBuffer
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

case class Category(id: Long = 0, name: String)
case class Tag(id: Long = 0, name: String)
case class Pet(id: Long = 0,
  category: Category,
  name: String,
  photoUrls: List[String],
  tags: List[Tag] = List.empty,
  status: String)

case class PetList(pets: List[Pet])

object PetData {
  val pets: ListBuffer[Pet] = new ListBuffer[Pet]()
  val categories: ListBuffer[Category] = new ListBuffer[Category]()

  categories ++= List(
      Category(1, "Dogs"),
      Category(2, "Cats"),
      Category(3, "Rabbits"),
      Category(4, "Lions"))

    pets ++= List(
      Pet(1, categories(1), "Cat 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
      Pet(2, categories(1), "Cat 2", List("url1", "url2"), List(Tag(2, "tag2"), Tag(3, "tag3")), "available"),
      Pet(3, categories(1), "Cat 3", List("url1", "url2"), List(Tag(3, "tag3"), Tag(4, "tag4")), "pending"),
      Pet(4, categories(0), "Dog 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
      Pet(5, categories(0), "Dog 2", List("url1", "url2"), List(Tag(2, "tag2"), Tag(3, "tag3")), "sold"),
      Pet(6, categories(0), "Dog 3", List("url1", "url2"), List(Tag(3, "tag3"), Tag(4, "tag4")), "pending"),
      Pet(7, categories(3), "Lion 1", List("url1", "url2"), List(Tag(1, "tag1"), Tag(2, "tag2")), "available"),
      Pet(8, categories(3), "Lion 2", List("url1", "url2"), List(Tag(2, "tag2"), Tag(3, "tag3")), "available"),
      Pet(9, categories(3), "Lion 3", List("url1", "url2"), List(Tag(3, "tag3"), Tag(4, "tag4")), "available"),
      Pet(10, categories(2), "Rabbit 1", List("url1", "url2"), List(Tag(3, "tag3"), Tag(4, "tag4")), "available"))

}

class SwaggerPetStoreSpecServlet extends ScalatraServlet with JacksonSupport with MagicJackson {
  get("/head") {
    PetData.pets.head
  }

  get("/") {
    PetData.pets.toList
  }
}
class SwaggerPetStoreSpec extends MutableScalatraSpec {

  addServlet(new SwaggerPetStoreSpecServlet, "/*")
  val jsonMapper = new ObjectMapper()
  val petjson =  """{"id":1,"category":{"id":2,"name":"Cats"},"name":"Cat 1","photoUrls":["url1","url2"],"tags":[{"id":1,"name":"tag1"},{"id":2,"name":"tag2"}],"status":"available"}"""
  val petxml = """<?xml version='1.0' encoding='UTF-8'?>
                 |<resp><id xmlns="">1</id><category xmlns=""><id>2</id><name>Cats</name></category><name xmlns="">Cat 1</name><photoUrls xmlns="">url1</photoUrls><photoUrls xmlns="">url2</photoUrls><tags xmlns=""><id>1</id><name>tag1</name></tags><tags xmlns=""><id>2</id><name>tag2</name></tags><status xmlns="">available</status></resp>""".stripMargin

  "The SwaggerPetStore" should {
    "return a pet as json" in {
      get("/head", headers = Map("Accept" -> "application/json")) {
        body must_== petjson
      }
    }

    "return a pet as xml" in {
      get("/head", headers = Map("Accept" -> "application/xml")) {
        body must_== petxml
      }
    }

    "return a list of pets as json" in {
//      get("/", headers = Map("Accept" -> "application/json")) {
//        body must_== petjson
//      }
    }

    "return a list of pets as json" in {
//      get("/", headers = Map("Accept" -> "application/xml")) {
//        body must_== petjson
//      }
    }
  }
}
