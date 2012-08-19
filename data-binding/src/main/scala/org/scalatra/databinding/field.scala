package org.scalatra
package databinding

import validation._
import util.conversion._
import scalaz._
import Scalaz._


object Field {
  def apply[T:Zero](name: String): Field[T] = new BasicField[T](name, transformations = identity)
}
trait Field[T] {

  def name: String
  def value: FieldValidation[T]
  def validator: Option[Validator[T]]

  def isValid = value.isSuccess
  def isInvalid = value.isFailure


  def required: Field[T]
  def optional: Field[T]

  override def toString() = "Field(name: %s)".format(name)

  def validateWith(validators: BindingValidator[T]*): Field[T]

  def apply[S](original: Option[S])(implicit zero: Zero[S], convert: TypeConverter[S, T]): ValidatableField[S, T]

  override def hashCode() = 41 + 41 * name.hashCode()

  def transform(endo: T => T): Field[T]

  override def equals(obj: Any) = obj match {
    case b : Field[_] => b.name == this.name
    case _ => false
  }

}

class BasicField[T:Zero](val name: String, val validator: Option[Validator[T]] = None, transformations: T => T = identity _, private[databinding] var isRequired: Boolean = false) extends Field[T] {

  val value: FieldValidation[T] = mzero[T].success

  def validateWith(bindingValidators: BindingValidator[T]*): Field[T] = {
    val nwValidators: Option[Validator[T]] =
      if(bindingValidators.nonEmpty) Some(bindingValidators.map(_ apply name).reduce(_ andThen _)) else None

    copy(validator = validator.flatMap(v => nwValidators.map(v andThen _)) orElse nwValidators)
  }

  def copy(name: String = name, validator: Option[Validator[T]] = validator, transformations: T => T = transformations, isRequired: Boolean = isRequired): Field[T] =
    new BasicField(name, validator, transformations, isRequired)

  private def parseFailure[B](v: Option[B]): FieldValidation[B] =
      v map (_.success) getOrElse ValidationError(name +" has invalid input", FieldName(name)).fail

  def apply[S](original: Option[S])(implicit zero: Zero[S], convert: TypeConverter[S, T]): ValidatableField[S, T] = {
    val o = ~original
    val conv = parseFailure(convert(o))

    if (!isRequired && o == zero.zero) {
      BoundField(o, (~convert(o)).success, this)
    } else {
      val realValidator = if (isRequired) {
        conv flatMap (v => if (v != zero.zero) v.success else ValidationError("%s is required.".format(name), FieldName(name)).fail)
      } else conv
      val validated = validator map (_ apply realValidator) getOrElse realValidator
      BoundField(o, validated map transformations, this)
    }
  }

  def transform(endo: T => T): Field[T] = copy(transformations = transformations andThen endo)

  def required = copy(isRequired = true)

  def optional = copy(isRequired = false)
}


trait ValidatableField[S, T] extends Field[T] {
  def field: Field[T]
  def original: S
  def transform(endo: T => T): ValidatableField[S, T]
  def apply[V](original: Option[V])(implicit zero: Zero[V], convert: TypeConverter[V, T]): ValidatableField[V, T] =
    this.asInstanceOf[ValidatableField[V, T]]

  override def toString() = "Field(name: %s, original: %s, value: %s)".format(name, original, value)
}

object BoundField {
  def apply[S, T](original: S, value: FieldValidation[T], binding: Field[T]): ValidatableField[S, T] =
    new BoundField(original, value, binding)
}
class BoundField[S, T](val original: S, val value: FieldValidation[T], val field: Field[T]) extends ValidatableField[S, T] {
  def name: String = field.name

  override def hashCode(): Int = field.hashCode()
  override def equals(other: Any) = field.equals(other)
  override def toString() = "BoundField(name: %s, original: %s, converted: %s)".format(name, original, value)

  def validateWith(bindingValidators: BindingValidator[T]*): Field[T] = {
    copy(field = field.validateWith(bindingValidators:_*))
  }

  def copy(original: S = original, value: FieldValidation[T] = value, field: Field[T] = field): ValidatableField[S, T] =
    new BoundField(original, value, field)

  def validator: Option[Validator[T]] = field.validator

  def transform(endo: T => T): ValidatableField[S, T] = copy(value = value map endo)

  def required = copy(field = field.required)

  def optional = copy(field = field.optional)


}

import scala.util.matching.Regex


trait BindingValidatorImplicits {

  import BindingValidators._
  implicit def validatableStringBinding(b: Field[String]) = new ValidatableStringBinding(b)
  implicit def validatableSeqBinding[T <: Seq[_]](b: Field[T]) = new ValidatableSeq(b)
  implicit def validatableGenericBinding[T](b: Field[T]) = new ValidatableGenericBinding(b)
  implicit def validatableOrderedBinding[T <% Ordered[T]](b: Field[T]) = new ValidatableOrdered(b)

}

object BindingValidators {


