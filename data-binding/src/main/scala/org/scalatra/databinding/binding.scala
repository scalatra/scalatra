package org.scalatra
package databinding

import org.scalatra.util.conversion._
import validation._
import java.util.Date
import scalaz._
import Scalaz._
import org.joda.time.DateTime
import util.{MultiParamsValueReader, StringMapValueReader, ValueReader}
import java.text.{DateFormat, SimpleDateFormat}

class BindingException(message: String) extends ScalatraException(message)
//
//object B {
//  type BinderContainerBuilder[S, I] = (S, ValueReader[S, I]) => BindingContainer
//
//  import BindingImplicits._
//  val binding = Binding[String]("login").notBlank.minLength(6)
//  val builders = Seq()
////  def apply[U: Manifest: Zero, S <% ValueReader[S, U], T](b: Binding[T])(data: R) =
//}

object BindingContainer {
  def apply[I, A](fieldName: String, cv: TypeConverter[I, A])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): BindingContainer = {
    new BindingContainer {
      type T = A
      type S = I
      implicit val typeConverter: TypeConverter[S, T] = cv
      val binding = Binding(fieldName, za.zero)
      implicit val sourceManifest: Manifest[S] = mf
      implicit val sourceZero: Zero[S] = z
      implicit val valueManifest: Manifest[T] = mt
      implicit val valueZero: Zero[T] = za
    }
  }

  def apply[I, A](prev: Binding[A], cv: TypeConverter[I, A])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): BindingContainer = {
    new BindingContainer {
      type T = A
      type S = I
      val binding = prev
      implicit val typeConverter: TypeConverter[S, T] = cv
      implicit val sourceManifest: Manifest[S] = mf
      implicit val sourceZero: Zero[S] = z
      implicit val valueManifest: Manifest[T] = mt
      implicit val valueZero: Zero[T] = za
    }
  }

}
trait BindingContainer  {

  type T
  implicit def valueManifest: Manifest[T]
  implicit def valueZero: Zero[T]

  type S
  implicit def sourceManifest: Manifest[S]
  implicit def sourceZero: Zero[S]

  implicit def typeConverter: TypeConverter[S, T]
  def binding: Binding[T]
  def name = binding.name
  def value = binding match {
    case b: ValidatedBinding[_, _] => b.asInstanceOf[ValidatedBinding[S, T]].value
    case _ => binding.value
  }
  def original = binding match {
    case b: ValidatedBinding[_, _] => b.original
    case _ => None
  }

  override def toString() = {
    "BindingContainer[%s, %s](name: %s, original: %s, value: %s)".format(sourceManifest.erasure.getSimpleName, valueManifest.erasure.getSimpleName, name, binding.value, original)
  }

  def apply(toBind: Option[S]): BindingContainer = {

    val b = binding(toBind)
    println("bound: " + b)
    println("bound: " + b.value)
    new BindingContainer {
      type T = BindingContainer.this.T
      type S = BindingContainer.this.S
      val binding: Binding[T] = b
      override val value = b.value
//      val value = binding.value
      println("binding: " + binding)
      implicit val typeConverter: TypeConverter[S, T] = BindingContainer.this.typeConverter
      implicit val sourceManifest: Manifest[S] = BindingContainer.this.sourceManifest
      implicit val sourceZero: Zero[S] = BindingContainer.this.sourceZero
      implicit val valueManifest: Manifest[T] = BindingContainer.this.valueManifest
      implicit val valueZero: Zero[T] = BindingContainer.this.valueZero
    }
  }

//  def bindTo[U:Manifest:Zero, R](data: R)(
//          implicit mf: Manifest[U], z: Zero[U], mr: Manifest[R], r: R => ValueReader[R, U]): BindingContainer = {
//    val d = data.read(binding.name).map(_.asInstanceOf[S])
//    val b = binding(d)
//    new BindingContainer  {
//      type S = U
//      type T = BindingContainer.this.T
//      val binding: Binding[T] = b.asInstanceOf[Binding[T]]
//      implicit val typeConverter: TypeConverter[S, T] = BindingContainer.this.typeConverter.asInstanceOf[TypeConverter[S, T]]
//    }
//
//  }
}
//
//trait BindableContainer extends BindingContainer {
//  type S
//  implicit def sourceManifest: Manifest[S] = manifest[S]
//  implicit def zero: Zero[S] = implicitly[Zero[S]]
//}

