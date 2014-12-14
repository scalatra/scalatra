package org.scalatra
package validation

import org.specs2.mutable.Specification
import org.json4s._

class ValidationSerializerSpec extends Specification {

  implicit val formats: Formats =
    DefaultFormats + new ErrorCodeSerializer(org.scalatra.validation.NotFound) + new ValidationErrorSerializer()

  "A validation error serializer" should {
    "serialize a validation error" in {
      val err = ValidationError("the error message", FieldName("a_field"), org.scalatra.validation.NotFound)
      Extraction.decompose(err) must_== JObject(JField("message", JString("the error message")) :: "field" -> JString("a_field") :: "code" -> JString("NotFound") :: Nil)
    }
  }
}