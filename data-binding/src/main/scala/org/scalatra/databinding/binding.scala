package org.scalatra
package databinding

import org.scalatra.util.conversion._
import validation._
import java.util.Date
import scalaz._
import Scalaz._
import org.joda.time.DateTime
import java.util.concurrent.atomic.AtomicReference
import Imports._
import scala.util.matching.Regex

class BindingException(message: String) extends ScalatraException(message)

object FieldBinding {


  def apply[A:Manifest:Zero:TypeConverterFactory](initial: Field[A]): FieldBinding = {
    val cmd = new FieldBinding()
    new PartialFieldBinding(initial, cmd)
    cmd
  }
  private class PartialFieldBinding[A](val field: Field[A], fieldBinding: FieldBinding)(implicit val valueManifest: Manifest[A],
                          val valueZero: Zero[A], val typeConverterFactory: TypeConverterFactory[A]) extends Binding {
    fieldBinding withBinding this
    type T = A
    type S = Null
    implicit def sourceManifest: Manifest[S] = null
    implicit def sourceZero: Zero[S] = null
    implicit def typeConverter: (S) => Option[T] = null
    def apply(toBind: Option[S]): Binding = null

    def validateWith(validators:BindingValidator[T]*): Binding =
      fieldBinding withBinding new PartialFieldBinding(field.validateWith(validators:_*), fieldBinding)

    def transform(transformer: (T) => T): Binding =
      fieldBinding withBinding new PartialFieldBinding(field transform transformer, fieldBinding)
  }

  private class DataboundFieldBinding[I, A]
                  (val field: Field[A], val typeConverterFactory: TypeConverterFactory[_], fieldBinding: FieldBinding)(
                      implicit
                      val sourceManifest: Manifest[I],
                      val sourceZero: Zero[I],
                      val valueManifest: Manifest[A],
                      val valueZero: Zero[A],
                      val typeConverter: TypeConverter[I, A]) extends Binding {

    fieldBinding withBinding this
    type T = A
    type S = I

    override def toString() = {
      "Binding[%s, %s](name: %s, original: %s, value: %s)".format(sourceManifest.erasure.getSimpleName, valueManifest.erasure.getSimpleName, name, value, original)
    }

    def transform(transformer: (T) => T): Binding = {
      val bnd = new DataboundFieldBinding(field.transform(transformer), typeConverterFactory, fieldBinding)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)
      fieldBinding withBinding bnd
    }

    def validateWith(validators:BindingValidator[T]*): Binding = {
      val bnd = new DataboundFieldBinding(field.validateWith(validators:_*), typeConverterFactory, fieldBinding)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)
      fieldBinding withBinding bnd
    }

    def apply(toBind: Option[S]): Binding =
      fieldBinding withBinding new DataboundFieldBinding(field(toBind), typeConverterFactory, fieldBinding)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)

  }
}
class FieldBinding{

  import FieldBinding.DataboundFieldBinding

  private[this] val _binding: AtomicReference[Binding] = new AtomicReference[Binding](null)
  def binding: Binding = _binding.get
  private[databinding] def withBinding(newBinding: Binding): Binding = {
    _binding.set(newBinding)
    newBinding
  }

  def bindData[I, A](prev: Field[A], cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): FieldBinding = {
    new DataboundFieldBinding(prev, tcf, this)(mf, z, mt, za, cv)
    this
  }


}

object Binding {


  def apply[I, A](fieldName: String, cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): Binding = {
    apply(Field[A](fieldName), cv, tcf)
  }

  def apply[I, A](prev: Field[A], cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): Binding = {
    new DefaultBinding(prev, tcf)(mf, z, mt, za, cv)
  }

  def apply[A](initial: String)(implicit ma: Manifest[A], za: Zero[A], tcFactory: TypeConverterFactory[A]): Binding = apply(Field[A](initial))
  def apply[A](initial: Field[A])(implicit ma: Manifest[A], za: Zero[A], tcFactory: TypeConverterFactory[A]): Binding = {
    new PartialBinding(initial)
  }