  class ValidatableSeq[T <: Seq[_]](b: Field[T]) {
    def notEmpty: Field[T] =
      b.validateWith(BindingValidators.nonEmptyCollection)
  }


  class ValidatableOrdered[T <% Ordered[T]](b: Field[T]) {
    def greaterThan(min: T): Field[T] =
      b.validateWith(BindingValidators.greaterThan(min))

    def lessThan(max: T): Field[T] =
      b.validateWith(BindingValidators.lessThan(max))

    def greaterThanOrEqualTo(min: T): Field[T] =
      b.validateWith(BindingValidators.greaterThanOrEqualTo(min))

    def lessThanOrEqualTo(max: T): Field[T] =
      b.validateWith(BindingValidators.lessThanOrEqualTo(max))

  }

  class ValidatableGenericBinding[T](b: Field[T]) {
    def oneOf(expected: T*): Field[T] =
      b.validateWith(BindingValidators.oneOf(expected:_*))

    def validate(validate: T => Boolean): Field[T] = b.validateWith(BindingValidators.validate(validate))
  }

  class ValidatableStringBinding(b: Field[String]) {
    def notBlank: Field[String] = b.validateWith(BindingValidators.nonEmptyString)

    def validEmail: Field[String] = b.validateWith(BindingValidators.validEmail)

    def validAbsoluteUrl(allowLocalHost: Boolean, schemes: String*): Field[String] =
      b.validateWith(BindingValidators.validAbsoluteUrl(allowLocalHost, schemes:_*))

    def validUrl(allowLocalHost: Boolean, schemes: String*): Field[String] =
      b.validateWith(BindingValidators.validUrl(allowLocalHost, schemes:_*))

    def validForFormat(regex: Regex, messageFormat: String = "%s is invalid."): Field[String] =
      b.validateWith(BindingValidators.validFormat(regex, messageFormat))

    def validForConfirmation(confirmationFieldName: String, confirmationValue: String): Field[String] =
      b.validateWith(BindingValidators.validConfirmation(confirmationFieldName, confirmationValue))


    def minLength(min: Int): Field[String] =
      b.validateWith(BindingValidators.minLength(min))


    def enumValue(enum: Enumeration): Field[String] =
      b.validateWith(BindingValidators.enumValue(enum))
  }

  import org.scalatra.validation.Validation


  def validate[TValue](validate: TValue => Boolean): BindingValidator[TValue] = (s: String) => {
    _ flatMap (Validators.validate(s, validate).validate(_))
  }

  def nonEmptyString: BindingValidator[String] = (s: String) => {
    _ flatMap (Validation.nonEmptyString(s, _))
  }

  def notNull: BindingValidator[AnyRef] = (s: String) => {
    _ flatMap (Validation.notNull(s, _))
  }

  def nonEmptyCollection[TResult <: Seq[_]]: BindingValidator[TResult] = (s: String) =>{
    _ flatMap (Validation.nonEmptyCollection(s, _))
  }

  def validEmail: BindingValidator[String] = (s: String) =>{
    _ flatMap (Validation.validEmail(s, _))
  }

  def validAbsoluteUrl(allowLocalHost: Boolean, schemes: String*): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.validAbsoluteUrl(s, allowLocalHost, schemes:_*).validate(_))
  }

  def validUrl(allowLocalHost: Boolean, schemes: String*): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.validUrl(s, allowLocalHost, schemes:_*).validate(_))
  }

  def validFormat(regex: Regex, messageFormat: String = "%sis invalid."): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.validFormat(s, regex, messageFormat).validate(_))
  }

  def validConfirmation(confirmationFieldName: String, confirmationValue: String): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.validConfirmation(s, confirmationFieldName, confirmationValue).validate(_))
  }

  def greaterThan[T <% Ordered[T]](min: T): BindingValidator[T] = (s: String) =>{
    _ flatMap (Validators.greaterThan(s, min).validate(_))
  }

  def lessThan[T <% Ordered[T]](max: T): BindingValidator[T] = (s: String) =>{
    _ flatMap (Validators.lessThan(s, max).validate(_))
  }

  def greaterThanOrEqualTo[T <% Ordered[T]](min: T): BindingValidator[T] = (s: String) =>{
    _ flatMap (Validators.greaterThanOrEqualTo(s, min).validate(_))
  }

  def lessThanOrEqualTo[T <% Ordered[T]](max: T): BindingValidator[T] = (s: String) =>{
    _ flatMap (Validators.lessThanOrEqualTo(s, max).validate(_))
  }

  def minLength(min: Int): BindingValidator[String] = (s: String) =>{
    _ flatMap (Validators.minLength(s, min).validate(_))
  }

  def oneOf[TResult](expected: TResult*): BindingValidator[TResult] = (s: String) => {
    _ flatMap (Validators.oneOf(s, expected:_*).validate(_))
  }

  def enumValue(enum: Enumeration): BindingValidator[String] = oneOf(enum.values.map(_.toString).toSeq:_*)
}