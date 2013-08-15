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
  case class Asset(name: String, filename: String, id: Option[Int])

  val taggedThingModels = Set(Swagger.modelToSwagger[Tag], Swagger.modelToSwagger[Name], Swagger.modelToSwagger[Sequence], Swagger.modelToSwagger[TaggedThing])
  val onlyPrimitivesModel = Swagger.modelToSwagger[OnlyPrimitives].get
  val assetModel = Swagger.modelToSwagger(Reflector.scalaTypeOf[Asset]).get

  case class Things(id: Long, taggedThings: List[TaggedThing], visits: List[Date], created: Date)
  val thingsModels = taggedThingModels + Swagger.modelToSwagger[Things]

  case class MapThings(id: Long, taggedThings: Map[String, TaggedThing], visits: Map[String, Date], created: Date)
  val mapThingsModels = taggedThingModels + Swagger.modelToSwagger[MapThings]

  case class Thing(id: Long, thing: Option[TaggedThing])
  val thingModels = taggedThingModels + Swagger.modelToSwagger[Thing]
}

class ModelCollectionSpec extends Specification {
  import ModelCollectionSpec._

  "Collect models" should {

    "return an empty set for a primitive" in {
      Swagger.collectModels[Int](Set.empty) must beEmpty
      Swagger.collectModels[Double](Set.empty) must beEmpty
      Swagger.collectModels[Long](Set.empty) must beEmpty
      Swagger.collectModels[Byte](Set.empty) must beEmpty
      Swagger.collectModels[Short](Set.empty) must beEmpty
      Swagger.collectModels[Float](Set.empty) must beEmpty
      Swagger.collectModels[Char](Set.empty) must beEmpty
      Swagger.collectModels[String](Set.empty) must beEmpty
      Swagger.collectModels[Unit](Set.empty) must beEmpty
      Swagger.collectModels[Date](Set.empty) must beEmpty
    }

    "only return the top level object for a case class with only primitives" in {
      Swagger.collectModels[OnlyPrimitives](Set.empty) must_== Set(onlyPrimitivesModel)
    }

    "collect all the models in a nested structure" in {
      Swagger.collectModels[TaggedThing](Set.empty) must haveTheSameElementsAs(taggedThingModels.flatten)
    }

    "collect models when hiding in a list" in {
      val collected = Swagger.collectModels[Things](Set.empty)
      collected.map(_.id) must haveTheSameElementsAs(thingsModels.flatten.map(_.id))
    }
    "collect models when hiding in a map" in {
      val collected = Swagger.collectModels[MapThings](Set.empty)
      collected must haveTheSameElementsAs(mapThingsModels.flatten)

    }

    "collect models when provided as a list" in {
      val collected = Swagger.collectModels[List[Name]](Set.empty)
      collected.map(_.id) must_== Set(Swagger.modelToSwagger[Name].get).map(_.id)
    }

    "collect models when hiding in an option" in {
      val collected = Swagger.collectModels[Thing](Set.empty)
      collected must haveTheSameElementsAs(thingModels.flatten)
      collected.find(_.id == "Thing").map(_.properties("thing").`type`) must beSome(DataType[TaggedThing])
    }

    "collect Asset model" in {
      val collected = Swagger.collectModels[Asset](Set.empty)
      collected.head must_== assetModel
    }
  }
}