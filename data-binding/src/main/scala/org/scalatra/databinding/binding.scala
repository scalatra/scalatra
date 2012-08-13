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

trait Binding[T] {

  def name: String
  def value: Option[T]
  def validators: Seq[Validator[T]]

  override def toString() = "Binding(name: %s)".format(name)
  
  def validateWith(validators: BindingValidator[_]*): Binding[T]

  def canValidate: Boolean

}

case class BoundBinding[S: Manifest, T: Manifest](original: S, value: Option[T], binding: Binding[T]) extends Binding[T] with BindingValidators[T] {
  def name: String = binding.name

  override def hashCode(): Int = 41 * (41 * (41 + name.##) + original.##) + value.##
  override def toString() = "BoundBinding(name: %s, original: %s, converted: %s)".format(name, original, value)

  def validateWith(bindingValidators: BindingValidator[T]*): Binding[T] = {
    copy(binding = binding.validateWith(bindingValidators:_*))
  }
}


case class BasicBinding[T: Manifest](name: String, validators: Seq[Validator[T]] = Nil) extends Binding[T] with BindingValidators[T] {
  val value: Option[T] = None
  def validateWith(bindingValidators: BindingValidator[T]*): Binding[T] = {
    copy(validators = validators ++ bindingValidators.map(_.apply(name)))
  }

  def apply[S](original: Option[S])(implicit zero: Zero[S], convert: TypeConverter[S, T]): Binding[T] =
    BoundBinding(~original, convert(~original), this)
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

  def asType[T](name: String): Binding[T] = Binding[T](name)

}

object BindingImplicits extends BindingImplicits

import scala.util.matching.Regex
trait BindingValidators[T] { self: Binding[T] =>

  def validate[TValue](validate: TValue => Boolean) = validateWith(BindingValidators.validate(validate))
  def notBlank: Binding[T] = validateWith(BindingValidators.nonEmptyString)
  def required: Binding[T] = validateWith(BindingValidators.notNull)

  def nonEmpty[TResult <: Seq[_]]: Binding[T] =
    validateWith(BindingValidators.nonEmptyCollection)

  def validEmail: Binding[T] = validateWith(BindingValidators.validEmail)

  def validAbsoluteUrl(allowLocalHost: Boolean, schemes: String*): Binding[T] =
    validateWith(BindingValidators.validAbsoluteUrl(allowLocalHost, schemes:_*))

  def validUrl(allowLocalHost: Boolean, schemes: String*): Binding[T] =
    validateWith(BindingValidators.validUrl(allowLocalHost, schemes:_*))

  def validForFormat(regex: Regex, messageFormat: String = "%s is invalid."): Binding[T] =
    validateWith(BindingValidators.validFormat(regex, messageFormat))

  def validForConfirmation[V](confirmationFieldName: String, confirmationValue: => V): Binding[T] =
    validateWith(BindingValidators.validConfirmation(confirmationFieldName, confirmationValue))

  def greaterThan[V <% Ordered[V]](min: V): Binding[T] =
    validateWith(BindingValidators.greaterThan(min))

  def lessThan[V <% Ordered[V]](max: V): Binding[T] =
    validateWith(BindingValidators.lessThan(max))

  def greaterThanOrEqualTo[V <% Ordered[V]](min: V): Binding[T] =
    validateWith(BindingValidators.greaterThanOrEqualTo(min))

  def lessThanOrEqualTo[V <% Ordered[V]](max: V): Binding[T] =
    validateWith(BindingValidators.lessThanOrEqualTo(max))

  def minLength(min: Int): Binding[T] =
    validateWith(BindingValidators.minLength(min))

  def oneOf[TResult](expected: TResult*): Binding[T] =
    validateWith(BindingValidators.oneOf(expected:_*))

  def enumValue(enum: Enumeration): Binding[T] =
    validateWith(BindingValidators.enumValue(enum))
  
} 

object BindingValidators { 

  import org.scalatra.validation.Validation
  
  
  def validate[TValue](validate: TValue => Boolean): BindingValidator[TValue] = (s: String) => {
    case o => Validators.validate(s, validate)
  }
  
  def nonEmptyString: BindingValidator[String] = (s: String) => {
    case o => Validation.nonEmptyString(s, o getOrElse "")
  }
  
  def notNull: BindingValidator[AnyRef] = (s: String) => {
    case o => Validation.notNull(s, o getOrElse "")
  }

  def nonEmptyCollection[TResult <: Seq[_]]: BindingValidator[TResult] = (s: String) =>{
    case s => Validation.nonEmptyCollection(s, s getOrElse Nil.asInstanceOf[TResult])
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

  def enumValue(enum: Enumeration): BindingValidator[String] = (s: String) => 
    oneOf(s, enum.values.map(_.toString).toSeq: _*)
}