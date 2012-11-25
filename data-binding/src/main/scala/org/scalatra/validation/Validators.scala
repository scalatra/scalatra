package org.scalatra
package validation

import org.apache.commons.validator.routines.EmailValidator
import scala.util.matching.Regex
import java.util.Locale._
import scala.util.control.Exception._
import java.net.URI
import org.apache.commons.validator.routines.UrlValidator
import scalaz._
import Scalaz._
import mojolly.inflector.InflectorImports._
import databinding.FieldValidation

object Validators {
  trait Validator[TValue] {
    def validate[TResult >: TValue <: TValue](subject: TResult): FieldValidation[TResult]
  }

  class PredicateValidator[TValue](fieldName: String, isValid: TValue ⇒ Boolean, messageFormat: String)
      extends Validator[TValue] {
    def validate[TResult >: TValue <: TValue](value: TResult): FieldValidation[TResult] = {
      if (isValid(value)) value.success
      else ValidationError(messageFormat.format(fieldName.humanize), FieldName(fieldName), ValidationFail).fail[TResult]
    }
  }

  def validate[TValue](fieldName: String, message: String = "%s is invalid.", validate: TValue => Boolean) =
    new PredicateValidator[TValue](fieldName: String, validate, message: String)

  /**
  * Must be a non-empty [String]. null, " ", and "" are not allowed.
  */
  def nonEmptyString(fieldName: String, message: String = "%s must be present."): Validator[String] =
    new PredicateValidator[String](fieldName, s => s != null && s.trim.nonEmpty, message)


  /**
   * Must be non-null.
   */
  def notNull(fieldName: String, message: String = "%s must be present."): Validator[AnyRef] =
    new PredicateValidator[AnyRef](fieldName, s => s != null, message)

  /**
   * Must be a collection which isn't empty.
   */
  def nonEmptyCollection[TResult <: Traversable[_]](fieldName: String, message: String = "%s must not be empty."): Validator[TResult] =
    new PredicateValidator[TResult](fieldName, _.nonEmpty, message)

  /**
   * Must be a valid email as determined by org.apache.commons.validator.routines.EmailValidator
   */
  def validEmail(fieldName: String, message: String = "%s must be a valid email."): Validator[String] =
    new PredicateValidator[String](fieldName, EmailValidator.getInstance.isValid(_), message)

  /**
   * Must be a valid absolute URL, parseable by the Apache Commons URI class.
   */
  def validAbsoluteUrl(fieldName: String, allowLocalHost: Boolean, schemes: String*) =
    buildUrlValidator(fieldName, true, allowLocalHost, schemes: _*)

  /**
   * Must be a valid URL, parseable by the Apache Commons URI class.
   */
  def validUrl(fieldName: String, allowLocalHost: Boolean, schemes: String*) =
    buildUrlValidator(fieldName, false, allowLocalHost, schemes: _*)

  /**
   * Must match the regex.
   */
  def validFormat(fieldName: String, regex: Regex, messageFormat: String = "%s is invalid."): Validator[String] =
    new PredicateValidator[String](fieldName, regex.findFirstIn(_).isDefined, messageFormat)

  /**
   * The confirmation fieldName must have a true value.
   */
  def validConfirmation(fieldName: String, confirmationFieldName: String, confirmationValue: => String): Validator[String] =
    new PredicateValidator[String](
      fieldName,
      _ == confirmationValue,
      "%s must match " + confirmationFieldName.underscore.humanize.toLowerCase(ENGLISH) + ".")

  /**
   * Must be greater than the min param.
   */
  def greaterThan[T <% Ordered[T]](fieldName: String, min: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ > min, "%s must be greater than " + min.toString)

  /**
   * Must be less than the max param.
   */
  def lessThan[T <% Ordered[T]](fieldName: String, max: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ < max, "%s must be less than " + max.toString)

  /**
   * Must be greater than or equal to the min param.
   */
  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, min: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ >= min, "%s must be greater than or equal to " + min.toString)

  /**
   * Must be less than or equal to the max param.
   */
  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, max: T): Validator[T] =
    new PredicateValidator[T](fieldName, _ <= max, "%s must be less than or equal to " + max.toString)

  /**
   * Must have a minimum length of min.
   */
  def minLength(fieldName: String, min: Int): Validator[String] =
    new PredicateValidator[String](
      fieldName, _.size >= min, "%s must be at least " + min.toString + " characters long.")

  /**
   * Must be included in the expected collection.
   */
  def oneOf[TResult](fieldName: String, expected: TResult*): Validator[TResult] =
    new PredicateValidator[TResult](
      fieldName, expected.contains, "%s must be one of " + expected.mkString("[", ", ", "]"))

  /**
   * Checks if the value of the data is a value of the specified enum.
   */ 
  def enumValue(fieldName: String, enum: Enumeration): Validator[String] =
    oneOf(fieldName, enum.values.map(_.toString).toSeq: _*)


  private def buildUrlValidator(fieldName: String, absolute: Boolean, allowLocalHost: Boolean, schemes: String*): Validator[String] = {
    val validator = (url: String) ⇒ {
      (allCatch opt {
        val u = URI.create(url).normalize()
        !absolute || u.isAbsolute
      }).isDefined && (allowLocalHost || UrlValidator.getInstance().isValid(url))
    }
    new PredicateValidator[String](fieldName, validator, "%s must be a valid url.")
  }

}