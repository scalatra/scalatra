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




object BindingContainer {
  def apply[I, A](fieldName: String, cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): BindingContainer = {
    apply(Binding[A](fieldName), cv, tcf)
  }

  def apply[I, A](prev: Binding[A], cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): BindingContainer = {
    new DefaultBindingContainer(prev, tcf)(mf, z, mt, za, cv)
  }

}
sealed trait ContainerForBinding {
  type T

  private[this] var _binding: Binding[T] = null
  def binding: Binding[T]
  private[ContainerForBinding] def binding_=(newBinding: Binding[T]) = {
    _binding = newBinding
  }
  private[databinding] def withBinding[A](newBinding: Binding[A]): Binding[A] = {
    _binding = newBinding.asInstanceOf[Binding[T]]
    newBinding
  }

  def name: String = binding.name
  def value: Option[T] = binding.value

  implicit def valueManifest: Manifest[T]
  implicit def valueZero: Zero[T]

  def typeConverterFactory: TypeConverterFactory[_]

//  def original: Option[_] = None

}

object NewBindingContainer {
  def apply[A](initial: String)(implicit ma: Manifest[A], za: Zero[A], tcFactory: TypeConverterFactory[A]): NewBindingContainer = apply(Binding[A](initial))
  def apply[A](initial: Binding[A])(implicit ma: Manifest[A], za: Zero[A], tcFactory: TypeConverterFactory[A]): NewBindingContainer = {

    new NewBindingContainer {

      type T = A
      val binding: Binding[T] = initial
      implicit def valueManifest: Manifest[T] = ma
      implicit def valueZero: Zero[T] = za

      implicit def typeConverterFactory: TypeConverterFactory[_] = tcFactory

    }

  }

}



trait NewBindingContainer extends ContainerForBinding {


  override def toString() = 
    "BindingContainer[%s](name: %s, value: %s)".format(valueManifest.erasure.getSimpleName, name, value)
}

private class DefaultBindingContainer[I, A]
                (val binding: Binding[A], val typeConverterFactory: TypeConverterFactory[_])(
                    implicit
                    val sourceManifest: Manifest[I],
                    val sourceZero: Zero[I],
                    val valueManifest: Manifest[A],
                    val valueZero: Zero[A],
                    val typeConverter: TypeConverter[I, A]) extends BindingContainer {
  type T = A
  type S = I
}

trait BindingContainer  extends NewBindingContainer {
  type S
   
  implicit def sourceManifest: Manifest[S]
  implicit def sourceZero: Zero[S]

  def original: Option[S] = binding match {
    case v: ValidatableBinding[_, _] => Some(v.original.asInstanceOf[S])
    case _ => None
  }


  implicit def typeConverter: TypeConverter[S, T]

  override def toString() = {
    "BindingContainer[%s, %s](name: %s, original: %s, value: %s)".format(sourceManifest.erasure.getSimpleName, valueManifest.erasure.getSimpleName, name, value, original)
  }

  def apply(toBind: Option[S]): BindingContainer = 
    new DefaultBindingContainer(binding(toBind), typeConverterFactory)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)

}

trait Binding[T] {

  def name: String
  def value: Option[T]
  def validators: Seq[Validator[T]]

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

  def validators: Seq[Validator[T]] = binding.validators

  def validate: ValidatedBinding[S, T] = new ValidatedBindingDecorator(this)

  def transform(endo: T => T): ValidatableBinding[S, T] = copy(value = value map endo)

}

class BasicBinding[T](val name: String, val validators: Seq[Validator[T]] = Nil, transformations: Seq[T => T] = Nil) extends Binding[T] {

  val value: Option[T] = None

  def validateWith(bindingValidators: BindingValidator[T]*): Binding[T] = {
    copy(validators = validators ++ bindingValidators.map(_.apply(name)))
  }

  def copy(name: String = name, validators: Seq[Validator[T]] = validators, transformations: Seq[T => T] = transformations): Binding[T] =
    new BasicBinding(name, validators, transformations)

  def apply[S](original: Option[S])(implicit zero: Zero[S], convert: TypeConverter[S, T]): ValidatableBinding[S, T] = {
    val endo: T => T = transformations.nonEmpty ? transformations.reduce(_ andThen _) | identity
    val o = ~original
    BoundBinding(o, convert(o) map endo, this)
  }

  def transform(endo: T => T): Binding[T] = copy(transformations = transformations :+ endo)

}

class ContainerBinding[T](name: String, validators: Seq[Validator[T]], transformations: Seq[T => T] = Nil, container: ContainerForBinding) extends BasicBinding[T](name, validators) {
  override def copy(name: String = name, validators: Seq[Validator[T]] = validators, transformations: Seq[T => T] = transformations): Binding[T] = {
    container withBinding new ContainerBinding(name, validators, transformations, container)
  }

  override def apply[S](original: Option[S])(implicit zero: Zero[S], convert: TypeConverter[S, T]): ValidatableBinding[S, T] = {
    val endo: T => T = transformations.nonEmpty ? transformations.reduce(_ andThen _) | identity
    // Strip command registration from here on out, the bound command takes over for that task
    val bnd = new BoundContainerBinding(~original, convert(~original) map endo, new BasicBinding(name, validators, transformations), container)
    (container withBinding bnd).asInstanceOf[ValidatableBinding[S, T]]
  }

}

