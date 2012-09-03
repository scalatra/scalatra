package org.scalatra
package databinding

import validation._
import util.conversion._
import scalaz._
import Scalaz._
import mojolly.inflector.InflectorImports._


object FieldDescriptor {
  def apply[T:Zero](name: String): FieldDescriptor[T] = new BasicFieldDescriptor[T](name, transformations = identity)
}
trait FieldDescriptor[T] {

  def name: String
  def value: FieldValidation[T]
  def validator: Option[Validator[T]]

  def isValid = value.isSuccess
  def isInvalid = value.isFailure

  private[databinding] def isRequired: Boolean
  def required: FieldDescriptor[T]
  def optional: FieldDescriptor[T]

  override def toString() = "FieldDescriptor(name: %s)".format(name)

  def validateWith(validators: BindingValidator[T]*): FieldDescriptor[T]

  def apply[S](original: Either[String, Option[S]])(implicit zero: Zero[S], ms: Manifest[S], convert: TypeConverter[S, T]): DataboundFieldDescriptor[S, T]

  override def hashCode() = 41 + 41 * name.hashCode()

  def transform(endo: T => T): FieldDescriptor[T]

  private[databinding] def transformations: T => T

  override def equals(obj: Any) = obj match {
    case b : FieldDescriptor[_] => b.name == this.name
    case _ => false
  }

}

class BasicFieldDescriptor[T:Zero](val name: String, val validator: Option[Validator[T]] = None, private[databinding] val transformations: T => T = identity _, private[databinding] var isRequired: Boolean = false) extends FieldDescriptor[T] {

  val value: FieldValidation[T] = mzero[T].success

  def validateWith(bindingValidators: BindingValidator[T]*): FieldDescriptor[T] = {
    val nwValidators: Option[Validator[T]] =
      if(bindingValidators.nonEmpty) Some(bindingValidators.map(_ apply name).reduce(_ andThen _)) else None

    copy(validator = validator.flatMap(v => nwValidators.map(v andThen _)) orElse nwValidators)
  }

  def copy(name: String = name, validator: Option[Validator[T]] = validator, transformations: T => T = transformations, isRequired: Boolean = isRequired): FieldDescriptor[T] =
    new BasicFieldDescriptor(name, validator, transformations, isRequired)

  def apply[S](original: Either[String, Option[S]])(implicit zero: Zero[S], ms: Manifest[S], convert: TypeConverter[S, T]): DataboundFieldDescriptor[S, T] = {
    val conv = original.fold(e => ValidationError(e).fail, o => (~convert(~o)).success)
    val o = original.fold(_ => zero.zero, og => ~og)
    BoundFieldDescriptor(o, conv, this)
  }

  def transform(endo: T => T): FieldDescriptor[T] = copy(transformations = transformations andThen endo)

  def required = copy(isRequired = true)

  def optional = copy(isRequired = false)
}


trait DataboundFieldDescriptor[S, T] extends FieldDescriptor[T] {
  def field: FieldDescriptor[T]
  def original: S
  def transform(endo: T => T): DataboundFieldDescriptor[S, T]
  def apply[V](original: Either[String, Option[V]])(implicit zero: Zero[V], mv: Manifest[V], convert: TypeConverter[V, T]): DataboundFieldDescriptor[V, T] =
    this.asInstanceOf[DataboundFieldDescriptor[V, T]]

  override def toString() = "FieldDescriptor(name: %s, original: %s, value: %s)".format(name, original, value)
  def validate: ValidatedFieldDescriptor[S, T]
  def validateWith(bindingValidators: BindingValidator[T]*): DataboundFieldDescriptor[S, T]
  def required: DataboundFieldDescriptor[S, T]
  def optional: DataboundFieldDescriptor[S, T]
  def isRequired = field.isRequired
}

trait ValidatedFieldDescriptor[S, T] extends DataboundFieldDescriptor[S, T] {
  def validate: ValidatedFieldDescriptor[S, T] = this
}

object BoundFieldDescriptor {
  def apply[S:Zero, T:Zero](original: S, value: FieldValidation[T], binding: FieldDescriptor[T]): DataboundFieldDescriptor[S, T] =
    new BoundFieldDescriptor(original, value, binding, binding.validator)
}


class BoundFieldDescriptor[S:Zero, T:Zero](val original: S, val value: FieldValidation[T], val field: FieldDescriptor[T], val validator: Option[Validator[T]]) extends DataboundFieldDescriptor[S, T] {
  def name: String = field.name

  override def hashCode(): Int = field.hashCode()
  override def equals(other: Any) = field.equals(other)
  override def toString() = "BoundFieldDescriptor(name: %s, original: %s, converted: %s)".format(name, original, value)

  def validateWith(bindingValidators: BindingValidator[T]*): DataboundFieldDescriptor[S, T] = {
    val nwFld = field.validateWith(bindingValidators:_*)
    copy(field = nwFld, validator = nwFld.validator)
  }

  def copy(original: S = original, value: FieldValidation[T] = value, field: FieldDescriptor[T] = field, validator: Option[Validator[T]] = validator): DataboundFieldDescriptor[S, T] =
    new BoundFieldDescriptor(original, value, field, validator)

  def transform(endo: T => T): DataboundFieldDescriptor[S, T] = copy(value = value map endo)

  def required = copy(field = field.required)

  def optional = copy(field = field.optional)

