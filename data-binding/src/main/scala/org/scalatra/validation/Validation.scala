package org.scalatra
package validation

import scala.util.matching.Regex

object Validation {

  def nonEmptyString(fieldName: String, value: ⇒ String): FieldValidation[String] =
    Validators.nonEmptyString(fieldName).validate(value)

  def nonEmptyCollection[TResult <: Seq[_]](fieldName: String, value: ⇒ TResult): FieldValidation[TResult] =
    Validators.nonEmptyCollection(fieldName).validate(value)

  def validEmail(fieldName: String, value: ⇒ String): FieldValidation[String] =
    Validators.validEmail(fieldName).validate(value)

  def validAbsoluteUrl(fieldName: String, value: ⇒ String, allowLocalHost: Boolean, schemes: String*) =
    Validators.validAbsoluteUrl(fieldName, allowLocalHost, schemes:_*).validate(value)

  def validUrl(fieldName: String, value: ⇒ String, allowLocalHost: Boolean, schemes: String*) =
    Validators.validUrl(fieldName, allowLocalHost, schemes:_*).validate(value)

  def validFormat(fieldName: String, value: ⇒ String, regex: Regex, messageFormat: String = "%s is invalid."): FieldValidation[String] =
    Validators.validFormat(fieldName, regex, messageFormat).validate(value)

  def validConfirmation(fieldName: String, value: ⇒ String, confirmationFieldName: String, confirmationValue: String): FieldValidation[String] =
    Validators.validConfirmation(fieldName, confirmationFieldName, confirmationValue).validate(value)

  def greaterThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T): FieldValidation[T] =
    Validators.greaterThan(fieldName, min).validate(value)

  def lessThan[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T): FieldValidation[T] =
    Validators.lessThan(fieldName, max).validate(value)

  def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, min: T): FieldValidation[T] =
    Validators.greaterThanOrEqualTo(fieldName, min).validate(value)

  def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, value: ⇒ T, max: T): FieldValidation[T] =
    Validators.lessThanOrEqualTo(fieldName, max).validate(value)

  def minLength(fieldName: String, value: ⇒ String, min: Int): FieldValidation[String] =
    Validators.minLength(fieldName, min).validate(value)

  def oneOf[TResult](fieldName: String, value: ⇒ TResult, expected: TResult*): FieldValidation[TResult] =
    Validators.oneOf(fieldName, expected:_*).validate(value)

  def enumValue(fieldName: String, value: ⇒ String, enum: Enumeration): FieldValidation[String] =
    oneOf(fieldName, value, enum.values.map(_.toString).toSeq: _*)


}