trait Binding[T] {

  def name: String
  def value: Option[T]
  def validators: Seq[Validator[T]]

//  def defaultValue: T
//  def withDefault(default: T): Binding[T]

  override def toString() = "Binding(name: %s)".format(name)
  
  def validateWith(validators: BindingValidator[T]*): Binding[T]

  def apply[S](original: Option[S])(implicit zero: Zero[S], convert: TypeConverter[S, T]): ValidatableBinding[S, T]

  override def hashCode() = 13 + 17 * name.hashCode()

  def transform(endo: T => T): Binding[T]

  override def equals(obj: Any) = obj match {
    case b : Binding[_] => b.name == this.name
    case _ => false
  }

}

trait ValidatableBinding[S, T] extends Binding[T] {
  def binding: Binding[T]
  def original: S
  def validate: ValidatedBinding[S, T]
//  def map[R: Manifest](endo: T => R): ValidatableBinding[S, R]
  def transform(endo: T => T): ValidatableBinding[S, T]
  def apply[V](original: Option[V])(implicit zero: Zero[V], convert: TypeConverter[V, T]): ValidatableBinding[V, T] =
    this.asInstanceOf[ValidatableBinding[V, T]]

  override def toString() = "Binding(name: %s, original: %s, value: %s)".format(name, original, value)
}

object BoundBinding {
  def apply[S, T](original: S, value: Option[T], binding: Binding[T]): ValidatableBinding[S, T] =
    new BoundBinding(original, value, binding)
}
class BoundBinding[S, T](val original: S, val value: Option[T], val binding: Binding[T]) extends ValidatableBinding[S, T] {
  def name: String = binding.name

  override def hashCode(): Int = binding.hashCode()
  override def equals(other: Any) = binding.equals(other)
  override def toString() = "BoundBinding(name: %s, original: %s, converted: %s)".format(name, original, value)

  def validateWith(bindingValidators: BindingValidator[T]*): Binding[T] = {
    copy(binding = binding.validateWith(bindingValidators:_*))
  }

  def copy(original: S = original, value: Option[T] = value, binding: Binding[T] = binding): ValidatableBinding[S, T] =
    new BoundBinding(original, value, binding)
//
//  def defaultValue: T = binding.defaultValue
//
//  def withDefault(default: T): Binding[T] = copy(binding = binding.withDefault(default))

  def validators: Seq[Validator[T]] = binding.validators
//
//  implicit def valueManifest: Manifest[T] = binding.valueManifest

  def validate: ValidatedBinding[S, T] = new ValidatedBindingDecorator(this)

  def transform(endo: T => T): ValidatableBinding[S, T] = copy(value = value map endo)

//  def map[R: Manifest](endo: (T) => R): ValidatableBinding[S, R] =
//    BoundBinding(original, value map endo, Binding(name, endo(defaultValue)))
}

class BasicBinding[T](val name: String, val validators: Seq[Validator[T]] = Nil, val defaultValue: T = null.asInstanceOf[T], transformations: Seq[T => T] = Nil) extends Binding[T] {

  val value: Option[T] = None

  def validateWith(bindingValidators: BindingValidator[T]*): Binding[T] = {
    copy(validators = validators ++ bindingValidators.map(_.apply(name)))
  }

//  def withDefault(default: T): Binding[T] = copy(defaultValue = default)

  def copy(name: String = name, validators: Seq[Validator[T]] = validators, defaultValue: T = defaultValue, transformations: Seq[T => T] = transformations): Binding[T] =
    new BasicBinding(name, validators, defaultValue, transformations)

