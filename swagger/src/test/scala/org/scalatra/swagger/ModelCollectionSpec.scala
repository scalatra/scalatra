package org.scalatra.swagger

import java.util.Date

import org.scalatra.swagger.reflect.Reflector
import org.specs2.mutable.Specification

object ModelCollectionSpec {
  case class OnlyPrimitives(id: Int, sequence: Long, deviation: Double, name: String, created: Date)

  case class Tag(id: Sequence, name: Name)
  case class Name(value: String)
  case class Sequence(value: Long)
  case class TaggedThing(id: Long, tag: Tag, created: Date)
  case class Asset(name: String, filename: String, id: Option[Int], relatedAsset: Asset)

  val taggedThingModels = Set(Swagger.modelToSwagger[Tag], Swagger.modelToSwagger[Name], Swagger.modelToSwagger[Sequence], Swagger.modelToSwagger[TaggedThing])
  val onlyPrimitivesModel = Swagger.modelToSwagger[OnlyPrimitives].get
  val assetModel = Swagger.modelToSwagger(Reflector.scalaTypeOf[Asset]).get

  case class Things(id: Long, taggedThings: List[TaggedThing], visits: List[Date], created: Date)
  val thingsModels = taggedThingModels + Swagger.modelToSwagger[Things]

  case class MapThings(id: Long, taggedThings: Map[String, TaggedThing], visits: Map[String, Date], created: Date)
  val mapThingsModels = taggedThingModels + Swagger.modelToSwagger[MapThings]

  case class Thing(id: Long, thing: Option[TaggedThing])
  val thingModels = taggedThingModels + Swagger.modelToSwagger[Thing]

  case class OptionListThing(id: Long, things: Option[List[TaggedThing]])
  val optionThingModels = taggedThingModels + Swagger.modelToSwagger[OptionListThing]
}

class ModelCollectionSpec extends Specification {
  import org.scalatra.swagger.ModelCollectionSpec._

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

    "not return Options" in {
      Swagger.collectModels[Option[String]](Set.empty) must beEmpty
      Swagger.collectModels[Option[OnlyPrimitives]](Set.empty) must_== Set(onlyPrimitivesModel)
    }

    "not return Lists" in {
      Swagger.collectModels[List[String]](Set.empty) must beEmpty
      Swagger.collectModels[List[OnlyPrimitives]](Set.empty) must_== Set(onlyPrimitivesModel)
    }

    "only return the top level object for a case class with only primitives" in {
      Swagger.collectModels[OnlyPrimitives](Set.empty) must_== Set(onlyPrimitivesModel)
    }

    "collect all the models in a nested structure" in {
      Swagger.collectModels[TaggedThing](Set.empty) must containTheSameElementsAs(taggedThingModels.flatten.toSeq)
    }

    "collect models when hiding in a list" in {
      val collected = Swagger.collectModels[Things](Set.empty)
      collected.map(_.id) must containTheSameElementsAs(thingsModels.flatten.map(_.id).toSeq)
    }
    "collect models when hiding in a map" in {
      val collected = Swagger.collectModels[MapThings](Set.empty)
      collected must containTheSameElementsAs(mapThingsModels.flatten.toSeq)

    }

    "collect models when provided as a list" in {
      val collected = Swagger.collectModels[List[Name]](Set.empty)
      collected.map(_.id) must_== Set(Swagger.modelToSwagger[Name].get).map(_.id)
    }

    "collect models when provided as a list inside an option" in {
      val collected = Swagger.collectModels[OptionListThing](Set.empty)
      val r = collected.find(_.id == "OptionListThing")
      r.flatMap(_.properties.find(_._1 == "things").map(_._2.`type`)) must beSome(DataType[List[TaggedThing]])
      r.flatMap(_.properties.find(_._1 == "things").map(_._2.required)) must beSome(false)
    }

    "collect models when hiding in an option" in {
      val collected = Swagger.collectModels[Thing](Set.empty)
      collected must containTheSameElementsAs(thingModels.flatten.toSeq)
      collected.find(_.id == "Thing").flatMap(_.properties.find(_._1 == "thing").map(_._2.`type`)) must beSome(DataType[TaggedThing])
    }

    "collect Asset model" in {
      val collected = Swagger.collectModels[Asset](Set.empty)
      collected.head must_== assetModel
    }

  }
}