  def validate: ValidatedFieldDescriptor[S, T] = {
    val defaultValidator: Validator[T] = validator getOrElse identity
    if (!isRequired && original == mzero[S]) {
      new ValidatedBoundFieldDescriptor(value map transformations, this)
    } else {
      val doValidation: Validator[T] = if (isRequired) {
        (x: FieldValidation[T]) => x flatMap { v =>
          if (v != mzero[T]) v.success else ValidationError("%s is required." format(name.humanize), FieldName(name), ValidationFail).fail
        }
      } else identity
      new ValidatedBoundFieldDescriptor((doValidation andThen defaultValidator)(value) map transformations, this)
    }
  }

  private[databinding] def transformations: (T) => T = field.transformations
}

class ValidatedBoundFieldDescriptor[S, T](val value: FieldValidation[T], val field: DataboundFieldDescriptor[S, T]) extends ValidatedFieldDescriptor[S, T] {
  def name: String = field.name

  override def hashCode(): Int = field.hashCode()
  override def equals(other: Any) = field.equals(other)
  override def toString() = "BoundFieldDescriptor(name: %s, original: %s, converted: %s)".format(name, original, value)

  def validateWith(bindingValidators: BindingValidator[T]*): DataboundFieldDescriptor[S, T] = {
    copy(field = field.validateWith(bindingValidators:_*))
  }

  def copy(value: FieldValidation[T] = value, field: DataboundFieldDescriptor[S, T] = field): ValidatedFieldDescriptor[S, T] =
    new ValidatedBoundFieldDescriptor(value, field)

  def transform(endo: T => T): DataboundFieldDescriptor[S, T] = copy(value = value map endo)

  def required = copy(field = field.required)

  def optional = copy(field = field.optional)

  def validator: Option[Validator[T]] = field.validator

  def original: S = field.original

  private[databinding] def transformations: (T) => T = field.transformations
}

import scala.util.matching.Regex


trait BindingValidatorImplicits {

  import BindingValidators._
  implicit def validatableStringBinding(b: FieldDescriptor[String]) = new ValidatableStringBinding(b)
  implicit def validatableSeqBinding[T <: Seq[_]](b: FieldDescriptor[T]) = new ValidatableSeq(b)
  implicit def validatableGenericBinding[T](b: FieldDescriptor[T]) = new ValidatableGenericBinding(b)
  implicit def validatableOrderedBinding[T <% Ordered[T]](b: FieldDescriptor[T]) = new ValidatableOrdered(b)

}

object BindingValidators {


  class ValidatableSeq[T <: Seq[_]](b: FieldDescriptor[T]) {
    def notEmpty: FieldDescriptor[T] =
      b.required.validateWith(BindingValidators.nonEmptyCollection)
  }


  class ValidatableOrdered[T <% Ordered[T]](b: FieldDescriptor[T]) {
    def greaterThan(min: T): FieldDescriptor[T] =
      b.validateWith(BindingValidators.greaterThan(min))

    def lessThan(max: T): FieldDescriptor[T] =
      b.validateWith(BindingValidators.lessThan(max))

    def greaterThanOrEqualTo(min: T): FieldDescriptor[T] =
      b.validateWith(BindingValidators.greaterThanOrEqualTo(min))

    def lessThanOrEqualTo(max: T): FieldDescriptor[T] =
      b.validateWith(BindingValidators.lessThanOrEqualTo(max))

  }

  class ValidatableGenericBinding[T](b: FieldDescriptor[T]) {
    def oneOf(expected: T*): FieldDescriptor[T] =
      b.validateWith(BindingValidators.oneOf(expected:_*))

    def validate(validate: T => Boolean): FieldDescriptor[T] = b.validateWith(BindingValidators.validate(validate))
  }

  class ValidatableStringBinding(b: FieldDescriptor[String]) {
    def notBlank: FieldDescriptor[String] = b.required.validateWith(BindingValidators.nonEmptyString)

    def validEmail: FieldDescriptor[String] = b.validateWith(BindingValidators.validEmail)

    def validAbsoluteUrl(allowLocalHost: Boolean, schemes: String*): FieldDescriptor[String] =
      b.validateWith(BindingValidators.validAbsoluteUrl(allowLocalHost, schemes:_*))

    def validUrl(allowLocalHost: Boolean, schemes: String*): FieldDescriptor[String] =
      b.validateWith(BindingValidators.validUrl(allowLocalHost, schemes:_*))

    def validForFormat(regex: Regex, messageFormat: String = "%s is invalid."): FieldDescriptor[String] =
      b.validateWith(BindingValidators.validFormat(regex, messageFormat))

    def validForConfirmation(against: Field[String]): FieldDescriptor[String] =
      b.validateWith(BindingValidators.validConfirmation(against))


    def minLength(min: Int): FieldDescriptor[String] =
      b.validateWith(BindingValidators.minLength(min))


    def enumValue(enum: Enumeration): FieldDescriptor[String] =
      b.validateWith(BindingValidators.enumValue(enum))
  }

  import org.scalatra.validation.Validation


  def validate[TValue](validate: TValue => Boolean): BindingValidator[TValue] = (s: String) => {
    _ flatMap (Validators.validate(s, validate = validate).validate(_))
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

  def validConfirmation(against: Field[String]): BindingValidator[String] = (s: String) =>{
    _ flatMap { Validators.validConfirmation(s, against.name, ~against.value).validate(_) }
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

class Field[A:Manifest:Zero](descr: FieldDescriptor[A], command: Command) {

  val name = descr.name
  def validation: FieldValidation[A] = binding.field.value.asInstanceOf[FieldValidation[A]]
  def value: Option[A] = binding.field.value.toOption.asInstanceOf[Option[A]]
  def error: Option[ValidationError] = binding.field.value.fail.toOption
  def original = binding.original

  def binding: Binding = command.bindings(name)

  def isValid = validation.isSuccess
  def isInvalid = validation.isFailure

}