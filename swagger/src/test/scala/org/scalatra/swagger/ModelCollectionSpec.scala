package org.scalatra.swagger

import org.specs2.mutable.Specification
import reflect.Reflector
import java.util.Date

object ModelCollectionSpec {
  case class OnlyPrimitives(id: Int, sequence: Long, deviation: Double, name: String, created: Date)

  case class Tag(id: Sequence, name: Name)
  case class Name(value: String)
  case class Sequence(value: Long)
  case class TaggedThing(id: Long, tag: Tag, created: Date)

  val taggedThingModels = Set(Swagger.modelToSwagger[Tag], Swagger.modelToSwagger[Name], Swagger.modelToSwagger[Sequence], Swagger.modelToSwagger[TaggedThing])
  val onlyPrimitivesModel = Swagger.modelToSwagger[OnlyPrimitives]

  case class Things(id: Long, taggedThings: List[TaggedThing], visits: List[Date], created: Date)
  val thingsModels = taggedThingModels + Swagger.modelToSwagger[Things]

  case class MapThings(id: Long, taggedThings: Map[String, TaggedThing], visits: Map[String, Date], created: Date)
  val mapThingsModels = taggedThingModels + Swagger.modelToSwagger[MapThings]
}

class ModelCollectionSpec extends Specification {
  import ModelCollectionSpec._

  "Collect models" should {

    "return an empty set for a primitive" in {
      Swagger.collectModels[Int] must beEmpty
      Swagger.collectModels[Double] must beEmpty
      Swagger.collectModels[Long] must beEmpty
      Swagger.collectModels[Byte] must beEmpty
      Swagger.collectModels[Short] must beEmpty
      Swagger.collectModels[Float] must beEmpty
      Swagger.collectModels[Char] must beEmpty
      Swagger.collectModels[String] must beEmpty
      Swagger.collectModels[Unit] must beEmpty
      Swagger.collectModels[Date] must beEmpty
    }

    "only return the top level object for a case class with only primitives" in {
      Swagger.collectModels[OnlyPrimitives] must_== Set(onlyPrimitivesModel)
    }

    "collect all the models in a nested structure" in {
      Swagger.collectModels[TaggedThing] must haveTheSameElementsAs(taggedThingModels)
    }

    "collect models when hiding in a list" in {
      val collected = Swagger.collectModels[Things]
      println("Collected: " + collected.map(_.id))
      collected.map(_.id) must haveTheSameElementsAs(thingsModels.map(_.id))
    }
    "collect models when hiding in a map" in {
      val collected = Swagger.collectModels[MapThings]

      collected must haveTheSameElementsAs(mapThingsModels)
    }

    "collect models when provided as a list" in {
      val collected = Swagger.collectModels[List[Name]]
      println("Collected: " + collected.map(_.id))
      collected.map(_.id) must_== Set(Swagger.modelToSwagger[Name]).map(_.id)
    }
  }
}