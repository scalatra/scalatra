package org.scalatra
package commands

import java.util.Date

import org.joda.time.DateTime
import org.scalatra.util.conversion._
import org.scalatra.validation._

import scalaz._
import scalaz.syntax.std.option._

class BindingException(message: String) extends ScalatraException(message)

object Binding {

  def apply[I, A](fieldName: String, cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], mt: Manifest[A]): Binding = {
    new DefaultBinding(FieldDescriptor[A](fieldName), tcf)(mf, mt, cv)
  }

  def apply[I, A](prev: FieldDescriptor[A], cv: TypeConverter[I, A], tcf: TypeConverterFactory[_])(implicit mf: Manifest[I], mt: Manifest[A]): Binding = {
    new DefaultBinding(prev, tcf)(mf, mt, cv)
  }

  def apply[A](initial: String)(implicit ma: Manifest[A], tcFactory: TypeConverterFactory[A]): Binding = apply(FieldDescriptor[A](initial))
  def apply[A](initial: FieldDescriptor[A])(implicit ma: Manifest[A], tcFactory: TypeConverterFactory[A]): Binding = {
    new PartialBinding(initial)
  }

  private class PartialBinding[A](val field: FieldDescriptor[A])(implicit val valueManifest: Manifest[A], val typeConverterFactory: TypeConverterFactory[A]) extends Binding {
    type T = A
    type S = Nothing
    implicit def sourceManifest: Manifest[S] = null
    implicit def typeConverter: TypeConverter[S, T] = null
    def apply(toBind: Either[String, Option[S]]): Binding = null

    def validateWith(validators: BindingValidator[T]*): Binding =
      new PartialBinding(field.validateWith(validators: _*))

    def transform(transformer: (T) => T): Binding =
      new PartialBinding(field transform transformer)

    def validate: Binding = throw new BindingException("Databinding needs to happen before validation")
  }

  private class DefaultBinding[I, A](val field: FieldDescriptor[A], val typeConverterFactory: TypeConverterFactory[_])(
      implicit val sourceManifest: Manifest[I],
      val valueManifest: Manifest[A],
      val typeConverter: TypeConverter[I, A]) extends Binding {
    type T = A
    type S = I

    override def toString() = {
      "Binding[%s, %s](name: %s, value: %s, original: %s)".format(sourceManifest.erasure.getSimpleName, valueManifest.erasure.getSimpleName, name, validation, original)
    }

    def transform(transformer: (T) => T): Binding =
      new DefaultBinding(field.transform(transformer), typeConverterFactory)(sourceManifest, valueManifest, typeConverter)

    def validateWith(validators: BindingValidator[T]*): Binding =
      new DefaultBinding(field.validateWith(validators: _*), typeConverterFactory)(sourceManifest, valueManifest, typeConverter)

    def apply(toBind: Either[String, Option[S]]): Binding =
      new DefaultBinding(field(toBind), typeConverterFactory)(sourceManifest, valueManifest, typeConverter)

    def validate: Binding = {
      val nwFld = field.asInstanceOf[DataboundFieldDescriptor[S, T]].validate
      new DefaultBinding(nwFld, typeConverterFactory)(sourceManifest, valueManifest, typeConverter)
    }
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
  def error: Option[ValidationError] = field.value.fold(_.some, _ => None)

  def isValid = validation.isSuccess
  def isInvalid = validation.isFailure

  implicit def valueManifest: Manifest[T]

  def typeConverterFactory: TypeConverterFactory[_]

  type S

  implicit def sourceManifest: Manifest[S]

  def validateWith(validators: BindingValidator[T]*): Binding
  def transform(transformer: T => T): Binding

  def original: Option[S] = field match {
    case v: DataboundFieldDescriptor[_, _] => Some(v.original.asInstanceOf[S])
    case _ => None
  }

  implicit def typeConverter: TypeConverter[S, T]

  def validate: Binding

  def apply(toBind: Either[String, Option[S]]): Binding

  override def toString() =
    "BindingContainer[%s](name: %s, value: %s, original: %s)".format(valueManifest.erasure.getSimpleName, name, validation, original)

}

trait BindingSyntax extends BindingValidatorImplicits {

  implicit def asType[T: Manifest](name: String): FieldDescriptor[T] = FieldDescriptor[T](name)

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
  def asSeq[T: Manifest](name: String): FieldDescriptor[Seq[T]] = FieldDescriptor[Seq[T]](name)

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

}

object BindingImplicits extends BindingImplicits
