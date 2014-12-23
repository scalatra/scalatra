package org.scalatra
package validation

import org.scalatra.commands.FieldValidation

import scala.util.matching.Regex

object Validation {

  def nonEmptyString(fieldName: String, value: ⇒ String, messageFormat: String = "%s is required."): FieldValidation[String] =
    Validators.nonEmptyString(fieldName, messageFormat).validate(value)

  def notNull(fieldName: String, value: ⇒ AnyRef, messageFormat: String = "%s is required."): FieldValidation[AnyRef] =
    Validators.notNull(fieldName, messageFormat).validate(value)

  def nonEmptyCollection[TResult <: Traversable[_]](fieldName: String, value: ⇒ TResult, messageFormat: String = "%s must not be empty."): FieldValidation[TResult] =
    Validators.nonEmptyCollection(fieldName, messageFormat).validate(value)

  def validEmail(fieldName: String, value: ⇒ String, messageFormat: String = "%s must be a valid email."): FieldValidation[String] =
    Validators.validEmail(fieldName, messageFormat).validate(value)

  def validAbsoluteUrl(fieldName: String, value: ⇒ String, allowLocalHost: Boolean, messageFormat: String = "%s must be a valid absolute url.", schemes: Seq[String] = Seq("http", "https")) =
    Validators.validAbsoluteUrl(fieldName, allowLocalHost, messageFormat, schemes).validate(value)

  def validUrl(fieldName: String, value: ⇒ String, allowLocalHost: Boolean, messageFormat: String = "%s must be a valid url.", schemes: Seq[String] = Seq("http", "https")) =
    Validators.validUrl(fieldName, allowLocalHost, messageFormat, schemes).validate(value)

  def validFormat(fieldName: String, value: ⇒ String, regex: Regex, messageFormat: String = "%s is invalid."): FieldValidation[String] =
    Validators.validFormat(fieldName, regex, messageFormat).validate(value)

  def validConfirmation(fieldName: String, value: ⇒ String, confirmationFieldName: String, confirmationValue: => String, messageFormat: String = "%%s must match %s."): FieldValidation[String] =
    Validators.validConfirmation(fieldName, confirmationFieldName, confirmationValue, messageFormat).validate(value)

  def greaterThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T, messageFormat: String = "%%s must be greater than %s."): FieldValidation[T] =
    Validators.greaterThan(fieldName, min, messageFormat).validate(value)

  def lessThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T, messageFormat: String = "%%s must be less than %s."): FieldValidation[T] =
    Validators.lessThan(fieldName, max, messageFormat).validate(value)

  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T, messageFormat: String = "%%s must be greater than or equal to %s."): FieldValidation[T] =
    Validators.greaterThanOrEqualTo(fieldName, min, messageFormat).validate(value)

  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T, messageFormat: String = "%%s must be less than or equal to %s."): FieldValidation[T] =
    Validators.lessThanOrEqualTo(fieldName, max, messageFormat).validate(value)

  def minLength(fieldName: String, value: ⇒ String, min: Int, messageFormat: String = "%%s must be at least %s characters long."): FieldValidation[String] =
    Validators.minLength(fieldName, min, messageFormat).validate(value)

  def oneOf[TResult](fieldName: String, value: ⇒ TResult, messageFormat: String = "%%s must be one of %s.", expected: Seq[TResult]): FieldValidation[TResult] =
    Validators.oneOf(fieldName, messageFormat, expected).validate(value)

  def enumValue(fieldName: String, value: ⇒ String, enum: Enumeration, messageFormat: String = "%%s must be one of %s."): FieldValidation[String] =
    oneOf(fieldName, value, messageFormat, enum.values.map(_.toString).toSeq)

}