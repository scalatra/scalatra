package org.scalatra
package validation

import org.scalatra.databinding.Command
import scala.util.matching.Regex

trait CommandValidators { self: Command with ValidationSupport =>

  protected def nonEmptyString(fieldName: String): Validator[String] = {
    case s => Validation.nonEmptyString(fieldName, s getOrElse "")
  }

  protected def nonEmptyCollection[TResult <: Seq[_]](fieldName: String): Validator[TResult] = {
    case s => Validation.nonEmptyCollection(fieldName, s getOrElse Nil.asInstanceOf[TResult])
  }

  protected def validEmail(fieldName: String): Validator[String] = {
    case m => Validation.validEmail(fieldName, m getOrElse "")
  }

  protected def validAbsoluteUrl(fieldName: String, allowLocalHost: Boolean, schemes: String*): Validator[String] = {
    case value => Validators.validAbsoluteUrl(fieldName, allowLocalHost, schemes:_*).validate(value getOrElse "")
  }

  protected def validUrl(fieldName: String, allowLocalHost: Boolean, schemes: String*): Validator[String] = {
    case value => Validators.validUrl(fieldName, allowLocalHost, schemes:_*).validate(value getOrElse "")
  }

  protected def validFormat(fieldName: String, regex: Regex, messageFormat: String = "%s is invalid."): Validator[String] = {
    case value => Validators.validFormat(fieldName, regex, messageFormat).validate(value getOrElse "")
  }

  protected def validConfirmation(fieldName: String, confirmationFieldName: String, confirmationValue: String): Validator[String] = {
    case value => Validators.validConfirmation(fieldName, confirmationFieldName, confirmationValue).validate(value getOrElse "")
  }

  protected def greaterThan[T <% Ordered[T]](fieldName: String, min: T): Validator[T] = {
    case value => Validators.greaterThan(fieldName, min).validate(value getOrElse null.asInstanceOf[T])
  }

  protected def lessThan[T <% Ordered[T]](fieldName: String, max: T): Validator[T] = {
    case value => Validators.lessThan(fieldName, max).validate(value getOrElse  null.asInstanceOf[T])
  }

  protected def greaterThanOrEqualTo[T <% Ordered[T]](fieldName: String, min: T): Validator[T] = {
    case value => Validators.greaterThanOrEqualTo(fieldName, min).validate(value getOrElse  null.asInstanceOf[T])
  }

  protected def lessThanOrEqualTo[T <% Ordered[T]](fieldName: String, max: T): Validator[T] = {
    case value => Validators.lessThanOrEqualTo(fieldName, max).validate(value getOrElse  null.asInstanceOf[T])
  }

  protected def minLength(fieldName: String, min: Int): Validator[String] = {
    case value => Validators.minLength(fieldName, min).validate(value getOrElse  "")
  }

  protected def oneOf[TResult](fieldName: String, expected: TResult*): Validator[TResult] = {
    case value => Validators.oneOf(fieldName, expected:_*).validate(value getOrElse Nil.asInstanceOf[TResult])
  }

  protected def enumValue(fieldName: String, enum: Enumeration): Validator[String] =
    oneOf(fieldName, enum.values.map(_.toString).toSeq: _*)
}