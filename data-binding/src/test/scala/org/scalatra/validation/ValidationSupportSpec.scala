//package org.scalatra
//package validation
//
//import org.specs2.mutable.Specification
//import scalaz.Failure
//import net.liftweb.json.{DefaultFormats, Formats}
//import org.scalatra.databinding.WithBinding
//
//class WithValidation extends WithBinding with ValidationSupport {
//
//  val notRequiredCap = bind(cap) validate {
//    case Some(capval: Int) if capval <= 100 => reject("CAP value should be greater than 100", capval)
//  }
//
//  val legalAge = bind(age) validate {
//    case Some(yo: Int) if yo < 18 => reject("Your age must be at least of 18", yo)
//    case None => reject("Age field is required")
//  }
//}
//
//
//class ValidationSupportSpec extends Specification {
//  implicit val formats: Formats = DefaultFormats
//  "The 'ValidationSupport' trait" should {
//
//    "do normal binding within 'doBinding'" in {
//
//      val ageValidatedForm = new WithValidation
//      val params = Map("name" -> "John", "surname" -> "Doe", "age" -> "15")
//
//      ageValidatedForm.doBinding(params)
//
//      ageValidatedForm.a.converted must_== Some(params("name").toUpperCase)
//      ageValidatedForm.lower.converted must_== Some(params("surname").toLowerCase)
//      ageValidatedForm.age.converted must_== Some(15)
//
//    }
//
//    "validate only 'validatable bindings' within doBinding" in {
//
//      val ageValidatedForm = new WithValidation
//      val params = Map("name" -> "John", "surname" -> "Doe", "age" -> "15")
//
//      ageValidatedForm.valid must beNone
//
//      ageValidatedForm.doBinding(params)
//
//      ageValidatedForm.valid must beSome[Boolean]
//      ageValidatedForm.valid.get must beFalse
//
//      ageValidatedForm.errors aka "validation error list" must have(_.name == "age")
//
//      //ageValidatedForm.errors.get("age").get.asInstanceOf[Rejected[Int]] aka "the validation error" must_== (Rejected(Some("Your age must be at least of 18"), Some(15)))
//
//      ageValidatedForm.legalAge.validation aka "the validation result" must_== Failure(FieldError("Your age must be at least of 18", 15))
//      //ageValidatedForm.errors.filter(_.name == "age").head.validation aka "the validation result" must_== Failure(FieldError("Your age must be at least of 18", 15))
//    }
//
//
//    "evaluate non-exaustive validation as 'accepted'" in {
//      val formUnderTest = new WithValidation
//      val params = Map("name" -> "John", "surname" -> "Doe", "age" -> "20")
//
//      params must not haveKey ("cap")
//
//      formUnderTest.doBinding(params)
//      formUnderTest.valid must beSome[Boolean]
//      formUnderTest.valid.get must beTrue
//
//      formUnderTest.notRequiredCap.converted must beNone
//      formUnderTest.notRequiredCap.valid must beTrue
//    }
//
//  }
//}
//
//
