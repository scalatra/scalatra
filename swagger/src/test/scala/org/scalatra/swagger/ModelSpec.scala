package org.scalatra.swagger

import org.scalatra.swagger.AllowableValues.{
  AllowableRangeValues,
  AllowableValuesList
}
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.reflect.Reflector
import org.specs2.mutable.Specification

object ModelSpec {

  case class WithDescription(
      @ApiModelProperty(
        description = "a description",
        allowableValues = "item1,item2"
      ) id: String
  )
  case class WithAllowableValues(
      @ApiModelProperty(allowableValues = "item1,item2") id: String
  )
  case class WithAllowableRangeValues(
      @ApiModelProperty(allowableValues = "range[1,10]") id: String
  )
  case class WithDefaultValueFromAnnotation(
      @ApiModelProperty(defaultValue = "default") id: String
  )

  case class WithRequiredFalse(
      id: String,
      @ApiModelProperty(required = false) name: String
  )
  case class WithRequiredTrue(
      id: String,
      @ApiModelProperty(required = true) name: String
  )

  case class WithHiddenFalse(
      id: String,
      @ApiModelProperty(hidden = false) name: String
  )
  case class WithHiddenTrue(
      id: String,
      @ApiModelProperty(hidden = true) name: String
  )

  case class WithOption(id: String, name: Option[String])
  case class WithDefaultValue(id: String, name: String = "April")
  case class WithRequiredValue(id: String, name: String)
  case class WithOptionList(id: String, flags: Option[List[String]])

  case class WithOrder(
      id: String,
      name: String,
      title: Option[String],
      email: String,
      company: Option[String]
  )
  case class WithTypeParameter[T](id: String, results: List[T])

  def swaggerProperties[T](implicit mf: Manifest[T]) = swaggerProperty[T]("id")
  def swaggerProperty[T](name: String)(implicit mf: Manifest[T]) =
    allSwaggerProperties[T].find(_._1 == name).get._2
  def allSwaggerProperties[T](implicit mf: Manifest[T]) =
    Swagger.modelToSwagger(Reflector.scalaTypeOf[T]).get.properties

}

class ModelSpec extends Specification {
  import org.scalatra.swagger.ModelSpec._

  "Model to Swagger" should {

    "convert a populated description property of an ApiProperty annotation" in {
      swaggerProperties[WithDescription].description must beSome(
        "a description"
      )
    }

    "convert a populated allowable values property of an ApiProperty annotation" in {
      swaggerProperties[
        WithAllowableValues
      ].allowableValues must_== AllowableValuesList(List("item1", "item2"))
    }
    "convert a populated allowable values property of an ApiProperty annotation when it is a range" in {
      swaggerProperties[
        WithAllowableRangeValues
      ].allowableValues must_== AllowableRangeValues(Range.inclusive(1, 10))
    }
    "convert a defaultValue=\"default\" to a model field" in {
      swaggerProperties[WithDefaultValueFromAnnotation].default must beSome(
        "default"
      )
    }
    "convert a required=false annotation of a model field" in {
      swaggerProperty[WithRequiredFalse]("name").required must beFalse
    }
    "convert a required=true annotation of a model field" in {
      swaggerProperty[WithRequiredTrue]("name").required must beTrue
    }

    "convert a hidden=false annotation of a model field" in {
      swaggerProperty[WithHiddenFalse]("name").hidden must beFalse
    }

    "convert a hidden=true annotation of a model field" in {
      swaggerProperty[WithHiddenTrue]("name").hidden must beTrue
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
    "convert an option list to a required false list of things" in {
      swaggerProperty[WithOptionList]("flags").required must beFalse
      swaggerProperty[WithOptionList]("flags").`type` must_== DataType[List[
        String
      ]]
    }
    "convert a case class with a type parameter to a model with the enclosed parameter" in {
      swaggerProperty[WithTypeParameter[String]](
        "results"
      ).`type` must_== DataType[List[String]]
    }
    "preserves order of properties" in {
      allSwaggerProperties[WithOrder]
        .map(_._1) must_== List("id", "name", "title", "email", "company")
    }

  }

}