class BoundContainerBinding[S, T](original: S, value: Option[T], binding: Binding[T], container: ContainerForBinding) extends BoundBinding[S,T](original, value, binding) {
  override def copy(original: S = original, value: Option[T] = value, binding: Binding[T] = binding): ValidatableBinding[S, T] =
    (container withBinding new BoundContainerBinding(original, value, binding, container)).asInstanceOf[ValidatableBinding[S, T]]


  override def hashCode() = binding.hashCode()

  override def equals(obj: Any) = binding.equals(obj)

}



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

  def copy(binding: ValidatableBinding[S, T] = binding): ValidatedBinding[S, T]  = new ValidatedBindingDecorator(binding)

  def validateWith(validators: databinding.BindingValidator[T]*): Binding[T] =
    copy(binding.validateWith(validators:_*).asInstanceOf[ValidatableBinding[S, T]])

  def canValidate: Boolean = true

  def validate: ValidatedBinding[S, T] = copy(binding)

  def transform(endo: T => T): ValidatedBinding[S, T] = copy(binding transform endo)

}


object Binding {

  def apply[T](name: String): Binding[T] = new BasicBinding(name)

}

//trait CommandBindingSyntax extends BindingSyntax {
//
//  private[this] var _stringParams: PartialFunction[Manifest[_], TypeConverter[String, _]] = {
//    case a => throw new BindingException("No conversion registered for " + a.erasure.getName)
//  }
//  private[this] var _multiParams: PartialFunction[Manifest[_], TypeConverter[Seq[String], _]] = {
//    case a => throw new BindingException("No conversion registered for " + a.erasure.getName)
//  }
//
//  def stringParamsConverters = _stringParams
//  def multiParamsConverters = _multiParams
//
//  protected def valueReaderToConverter: PartialFunction[ValueReader[_, _], PartialFunction[Manifest[_], TypeConverter[_, _]]] = {
//    case _: MultiParamsValueReader => multiParamsConverters
//    case _ => stringParamsConverters
//  }
//
//  override implicit def safe[S:Manifest, T:Manifest](f: (S) => T): TypeConverter[S, T] = {
//    val conv: TypeConverter[S, T] = super.safe(f)
//    addToParamsPartialFunction(conv)(manifest[S], manifest[T])
//    conv
//  }
//
//  private[this] def addToParamsPartialFunction[S, T](conv: TypeConverter[S, T])(implicit ms: Manifest[S], mt: Manifest[T]) {
//    val mf = manifest[S]
//    mf match {
//      case a if a <:< manifest[String] =>
//        val pf: PartialFunction[Manifest[_], TypeConverter[String, T]] = {
//          case b if b <:< manifest[T] => conv.asInstanceOf[TypeConverter[String, T]]
//        }
//        _stringParams = pf orElse _stringParams
//
//      case a if a <:< manifest[Seq[String]] =>
//        val pf: PartialFunction[Manifest[_], TypeConverter[Seq[String], T]] = {
//          case b if b <:< manifest[T] => conv.asInstanceOf[TypeConverter[Seq[String], T]]
//        }
//        _multiParams = pf orElse _multiParams
//    }
//
//  }
//
//  override implicit def safeOption[S:Manifest, T:Manifest](f: (S) => Option[T]): TypeConverter[S, T] = {
//    val conv: TypeConverter[S, T] = super.safeOption(f)
//    addToParamsPartialFunction(conv)(manifest[S], manifest[T])
//    conv
//  }
//
//}

trait BindingSyntax extends TypeConverterFactories {


  implicit def asType[T](name: String): Binding[T] = Binding[T](name)

  def asBoolean(name: String): Binding[Boolean] = Binding[Boolean](name)
  def asByte(name: String): Binding[Byte] = Binding[Byte](name)
  def asShort(name: String): Binding[Short] = Binding[Short](name)
  def asInt(name: String): Binding[Int] = Binding[Int](name)
  def asLong(name: String): Binding[Long] = Binding[Long](name)
  def asFloat(name: String): Binding[Float] = Binding[Float](name)
  def asDouble(name: String): Binding[Double] = Binding[Double](name)
  def asBigDecimal(name: String): Binding[BigDecimal] = Binding[BigDecimal](name)
  def asString(name: String): Binding[String] = Binding[String](name)
  def asDate(name: String): Binding[Date] = Binding[Date](name)
  def asDateTime(name: String): Binding[DateTime] = Binding[DateTime](name)
  def asSeq[T](name: String): Binding[Seq[T]] = Binding[Seq[T]](name)
}

object BindingSyntax extends BindingSyntax



/**
* Commonly-used field implementations factory.
*
* @author mmazzarolo
*/
trait BindingImplicits extends DefaultImplicitConversions with BindingValidatorImplicits {

  implicit def stringToDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[String, DateTime] =
    safeOption(df.parse)

  implicit def stringToDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[String, Date] =
    safeOption(df.parse(_).map(_.toDate))

  implicit def stringToSeqDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[String, Seq[DateTime]] =
    stringToSeq(stringToDateTime)

  implicit def stringToSeqDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[String, Seq[Date]] =
    stringToSeq(stringToDate)

  implicit def stringSeqToHeadDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[Seq[String], Date] =
    seqHead(stringToDate)

  implicit def stringSeqToHeadDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[Seq[String], DateTime] =
    seqHead(stringToDateTime)

  implicit def stringSeqToSeqDate(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[Seq[String], Seq[Date]] =
    seqToSeq(stringToDate)

  implicit def stringSeqToSeqDateTime(implicit df: DateParser = JodaDateFormats.Web): TypeConverter[Seq[String], Seq[DateTime]] =
    seqToSeq(stringToDateTime)




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