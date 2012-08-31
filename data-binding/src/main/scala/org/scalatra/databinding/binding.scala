package org.scalatra
package databinding

import org.scalatra.util.conversion._
import validation._
import java.util.Date
import scalaz._
import Scalaz._
import org.joda.time.DateTime
import java.util.concurrent.atomic.AtomicReference
import DefaultZeroes._
import scala.util.matching.Regex

class BindingException(message: String) extends ScalatraException(message)


object Binding {



  def apply[I, A](fieldName: String, cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): Binding = {
    new DefaultBinding(FieldDescriptor[A](fieldName), tcf)(mf, z, mt, za, cv)
  }


  def apply[I, A](prev: FieldDescriptor[A], cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], z: Zero[I], mt: Manifest[A], za: Zero[A]): Binding = {
    new DefaultBinding(prev, tcf)(mf, z, mt, za, cv)
  }

  def apply[A](initial: String)(implicit ma: Manifest[A], za: Zero[A], tcFactory: TypeConverterFactory[A]): Binding = apply(FieldDescriptor[A](initial))
  def apply[A](initial: FieldDescriptor[A])(implicit ma: Manifest[A], za: Zero[A], tcFactory: TypeConverterFactory[A]): Binding = {
    new PartialBinding(initial)
  }

  private class PartialBinding[A](val field: FieldDescriptor[A])(implicit val valueManifest: Manifest[A],
                        val valueZero: Zero[A], val typeConverterFactory: TypeConverterFactory[A]) extends Binding {
    type T = A
    type S = Null
    implicit def sourceManifest: Manifest[S] = null
    implicit def sourceZero: Zero[S] = null
    implicit def typeConverter: (S) => Option[T] = null
    def apply(toBind: Either[String, Option[S]]): Binding = null

    def validateWith(validators:BindingValidator[T]*): Binding =
      new PartialBinding(field.validateWith(validators:_*))

    def transform(transformer: (T) => T): Binding =
      new PartialBinding(field transform transformer)
  }

  private class DefaultBinding[I, A]
                  (val field: FieldDescriptor[A], val typeConverterFactory: TypeConverterFactory[_])(
                      implicit
                      val sourceManifest: Manifest[I],
                      val sourceZero: Zero[I],
                      val valueManifest: Manifest[A],
                      val valueZero: Zero[A],
                      val typeConverter: TypeConverter[I, A]) extends Binding {
    type T = A
    type S = I

    override def toString() = {
      "Binding[%s, %s](name: %s, original: %s, value: %s)".format(sourceManifest.erasure.getSimpleName, valueManifest.erasure.getSimpleName, name, validation, original)
    }


    def transform(transformer: (T) => T): Binding =
      new DefaultBinding(field.transform(transformer), typeConverterFactory)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)

    def validateWith(validators:BindingValidator[T]*): Binding =
      new DefaultBinding(field.validateWith(validators:_*), typeConverterFactory)(sourceManifest, sourceZero, valueManifest, valueZero, typeConverter)

    def apply(toBind: Either[String, Option[S]]): Binding =
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

  def field: FieldDescriptor[T]

  def name: String = field.name
  def validation: FieldValidation[T] = field.value
  def value: Option[T] = field.value.toOption
  def error: Option[ValidationError] = field.value.fail.toOption

  def isValid = validation.isSuccess
  def isInvalid = validation.isFailure


  implicit def valueManifest: Manifest[T]
  implicit def valueZero: Zero[T]

  def typeConverterFactory: TypeConverterFactory[_]

  type S

  implicit def sourceManifest: Manifest[S]
  implicit def sourceZero: Zero[S]

  def validateWith(validators: BindingValidator[T]*): Binding
  def transform(transformer: T => T): Binding

  def original: Option[S] = field match {
    case v: ValidatableFieldDescriptor[_, _] => Some(v.original.asInstanceOf[S])
    case _ => None
  }


  implicit def typeConverter: TypeConverter[S, T]


  def apply(toBind: Either[String, Option[S]]): Binding

  override def toString() =
    "BindingContainer[%s](name: %s, value: %s, original: %s)".format(valueManifest.erasure.getSimpleName, name, validation, original)

}

trait BindingSyntax extends BindingValidatorImplicits {



  implicit def asType[T:Zero](name: String): FieldDescriptor[T] = FieldDescriptor[T](name)

  def asBoolean(name: String): FieldDescriptor[Boolean] = FieldDescriptor[Boolean](name)
  def asByte(name: String): FieldDescriptor[Byte] = FieldDescriptor[Byte](name)
  def asShort(name: String): FieldDescriptor[Short] = FieldDescriptor[Short](name)
  def asInt(name: String): FieldDescriptor[Int] = FieldDescriptor[Int](name)
  def asLong(name: String): FieldDescriptor[Long] = FieldDescriptor[Long](name)
  def asFloat(name: String): FieldDescriptor[Float] = FieldDescriptor[Float](name)
  def asDouble(name: String): FieldDescriptor[Double] = FieldDescriptor[Double](name)
  def asBigDecimal(name: String): FieldDescriptor[BigDecimal] = FieldDescriptor[BigDecimal](name)
  def asString(name: String): FieldDescriptor[String] = FieldDescriptor[String](name)
  def asDate(name: String): FieldDescriptor[Date] = FieldDescriptor[Date](name)
  def asDateTime(name: String): FieldDescriptor[DateTime] = FieldDescriptor[DateTime](name)
  def asSeq[T](name: String): FieldDescriptor[Seq[T]] = FieldDescriptor[Seq[T]](name)


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
