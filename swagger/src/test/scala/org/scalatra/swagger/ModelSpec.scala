package org.scalatra.swagger

import org.specs2.mutable.Specification
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.AllowableValues.{AllowableRangeValues, AllowableValuesList}
import org.scalatra.swagger.reflect.Reflector

object ModelSpec {

  case class WithDescription(@ApiProperty(value = "a description", allowableValues = "item1,item2")
                             id: String)
  case class WithAllowableValues(@ApiProperty(allowableValues = "item1,item2")
                             id: String)
  case class WithAllowableRangeValues(@ApiProperty(allowableValues = "range[1,10]")
                                 id: String)

  case class WithRequiredFalse(id: String, @ApiProperty(required = false) name: String)
  case class WithRequiredTrue(id: String, @ApiProperty(required = true) name: String)

  case class WithOption(id: String, name: Option[String])
  case class WithDefaultValue(id: String, name: String = "April")
  case class WithRequiredValue(id: String, name: String)

  def swaggerProperties[T](implicit mf: Manifest[T]) = swaggerProperty[T]("id")
  def swaggerProperty[T](name: String)(implicit mf: Manifest[T]) =
    Swagger.modelToSwagger(Reflector.scalaTypeOf[T]).get.properties(name)

}

class ModelSpec extends Specification {
  import ModelSpec._

  "Model to Swagger" should {

    "convert a populated description property of an ApiProperty annotation" in {
      swaggerProperties[WithDescription].description must_== "a description"
    }

    "convert a populated allowable values property of an ApiProperty annotation" in {
      swaggerProperties[WithAllowableValues].allowableValues must_== AllowableValuesList(List("item1", "item2"))
    }
    "convert a populated allowable values property of an ApiProperty annotation when it is a range" in {
      swaggerProperties[WithAllowableRangeValues].allowableValues must_== AllowableRangeValues(Range(1, 10))
    }
    "convert a required=false annotation of a model field" in {
      swaggerProperty[WithRequiredFalse]("name").required must beFalse
    }
    "convert a required=true annotation of a model field" in {
      swaggerProperty[WithRequiredTrue]("name").required must beTrue
    }
    "convert an option to a required false" in {
      swaggerProperty[WithOption]("name").required must beFalse
    }
    "convert an default value to a required true" in {
      swaggerProperty[WithDefaultValue]("name").required must beTrue
    }
    "convert an non-option to a required false" in {
      swaggerProperty[WithRequiredValue]("name").required must beTrue
    }

  }

}