  override def apply[S](original: Option[S])(implicit zero: Zero[S], convert: TypeConverter[S, T]): ValidatableBinding[S, T] = {
    println("binding to " + original)
    val endo: T => T = transformations.nonEmpty ? transformations.reduce(_ andThen _) | identity
    val o = ~original
    val c = convert(o)
    val m = c map endo
    val s = this
    BoundBinding(o, m, s)
  }

  def transform(endo: T => T): Binding[T] = copy(transformations = transformations :+ endo)

//  def map[R: Manifest](endo: (T) => R): Binding[R] = {
//    val thisBinding = this
//    val newValidators = validators.head.apply(Some("")).map(endo)
//    new BasicBinding[R](name, validators, endo(defaultValue)) {
//      override def apply[S](original: Option[S])(implicit mf: Manifest[S], zero: Zero[S], convert: (S) => Option[R]): ValidatableBinding[S, R] = {
//        thisBinding.apply(original) map endo
//      }
//    }
//  }
}

//object BoundCommandBinding {
//  def apply[S, T](original: S, value: Option[T], binding: Binding[T])(implicit command: Command): BoundBinding[S, T] =
//      new BoundCommandBinding(original, value, binding)
//}
//
//class BoundCommandBinding[S, T](original: S, value: Option[T], binding: Binding[T])(implicit command: Command) extends BoundBinding(original, value, binding) {
//
//  override def copy(original: S = original, value: Option[T] = value, binding: Binding[T] = binding): ValidatableBinding[S, T] =
//    (command replace BoundCommandBinding(original, value, binding)).asInstanceOf[ValidatableBinding[S, T]]
//
//  override def validate: ValidatedBinding[S, T] =
//    new ValidatedCommandBinding(BoundBinding(original, value, binding))
//
//
//  override def hashCode() = binding.hashCode()
//
//  override def equals(obj: Any) = binding.equals(obj)
//
//
////
////  override def map[R: Manifest](endo: (T) => R): ValidatableBinding[S, R] = {
////    (command replace BoundCommandBinding(original, value map endo, Binding(name, endo(defaultValue)))).asInstanceOf[ValidatableBinding[S, R]]
////  }
//}

//object CommandBinding {
//  def apply[T](name: String, defaultValue: T = null.asInstanceOf[T])(implicit mf: Manifest[T], command: Command): Binding[T] = {
//    new CommandBinding(name, defaultValue = defaultValue)
//  }
//}
//class CommandBinding[T](
//        name: String,
//        validators: Seq[Validator[T]] = Nil,
//        defaultValue: T = null.asInstanceOf[T],
//        transformations: Seq[T => T] = Nil)(implicit valueManifest: Manifest[T], command: Command)
//          extends BasicBinding[T](name, validators, defaultValue, transformations) {
//
//  override def copy(name: String = name, validators: Seq[Validator[T]] = validators, defaultValue: T = defaultValue, transformations: Seq[T => T] = transformations): Binding[T] = {
//    command replace new CommandBinding(name, validators, defaultValue, transformations)
//  }
//
//  override def apply[S](original: Option[S])(implicit zero: Zero[S], convert: TypeConverter[S, T]): ValidatableBinding[S, T] = {
//    val endo: T => T = transformations.nonEmpty ? transformations.reduce(_ andThen _) | identity
//    // Strip command registration from here on out, the bound command takes over for that task
//    BoundCommandBinding(~original, convert(~original) map endo, new BasicBinding(name, validators, defaultValue, transformations))
//  }
//
////
////  def map[R: Manifest](endo: (T) => R): Binding[R] = {
////    val thisBinding = this
////    new CommandBinding[R](name, validators, endo(defaultValue)) {
////      override def apply[S](original: Option[S])(implicit mf: Manifest[S], zero: Zero[S], convert: (S) => Option[R]): ValidatableBinding[S, R] = {
////        thisBinding.apply(original) map endo
////      }
////    }
////  }
//
//}

