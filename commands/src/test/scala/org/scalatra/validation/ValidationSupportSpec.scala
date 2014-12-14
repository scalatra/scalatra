package org.scalatra
package validation

import org.specs2.mutable.Specification
import org.json4s.{ DefaultFormats, Formats }
import commands._
import scalaz._
import Scalaz._
import org.scalatra.commands.BindingSyntax._

class WithValidation extends WithBindingFromParams {
  val notRequiredCap: Field[Int] = asInt("cap").optional(0).greaterThan(100)

  val legalAge: Field[Int] = asInt("age").greaterThanOrEqualTo(18)
}

class ValidationSupportSpec extends Specification {
  implicit val formats: Formats = DefaultFormats
  import org.scalatra.util.ParamsValueReaderProperties._

  "The 'ValidationSupport' trait" should {

    "do normal binding within 'bindTo'" in {

      val ageValidatedForm = new WithValidation
      val params = Map("name" -> "John", "surname" -> "Doe", "age" -> "18")

      ageValidatedForm.bindTo(params)

      ageValidatedForm.a.validation must_== params("name").toUpperCase.success
      ageValidatedForm.lower.validation must_== params("surname").toLowerCase.success
      ageValidatedForm.age.validation must_== 18.success

    }

    "validate only 'validatable bindings' within bindTo" in {

      val ageValidatedForm = new WithValidation
      val params = Map("name" -> "John", "surname" -> "Doe", "age" -> "15")

      ageValidatedForm.isValid must beTrue

      ageValidatedForm.bindTo(params)

      ageValidatedForm.isValid must beFalse

      ageValidatedForm.errors must contain { x: Binding => x.name == "age" }

      ageValidatedForm.legalAge.validation aka "the validation result" must_== Failure(ValidationError("Age must be greater than or equal to 18", FieldName("age"), ValidationFail))
    }

    "evaluate non-exhaustive validation as 'accepted'" in {
      val formUnderTest = new WithValidation
      val params = Map("name" -> "John", "surname" -> "Doe", "age" -> "20")

      params must not haveKey ("cap")

      formUnderTest.bindTo(params)
      formUnderTest.isValid must beTrue

      formUnderTest.notRequiredCap.validation must_== 0.success
      formUnderTest.notRequiredCap.isValid must beTrue
    }

  }
}

