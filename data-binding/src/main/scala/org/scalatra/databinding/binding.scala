package org.scalatra
package databinding

import org.scalatra.util.conversion._
import validation.{Validators, FieldError, FieldValidation, Validator}
import java.text.DateFormat
import java.util.Date
import scalaz._
import Scalaz._
import util.{ValueReader, ParamsValueReaderProperties}
import org.scalatra.json.{JsonSupport, JsonValueReaderProperty}


trait BindingSupport extends ParamsValueReaderProperties with JsonValueReaderProperty { self: ScalatraBase with JsonSupport =>
  def bindTo[T](data: T, params: MultiParams, paramsOnly: Boolean = true)(implicit read: T => ValueReader[T], multiParams: MultiParams => ValueReader[MultiParams]) = {

  }
}

abstract class Binding[T] {

  def name: String
  def value: Option[T]
  def validators: Seq[Validator[T]]

  override def toString() = "Binding(name: %s)".format(name)
  
  def validateWith(validators: BindingValidator[T]*): Binding[T]

  def canValidate: Boolean

}

case class BoundBinding[S: Manifest, T: Manifest](original: S, value: Option[T], binding: Binding[T]) extends Binding[T] {
  def name: String = binding.name

  override def hashCode(): Int = 41 * (41 * (41 + name.##) + original.##) + value.##
  override def toString() = "BoundBinding(name: %s, original: %s, converted: %s)".format(name, original, value)

  def validateWith(bindingValidators: BindingValidator[T]*): Binding[T] = {
    copy(binding = binding.validateWith(bindingValidators:_*))
  }

  def canValidate: Boolean = true

  def validators: Seq[_root_.org.scalatra.validation.Validator[T]] = binding.validators
}


class BasicBinding[T: Manifest](val name: String, val validators: Seq[Validator[T]] = Nil) extends Binding[T] {
  val value: Option[T] = None
  def validateWith(bindingValidators: BindingValidator[T]*): Binding[T] = {
    copy(validators = validators ++ bindingValidators.map(_.apply(name)))
  }

  def copy(name: String = name, validators: Seq[Validator[T]] = validators) = new BasicBinding(name, validators)

  def apply[S](original: Option[S])(implicit mf: Manifest[S], zero: Zero[S], convert: TypeConverter[S, T]): Binding[T] =
    BoundBinding(~original, convert(~original), this)

  def canValidate: Boolean = false
}


object Binding {

  def apply[T: Manifest](name: String): Binding[T] = new BasicBinding[T](name)

}

/**
* Commonly-used field implementations factory.
*
* @author mmazzarolo
*/
trait BindingImplicits extends DefaultImplicitConversions {

  def asType[T:Manifest](name: String): Binding[T] = Binding[T](name)

}

object BindingImplicits extends BindingImplicits

import scala.util.matching.Regex

class ValidatableSeq[T <: Seq[_]](b: Binding[T]) {
  def nonEmpty: Binding[T] =
    b.validateWith(BindingValidators.nonEmptyCollection)
}


class ValidatableOrdered[T <% Ordered[T]](b: Binding[T]) {
  def greaterThan(min: T): Binding[T] =
    b.validateWith(BindingValidators.greaterThan(min))

  def lessThan(max: T): Binding[T] =
    b.validateWith(BindingValidators.lessThan(max))

  def greaterThanOrEqualTo(min: T): Binding[T] =
    b.validateWith(BindingValidators.greaterThanOrEqualTo(min))

  def lessThanOrEqualTo(max: T): Binding[T] =
    b.validateWith(BindingValidators.lessThanOrEqualTo(max))

}

class ValidatableAnyBinding(b: Binding[AnyRef]) {
  def required: Binding[AnyRef] = b.validateWith(BindingValidators.notNull)
}

class ValidatableGenericBinding[T](b: Binding[T]) {
  def oneOf(expected: T*): Binding[T] =
    b.validateWith(BindingValidators.oneOf(expected:_*))

  def validate(validate: T => Boolean): Binding[T] = b.validateWith(BindingValidators.validate(validate))
}

class ValidatableStringBinding(b: Binding[String]) {
  def notBlank: Binding[String] = b.validateWith(BindingValidators.nonEmptyString)

  def validEmail: Binding[String] = b.validateWith(BindingValidators.validEmail)

  def validAbsoluteUrl(allowLocalHost: Boolean, schemes: String*): Binding[String] =
    b.validateWith(BindingValidators.validAbsoluteUrl(allowLocalHost, schemes:_*))

  def validUrl(allowLocalHost: Boolean, schemes: String*): Binding[String] =
    b.validateWith(BindingValidators.validUrl(allowLocalHost, schemes:_*))

  def validForFormat(regex: Regex, messageFormat: String = "%s is invalid."): Binding[String] =
    b.validateWith(BindingValidators.validFormat(regex, messageFormat))

  def validForConfirmation(confirmationFieldName: String, confirmationValue: String): Binding[String] =
    b.validateWith(BindingValidators.validConfirmation(confirmationFieldName, confirmationValue))


  def minLength(min: Int): Binding[String] =
    b.validateWith(BindingValidators.minLength(min))


  def enumValue(enum: Enumeration): Binding[String] =
    b.validateWith(BindingValidators.enumValue(enum))
}
trait BindingValidatorImplicits {



  implicit def validatableStringBinding(b: Binding[String]) = new ValidatableStringBinding(b)
  implicit def validatableSeqBinding[T <: Seq[_]](b: Binding[T]) = new ValidatableSeq(b)
  implicit def validatableGenericBinding[T](b: Binding[T]) = new ValidatableGenericBinding(b)
  implicit def validatableAnyBinding(b: Binding[AnyRef]) = new ValidatableAnyBinding(b)
  implicit def validatableOrderedBinding[T <% Ordered[T]](b: Binding[T]) = new ValidatableOrdered(b)
  
}

object BindingValidatorImplicits extends BindingValidatorImplicits

object BindingValidators { 

  import org.scalatra.validation.Validation
  
  
  def validate[TValue](validate: TValue => Boolean): BindingValidator[TValue] = (s: String) => {
    case o => Validators.validate(s, validate).validate(o getOrElse null.asInstanceOf[TValue])
  }
  
  def nonEmptyString: BindingValidator[String] = (s: String) => {
    case o => Validation.nonEmptyString(s, o getOrElse "")
  }
  
  def notNull: BindingValidator[AnyRef] = (s: String) => {
    case o => Validation.notNull(s, o getOrElse "")
  }

  def nonEmptyCollection[TResult <: Seq[_]]: BindingValidator[TResult] = (s: String) =>{
    case v => Validation.nonEmptyCollection(s, v getOrElse Nil.asInstanceOf[TResult])
  }

  def validEmail: BindingValidator[String] = (s: String) =>{
    case m => Validation.validEmail(s, m getOrElse "")
  }

  def validAbsoluteUrl(allowLocalHost: Boolean, schemes: String*): BindingValidator[String] = (s: String) =>{
    case value => Validators.validAbsoluteUrl(s, allowLocalHost, schemes:_*).validate(value getOrElse "")
  }

  def validUrl(allowLocalHost: Boolean, schemes: String*): BindingValidator[String] = (s: String) =>{
    case value => Validators.validUrl(s, allowLocalHost, schemes:_*).validate(value getOrElse "")
  }

  def validFormat(regex: Regex, messageFormat: String = "%sis invalid."): BindingValidator[String] = (s: String) =>{
    case value => Validators.validFormat(s, regex, messageFormat).validate(value getOrElse "")
  }

  def validConfirmation(confirmationFieldName: String, confirmationValue: String): BindingValidator[String] = (s: String) =>{
    case value => Validators.validConfirmation(s, confirmationFieldName, confirmationValue).validate(value getOrElse "")
  }

  def greaterThan[T <% Ordered[T]](min: T): BindingValidator[T] = (s: String) =>{
    case value => Validators.greaterThan(s, min).validate(value getOrElse null.asInstanceOf[T])
  }

  def lessThan[T <% Ordered[T]](max: T): BindingValidator[T] = (s: String) =>{
    case value => Validators.lessThan(s, max).validate(value getOrElse  null.asInstanceOf[T])
  }

  def greaterThanOrEqualTo[T <% Ordered[T]](min: T): BindingValidator[T] = (s: String) =>{
    case value => Validators.greaterThanOrEqualTo(s, min).validate(value getOrElse  null.asInstanceOf[T])
  }

  def lessThanOrEqualTo[T <% Ordered[T]](max: T): BindingValidator[T] = (s: String) =>{
    case value => Validators.lessThanOrEqualTo(s, max).validate(value getOrElse  null.asInstanceOf[T])
  }

  def minLength(min: Int): BindingValidator[String] = (s: String) =>{
    case value => Validators.minLength(s, min).validate(value getOrElse  "")
  }

  def oneOf[TResult](expected: TResult*): BindingValidator[TResult] = (s: String) => {
    case value => Validators.oneOf(s, expected:_*).validate(value getOrElse Nil.asInstanceOf[TResult])
  }

  def enumValue(enum: Enumeration): BindingValidator[String] = oneOf(enum.values.map(_.toString).toSeq:_*)
}