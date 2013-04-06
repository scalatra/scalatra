package org.scalatra.swagger

import org.specs2.mutable.Specification
import com.wordnik.swagger.annotations.ApiProperty
import scala.annotation.meta.field
import org.scalatra.swagger.AllowableValues.{AllowableRangeValues, AllowableValuesList}

object ModelSpec {

  case class WithDescription(@(ApiProperty @field)(value = "a description", allowableValues = "item1,item2")
                             id: String)
  case class WithAllowableValues(@(ApiProperty @field)(allowableValues = "item1,item2")
                             id: String)
  case class WithAllowableRangeValues(@(ApiProperty @field)(allowableValues = "range[1,10]")
                                 id: String)

  def swaggerProperties[T](implicit mf: Manifest[T]) = Swagger.modelToSwagger(mf.erasure).get.properties.get("id").get

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
  }

}