  private class PartialBinding[A](val field: Field[A])(implicit val valueManifest: Manifest[A],
                        val valueZero: Zero[A], val typeConverterFactory: TypeConverterFactory[A]) extends Binding {
    type T = A
    type S = Null
    implicit def sourceManifest: Manifest[S] = null
    implicit def sourceZero: Zero[S] = null
    implicit def typeConverter: (S) => Option[T] = null
    def apply(toBind: Option[S]): Binding = null

    def validateWith(validators:BindingValidator[T]*): Binding =
      new PartialBinding(field.validateWith(validators:_*))

    def transform(transformer: (T) => T): Binding =
      new PartialBinding(field transform transformer)
  }

  private class DefaultBinding[I, A]
                  (val field: Field[A], val typeConverterFactory: TypeConverterFactory[_])(
                      implicit
                      val sourceManifest: Manifest[I],
                      val sourceZero: Zero[I],
                      val valueManifest: Manifest[A],
                      val valueZero: Zero[A],
                      val typeConverter: TypeConverter[I, A]) extends Binding {
    type T = A
    type S = I

    override def toString() = {
      "Binding[%s, %s](name: %s, original: %s, value: %s)".format(sourceManifest.erasure.getSimpleName, valueManifest.erasure.getSimpleName, name, value, original)
    }


    def transform(transformer: (T) => T): Binding =
      new DefaultBinding(field.transform(transformer), typeConverterFactory)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)

    def validateWith(validators:BindingValidator[T]*): Binding =
      new DefaultBinding(field.validateWith(validators:_*), typeConverterFactory)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)

    def apply(toBind: Option[S]): Binding =
      new DefaultBinding(field(toBind), typeConverterFactory)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)

  }

}

sealed trait Binding {
  // We want to take advantage of compile time checking but we don't want the types
  // and potential changing of a type of a binding to be a problem
  // So we capture the type information in this trait taking the
  // generics out of play.
  // In addition this allows us to retain the type when we put things into
  // a sequence Seq[Binding]
  type T

  def field: Field[T]

  def name: String = field.name
  def value: FieldValidation[T] = field.value

  def isValid = value.isSuccess
  def isInvalid = value.isFailure


  implicit def valueManifest: Manifest[T]
  implicit def valueZero: Zero[T]

  def typeConverterFactory: TypeConverterFactory[_]

  type S

  implicit def sourceManifest: Manifest[S]
  implicit def sourceZero: Zero[S]

  def validateWith(validators: BindingValidator[T]*): Binding
  def transform(transformer: T => T): Binding

  def original: Option[S] = field match {
    case v: ValidatableField[_, _] => Some(v.original.asInstanceOf[S])
    case _ => None
  }


  implicit def typeConverter: TypeConverter[S, T]


  def apply(toBind: Option[S]): Binding

  override def toString() =
    "BindingContainer[%s](name: %s, value: %s, original: %s)".format(valueManifest.erasure.getSimpleName, name, value)

}

trait BindingProxy {
  implicit def fieldBinding2Binding(cmd: FieldBinding) : Binding = {
      val b = cmd.binding
      b
    }
}

trait BindingSyntax extends BindingProxy with BindingValidatorImplicits {



  implicit def asType[T:Zero](name: String): Field[T] = Field[T](name)

  def asBoolean(name: String): Field[Boolean] = Field[Boolean](name)
  def asByte(name: String): Field[Byte] = Field[Byte](name)
  def asShort(name: String): Field[Short] = Field[Short](name)
  def asInt(name: String): Field[Int] = Field[Int](name)
  def asLong(name: String): Field[Long] = Field[Long](name)
  def asFloat(name: String): Field[Float] = Field[Float](name)
  def asDouble(name: String): Field[Double] = Field[Double](name)
  def asBigDecimal(name: String): Field[BigDecimal] = Field[BigDecimal](name)
  def asString(name: String): Field[String] = Field[String](name)
  def asDate(name: String): Field[Date] = Field[Date](name)
  def asDateTime(name: String): Field[DateTime] = Field[DateTime](name)
  def asSeq[T](name: String): Field[Seq[T]] = Field[Seq[T]](name)


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