/**
* A field [[org.scalatra.command.Binding]] which value has been validated.
*/
trait ValidatedBinding[S, T] extends ValidatableBinding[S, T] {

  def binding: ValidatableBinding[S, T]
  /**
   * Result of command. Either one of @Rejected or @Accepted
   */
  def validation: FieldValidation[T]

  /**
   * Check whether the the field value conforms to the user requirements.
   */
  def valid = validation.isSuccess

  /**
   * The rejected message, if any.
   */
  def rejected = validation.fail.toOption

//  def map[R: Manifest](endo: T => R): ValidatedBinding[S, R]
//
  def transform(endo: T => T): ValidatedBinding[S, T]


}

class ValidatedBindingDecorator[S, T](val binding: ValidatableBinding[S, T]) extends ValidatedBinding[S, T]  {

  lazy val validation =
    validators.find(_.apply(value).isFailure).map(_.apply(value)).getOrElse(value.getOrElse(null.asInstanceOf[T]).success)

  def name = binding.name

  def original = binding.original

  def value = binding.value

  override def hashCode() = binding.hashCode()

  override def equals(obj: Any) = binding.equals(obj)

  def validators: Seq[Validator[T]] = binding.validators
//
//  implicit def valueManifest: Manifest[T] = binding.valueManifest
//
//  def defaultValue: T = binding.defaultValue

  def copy(binding: ValidatableBinding[S, T] = binding): ValidatedBinding[S, T]  = new ValidatedBindingDecorator(binding)

//  def withDefault(default: T): Binding[T] =
//    copy(binding.withDefault(default).asInstanceOf[ValidatableBinding[S, T]])

  def validateWith(validators: databinding.BindingValidator[T]*): Binding[T] =
    copy(binding.validateWith(validators:_*).asInstanceOf[ValidatableBinding[S, T]])

  def canValidate: Boolean = true

  def validate: ValidatedBinding[S, T] = copy(binding)

  def transform(endo: T => T): ValidatedBinding[S, T] = copy(binding transform endo)

//
//  def map[R: Manifest](endo: (T) => R): ValidatedBinding[S, R] = {
//    new ValidatedBindingDecorator(binding.map(endo)) {
//      override lazy val validation: FieldValidation[R] = ValidatedBindingDecorator.this.validation map endo
//    }
//  }
}

//class ValidatedCommandBinding[S, T](binding: ValidatableBinding[S, T])(implicit command: Command) extends ValidatedBindingDecorator(binding) {
//  override def copy(binding: ValidatableBinding[S, T]): ValidatedBinding[S, T] =
//    (command replace new ValidatedCommandBinding(binding)).asInstanceOf[ValidatedBinding[S, T]]
//
////  override def map[R: Manifest](endo: (T) => R): ValidatedBinding[S, R] =
////    (command replace new ValidatedCommandBinding(binding map endo)).asInstanceOf[ValidatedBinding[S, R]]
//}

object Binding {

  def apply[T](name: String, defaultValue: T = null.asInstanceOf[T]): Binding[T] =
    new BasicBinding(name, defaultValue = defaultValue)

}

trait BindingSyntax extends BindingImplicits {
//  def asGeneric[S, T](name: String, f: (S) => T)(implicit mf: Manifest[T], tc: TypeConverter[S, T]): Binding[T] =
//    new GenericBasicBinding(name)

  implicit def asType[T](name: String): Binding[T] = Binding[T](name)

  def asBoolean(name: String): Binding[Boolean] = Binding[Boolean](name)
  def asByte(name: String): Binding[Byte] = Binding[Byte](name)
  def asShort(name: String): Binding[Short] = Binding[Short](name)
  def asInt(name: String): Binding[Int] = Binding[Int](name)
  def asLong(name: String): Binding[Long] = Binding[Long](name)
  def asFloat(name: String): Binding[Float] = Binding[Float](name)
  def asDouble(name: String): Binding[Double] = Binding[Double](name)
  def asString(name: String): Binding[String] = Binding[String](name)
  def asDate(name: String): Binding[Date] = Binding[Date](name)
  def asDateTime(name: String): Binding[DateTime] = Binding[DateTime](name)
  def asSeq[T](name: String): Binding[Seq[T]] = Binding[Seq[T]](name)
}

