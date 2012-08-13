package org.scalatra
package validation

import scalaz.Validations
import org.scalatra.databinding.Command
import org.scalatra.databinding.Binding
import scala.util.matching.Regex

trait ValidationSupport extends Validations {

  this: Command =>

  private var _valid: Option[Boolean] = None

  private var _errors: List[ValidatedBinding[_]] = Nil

  /**
   * Check whether this command is valid.
   */
  def valid: Option[Boolean] = _valid

  /**
   * Return a Map of all field command error keyed by field binding name (NOT the name of the variable in command
   * object).
   */
  def errors: Seq[ValidatedBinding[_]] = _errors

  def accept[T](value: T): FieldValidation[T] = success(value)

  def reject[T](message: String, args: Any*): FieldValidation[T] = failure(FieldError(message, args:_*))

  /**
   * Support class for 'validate' method provided by the implicit below.
   */
  sealed class BindingValidationSupport[T](command: Command, binding: Binding[T]) extends Validations {

    private def acceptAsDefault: Validator[T] = {
      case Some(x) => accept[T](x)
      case None => accept(null.asInstanceOf[T])
    }

    /**
     * Validate this binding with the given partial function.
     */
    def validate(v: Validator[T]): ValidatedBinding[T] = {
      val validator = v orElse acceptAsDefault
      val newBinding = new ValidatedBindingDecorator[T](validator, binding)
      command.bindings = command.bindings :+ newBinding
      newBinding
    }

    def withBinding(bf: Binding[T] => Binding[T]) = bf(binding)


    def nonEmptyString: Validator[String] = {
      case s => Validation.nonEmptyString(binding.name, s getOrElse "")
    }

    def nonEmptyCollection[TResult <: Seq[_]]: Validator[TResult] = {
      case s => Validation.nonEmptyCollection(binding.name, s getOrElse Nil.asInstanceOf[TResult])
    }

    def validEmail: Validator[String] = {
      case m => Validation.validEmail(binding.name, m getOrElse "")
    }

    def validAbsoluteUrl(allowLocalHost: Boolean, schemes: String*): Validator[String] = {
      case value => Validators.validAbsoluteUrl(binding.name, allowLocalHost, schemes:_*).validate(value getOrElse "")
    }

    def validUrl(allowLocalHost: Boolean, schemes: String*): Validator[String] = {
      case value => Validators.validUrl(binding.name, allowLocalHost, schemes:_*).validate(value getOrElse "")
    }

    def validFormat(regex: Regex, messageFormat: String = "%s is invalid."): Validator[String] = {
      case value => Validators.validFormat(binding.name, regex, messageFormat).validate(value getOrElse "")
    }

    def validConfirmation(confirmationFieldName: String, confirmationValue: String): Validator[String] = {
      case value => Validators.validConfirmation(binding.name, confirmationFieldName, confirmationValue).validate(value getOrElse "")
    }

    def greaterThan[T <% Ordered[T]](min: T): Validator[T] = {
      case value => Validators.greaterThan(binding.name, min).validate(value getOrElse null.asInstanceOf[T])
    }

    def lessThan[T <% Ordered[T]](max: T): Validator[T] = {
      case value => Validators.lessThan(binding.name, max).validate(value getOrElse  null.asInstanceOf[T])
    }

    def greaterThanOrEqualTo[T <% Ordered[T]](min: T): Validator[T] = {
      case value => Validators.greaterThanOrEqualTo(binding.name, min).validate(value getOrElse  null.asInstanceOf[T])
    }

    def lessThanOrEqualTo[T <% Ordered[T]](max: T): Validator[T] = {
      case value => Validators.lessThanOrEqualTo(binding.name, max).validate(value getOrElse  null.asInstanceOf[T])
    }

    def minLength(min: Int): Validator[String] = {
      case value => Validators.minLength(binding.name, min).validate(value getOrElse  "")
    }

    def oneOf[TResult](expected: TResult*): Validator[TResult] = {
      case value => Validators.oneOf(binding.name, expected:_*).validate(value getOrElse Nil.asInstanceOf[TResult])
    }

    def enumValue(enum: Enumeration): Validator[String] = oneOf(enum.values.map(_.toString).toSeq: _*)


  }

  /**
   * Implicit enhancement for [[org.scalatra.command.Binding]]
   */
  implicit def binding2Validated[T](binding: Binding[T]) = new BindingValidationSupport[T](this, binding)

  /**
   * Perform command as afterBinding task.
   */
  afterBinding {
    _errors = bindings.collect { case (b: ValidatedBinding[_]) if !b.valid => b }
    _valid = Some(_errors.isEmpty)
  }

}

