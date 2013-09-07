package org.scalatra.swagger

import org.specs2.mutable.Specification
import org.scalatra.swagger.annotations._
import org.scalatra.swagger.AllowableValues.{AllowableRangeValues, AllowableValuesList}
import org.scalatra.swagger.reflect.Reflector
import org.json4s.{DefaultWriters, DefaultReaders, DefaultJsonFormats}

object ModelSpec {

  case class WithDescription(@ApiModelProperty(description = "a description", allowableValues = "item1,item2")
                             id: String)
  case class WithAllowableValues(@ApiModelProperty(allowableValues = "item1,item2")
                             id: String)
  case class WithAllowableRangeValues(@ApiModelProperty(allowableValues = "range[1,10]")
                                 id: String)

  case class WithRequiredFalse(id: String, @ApiModelProperty(required = false) name: String)
  case class WithRequiredTrue(id: String, @ApiModelProperty(required = true) name: String)

  case class WithOption(id: String, name: Option[String])
  case class WithDefaultValue(id: String, name: String = "April")
  case class WithRequiredValue(id: String, name: String)
  case class WithOptionList(id: String, flags: Option[List[String]])

  case class PositionNotSpecified(b2Field: Int, aField: Int, bField: Int)
  case class PositionSpecified(@ApiModelProperty(position = 0) b2Field: Int, @ApiModelProperty(position = 1) aField: Option[Int], @ApiModelProperty(position = 2) bField: String)
  case class PositionPartiallySpecified(b2Field: Int, aField: String, @ApiModelProperty(position = 0) bField: String)


  def swaggerProperties[T](implicit mf: Manifest[T]) = swaggerProperty[T]("id")
  def swaggerProperty[T](name: String)(implicit mf: Manifest[T]) =
    Swagger.modelToSwagger(Reflector.scalaTypeOf[T]).get.properties.find(_._1 == name).get._2

}

class ModelSpec extends Specification {
  import ModelSpec._



  "Model to Swagger" should {

    "convert a populated description property of an ApiProperty annotation" in {
      swaggerProperties[WithDescription].description must beSome("a description")
    }

    "convert a populated allowable values property of an ApiProperty annotation" in {
      swaggerProperties[WithAllowableValues].allowableValues must_== AllowableValuesList(List("item1", "item2"))
    }
    "convert a populated allowable values property of an ApiProperty annotation when it is a range" in {
      swaggerProperties[WithAllowableRangeValues].allowableValues must_== AllowableRangeValues(Range.inclusive(1, 10))
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
    "convert an option list to a required false list of things" in {
      swaggerProperty[WithOptionList]("flags").required must beFalse
      swaggerProperty[WithOptionList]("flags").`type` must_== DataType[List[String]]
    }

    "assign position to unspecified fields by alphabetical order" in {
      swaggerProperty[PositionNotSpecified]("b2Field").position must_== 1
      swaggerProperty[PositionNotSpecified]("aField").position must_== 0
      swaggerProperty[PositionNotSpecified]("bField").position must_== 2
    }

    "respect fully specified position annotations" in {
      swaggerProperty[PositionSpecified]("b2Field").position must_== 0
      swaggerProperty[PositionSpecified]("aField").position must_== 1
      swaggerProperty[PositionSpecified]("bField").position must_== 2
    }

    "respect position annotations and alphabetize the rest" in {
      swaggerProperty[PositionPartiallySpecified]("b2Field").position must_== 2
      swaggerProperty[PositionPartiallySpecified]("aField").position must_== 1
      swaggerProperty[PositionPartiallySpecified]("bField").position must_== 0
    }

  }

}