//object BindingSyntax extends BindingSyntax
//class GenericBasicBinding[V, T: Manifest](name: String, process: V => T)(implicit mf: Manifest[T]) extends BasicBinding[T](name) {
//  override def apply[S](original: Option[S])(implicit mf: Manifest[S], zero: Zero[S], convert: (S) => Option[T]): Binding[T] =
//    BoundBinding(~original, original map (convert andThen process), this)
//}
//class GenericCommandBinding[T](name: String, process: T => T)(implicit mf: Manifest[T], command: Command) extends CommandBinding[T](name) {
//  override def apply[S](original: Option[S])(implicit mf: Manifest[S], zero: Zero[S], convert: (S) => Option[T]): Binding[T] = {
//    BoundCommandBinding(~original, original map (convert andThen process), new GenericBasicBinding[T](this.name, process))
//  }
//}
//trait CommandBindingSyntax extends BindingImplicits {
//
//  protected implicit def thisCommand: Command
//  implicit def asType[T:Manifest](name: String): Binding[T] = CommandBinding[T](name)
//
////  def asGeneric[S, T](name: String, f: (S) => T)(implicit mf: Manifest[T], tc: TypeConverter[S, T]): Binding[T] =
////    new GenericCommandBinding[T](name, (s: S) => tc(s))
//
//  def asBoolean(name: String): Binding[Boolean] = CommandBinding[Boolean](name)
//  def asByte(name: String): Binding[Byte] = CommandBinding[Byte](name)
//  def asShort(name: String): Binding[Short] = CommandBinding[Short](name)
//  def asInt(name: String): Binding[Int] = CommandBinding[Int](name)
//  def asLong(name: String): Binding[Long] = CommandBinding[Long](name)
//  def asFloat(name: String): Binding[Float] = CommandBinding[Float](name)
//  def asDouble(name: String): Binding[Double] = CommandBinding[Double](name)
//  def asString(name: String): Binding[String] = CommandBinding[String](name)
//  def asDate(name: String): Binding[Date] = CommandBinding[Date](name)
//  def asDateTime(name: String): Binding[DateTime] = CommandBinding[DateTime](name)
//  def asSeq[T:Manifest](name: String): Binding[Seq[T]] = CommandBinding[Seq[T]](name)
//}

/**
* Commonly-used field implementations factory.
*
* @author mmazzarolo
*/
trait BindingImplicits extends DefaultImplicitConversions with BindingValidatorImplicits {

  implicit def stringToDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[String, DateTime] =
    safeOption(s => df.parse(s))

  implicit def stringToDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[String, Date] =
    safeOption(s => df.parse(s).map(_.toDate))

}

object BindingImplicits extends BindingImplicits

import scala.util.matching.Regex


trait BindingValidatorImplicits {

  import BindingValidators._
  implicit def validatableStringBinding(b: Binding[String]) = new ValidatableStringBinding(b)
  implicit def validatableSeqBinding[T <: Seq[_]](b: Binding[T]) = new ValidatableSeq(b)
  implicit def validatableGenericBinding[T](b: Binding[T]) = new ValidatableGenericBinding(b)
  implicit def validatableAnyBinding(b: Binding[AnyRef]) = new ValidatableAnyBinding(b)
  implicit def validatableOrderedBinding[T <% Ordered[T]](b: Binding[T]) = new ValidatableOrdered(b)
  
}

object BindingValidators {


  class ValidatableSeq[T <: Seq[_]](b: Binding[T]) {
    def notEmpty: Binding[T] =
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