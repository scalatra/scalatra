package org.scalatra
package validation

import scalaz.Validations
import databinding.{BoundBinding, Command, ValidatedBinding}

trait ValidationSupport extends Validations {

  this: Command =>

  private var _valid: Option[Boolean] = None

  private var _errors: Seq[ValidatedBinding[_, _]] = Nil

  /**
   * Check whether this command is valid.
   */
  def valid: Option[Boolean] = _valid

  /**
   * Return a Map of all field command error keyed by field binding name (NOT the name of the variable in command
   * object).
   */
  def errors: Seq[ValidatedBinding[_, _]] = _errors

  def accept[T](value: T): FieldValidation[T] = success(value)

  def reject[T](message: String, args: Any*): FieldValidation[T] = failure(FieldError(message, args:_*))

  /**
   * Support class for 'validate' method provided by the implicit below.
   */
  sealed class BindingValidationSupport[S: Manifest, T: Manifest](command: Command, binding: BoundBinding[S, T]) extends Validations {

    private def acceptAsDefault: Validator[T] = {
      case Some(x) => accept[T](x)
      case None => accept(null.asInstanceOf[T])
    }

//    /**
//     * Validate this binding with the given partial function.
//     */
//    def validate(v: Validator[T]): ValidatedBinding[S, T] = {
//      val validator = v orElse acceptAsDefault
//      val newBinding = new ValidatedBindingDecorator(validator, binding)
//      command.bindings = command.bindings :+ newBinding
//      newBinding
//    }
  }

  /**
   * Implicit enhancement for [[org.scalatra.command.Binding]]
   */
  implicit def binding2Validated[S: Manifest, T:Manifest](binding: BoundBinding[S, T]) =
    new BindingValidationSupport(this, binding)

  /**
   * Perform command as afterBinding task.
   */
  afterBinding {
    _errors = bindings.collect { case (b: ValidatedBinding[_, _]) if !b.valid => b }
    _valid = Some(_errors.isEmpty)
  }

}

