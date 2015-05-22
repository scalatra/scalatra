package org.scalatra
package validation

import java.net.URI
import java.util.Locale._

import mojolly.inflector.InflectorImports._
import org.apache.commons.validator.routines.{ EmailValidator, UrlValidator }
import org.scalatra.commands.FieldValidation
import org.scalatra.util.RicherString._

import scala.util.control.Exception._
import scala.util.matching.Regex
import scalaz.Scalaz._
import scalaz._

object Validators {
  trait Validator[TValue] {
    def validate[TResult >: TValue <: TValue](subject: TResult): FieldValidation[TResult]
  }

  class PredicateValidator[TValue](fieldName: String, isValid: TValue ⇒ Boolean, messageFormat: String)
      extends Validator[TValue] {
    def validate[TResult >: TValue <: TValue](value: TResult): FieldValidation[TResult] = {
      if (isValid(value)) value.success
      else ValidationError(messageFormat.format(fieldName.underscore.humanize), FieldName(fieldName), ValidationFail).failure[TResult]
    }
  }

  def validate[TValue](fieldName: String, messageFormat: String = "%s is invalid.", validate: TValue => Boolean) =
    new PredicateValidator[TValue](fieldName, validate, messageFormat)

  /**
   * Must be a non-empty [String]. null, " ", and "" are not allowed.
   */
  def nonEmptyString(fieldName: String, messageFormat: String = "%s must be present."): Validator[String] =
    new PredicateValidator[String](fieldName, _.nonBlank, messageFormat)

  /**
   * Must be non-null.
   */
  def notNull(fieldName: String, messageFormat: String = "%s must be present."): Validator[AnyRef] =
    new PredicateValidator[AnyRef](fieldName, _ != null, messageFormat)

  /**
   * Must be a collection which isn't empty.
   */
  def nonEmptyCollection[TResult <: Traversable[_]](fieldName: String, messageFormat: String = "%s must not be empty."): Validator[TResult] =
    new PredicateValidator[TResult](fieldName, _.nonEmpty, messageFormat)

  /**
   * Must be a valid email as determined by org.apache.commons.validator.routines.EmailValidator
   */
  def validEmail(fieldName: String, messageFormat: String = "%s must be a valid email."): Validator[String] =
    new PredicateValidator[String](fieldName, EmailValidator.getInstance.isValid(_), messageFormat)

  /**
   * Must be a valid absolute URL, parseable by the Apache Commons URI class.
   */
  def validAbsoluteUrl(fieldName: String, allowLocalHost: Boolean, messageFormat: String = "%s must be a valid absolute url.", schemes: Seq[String] = Seq("http", "https")) =
    buildUrlValidator(fieldName, absolute = true, allowLocalHost = allowLocalHost, messageFormat = messageFormat, schemes)

  /**
   * Must be a valid URL, parseable by the Apache Commons URI class.
   */
  def validUrl(fieldName: String, allowLocalHost: Boolean, messageFormat: String = "%s must be a valid url.", schemes: Seq[String] = Seq("http", "https")) =
    buildUrlValidator(fieldName, absolute = false, allowLocalHost = allowLocalHost, messageFormat = messageFormat, schemes)

  /**
   * Must match the regex.
   */
  def validFormat(fieldName: String, regex: Regex, messageFormat: String = "%s is invalid."): Validator[String] =
    new PredicateValidator[String](fieldName, regex.findFirstIn(_).isDefined, messageFormat)

  /**
   * The confirmation fieldName must have a true value.
   */
  def validConfirmation(fieldName: String, confirmationFieldName: String, confirmationValue: => String, messageFormat: String = "%%s must match %s."): Validator[String] =
    new PredicateValidator[String](
      fieldName,
      _ == confirmationValue,
      messageFormat.format(confirmationFieldName.underscore.humanize.toLowerCase(ENGLISH)))

  /**
   * Must be greater than the min param.
   */
  def greaterThan[T <% Ordered[T]](fieldName: String, min: T, messageFormat: String = "%%s must be greater than %s."): Validator[T] =
    new PredicateValidator[T](fieldName, _ > min, messageFormat format min.toString)

  /**
   * Must be less than the max param.
   */
  def lessThan[T <% Ordered[T]](fieldName: String, max: T, messageFormat: String = "%%s must be less than %s."): Validator[T] =
    new PredicateValidator[T](fieldName, _ < max, messageFormat format max.toString)

  /**
   * Must be greater than or equal to the min param.
   */
  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, min: T, messageFormat: String = "%%s must be greater than or equal to %s."): Validator[T] =
    new PredicateValidator[T](fieldName, _ >= min, messageFormat format min)

  /**
   * Must be less than or equal to the max param.
   */
  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, max: T, messageFormat: String = "%%s must be less than or equal to %s."): Validator[T] =
    new PredicateValidator[T](fieldName, _ <= max, messageFormat.format(max))

  /**
   * Must have a minimum length of min.
   */
  def minLength(fieldName: String, min: Int, messageFormat: String = "%%s must be at least %s characters long."): Validator[String] =
    new PredicateValidator[String](
      fieldName, _.size >= min, messageFormat.format(min))

  /**
   * Must be included in the expected collection.
   */
  def oneOf[TResult](fieldName: String, messageFormat: String = "%%s must be one of %s.", expected: Seq[TResult]): Validator[TResult] =
    new PredicateValidator[TResult](
      fieldName, expected.contains, messageFormat format expected.mkString("[", ", ", "]"))

  /**
   * Checks if the value of the data is a value of the specified enum.
   */
  def enumValue(fieldName: String, enum: Enumeration, messageFormat: String = "%%s must be one of %s."): Validator[String] =
    oneOf(fieldName, messageFormat, enum.values.map(_.toString).toSeq)

  private def buildUrlValidator(fieldName: String, absolute: Boolean, allowLocalHost: Boolean, messageFormat: String = "%s must be a valid url.", schemes: Seq[String]): Validator[String] = {
    val validator = (url: String) ⇒ {
      (allCatch opt {
        val u = URI.create(url).normalize()
        !absolute || u.isAbsolute
      }).isDefined && (allowLocalHost || UrlValidator.getInstance().isValid(url))
    }
    new PredicateValidator[String](fieldName, validator, messageFormat)
  }

}
