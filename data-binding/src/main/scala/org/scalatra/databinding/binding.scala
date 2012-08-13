package org.scalatra.databinding

import org.scalatra.util.conversion._
import java.text.DateFormat
import java.util.Date

trait Binding[T] {

  type ValueType = T


  def name: String

  def original: String

  def converted: Option[T]

  def apply(value: String)

  override def toString() = "Binding(name: %s, original: %s, converted: %s)".format(name, original, converted)

}

class BasicBinding[T](val name: String)(implicit val conversion: TypeConverter[T]) extends Binding[T] {

  var original = null.asInstanceOf[String]

  def converted = conversion(original)

  def apply(value: String) {
    original = value
  }

  override def hashCode() = 13 + 17 * name.hashCode()

  override def equals(obj: Any) = obj match {
    case b : BasicBinding[_] => b.name == this.name
    case _ => false
  }
}

object Binding {

  def apply[T](name: String)(implicit converter: TypeConverter[T]): Binding[T] = new BasicBinding[T](name)

}

/**
 * Commonly-used field implementations factory.
 *
 * @author mmazzarolo
 */
trait BindingImplicits extends DefaultImplicitConversions {

  private def blankStringConverter(blankAsNull: Boolean): TypeConverter[String] = (s: String) => Option(s) match {
    case x@Some(value: String) if (!blankAsNull || value.trim.nonEmpty) => x
    case _ => None
  }

  def asGeneric[T](name: String, f: (String) => T): Binding[T] = asImplicitGeneric(name)(f)

  def asImplicitGeneric[T](name: String)(implicit tc: TypeConverter[T]): Binding[T] = Binding[T](name)

  implicit def asType[T <: Any : TypeConverter](name: String): Binding[T] = Binding[T](name)

  implicit def asString(name: String, blankAsNull: Boolean = true): Binding[String] = Binding(name)(blankStringConverter(blankAsNull))

  implicit def asString(param: (String, Boolean)): Binding[String] = asString(param._1, param._2)

  def asDate(name: String, format: DateFormat = DateFormat.getInstance()): Binding[Date] = Binding(name)(stringToDateFormat(format))

  def asDate(name: String, format: String): Binding[Date] = Binding(name)(stringToDate(format))

  implicit def asDateWithStringFormat(param: (String, String)): Binding[Date] = asDate(param._1, param._2)

  implicit def asDateWithDateFormat(param: (String, DateFormat)): Binding[Date] = asDate(param._1, param._2)

}

object BindingImplicits extends BindingImplicits