package org.scalatra
package databinding

import org.scalatra.util.conversion._

import scala.math._
import org.specs2.mutable.Specification
import java.util.Date
import java.text.{SimpleDateFormat, DateFormat}

class BindingSpec extends Specification {

  "BasicBinding class " should {

    case class BasicBindingTester[T](_name: String, parse:(String) => T) extends BasicBinding[T](_name)(parse.andThen((x:T) => Option(x)))

    "provide a apply() method that updates 'original'" in {

      val updater = new BasicBindingTester[String]("name", (s: String) => null)
      updater.original must beNull

      val newValue = System.currentTimeMillis().toString
      updater(newValue)

      updater.original must_== newValue
    }

    "delegate to TypeConverter conversions from String to specific type T" in {

      val stringToDate = new BasicBindingTester[Date]("date", (s: String) => new Date(s.toLong))
      val now = new Date

      stringToDate.original = now.getTime.toString

      stringToDate.converted must beSome(now)
    }

  }

  object ExistingBindings extends BindingImplicits

  "BindingImplicits" should {

    import ExistingBindings._

    "provide Binding[Boolean]" in {

      val field = testImplicitBindingType[Boolean]
      setAndCheckValue(field, true)
    }

    "provide Binding[Float]" in {
      val field = testImplicitBindingType[Float]
      val num = random.toFloat
      setAndCheckValue(field, num)
    }

    "provide Binding[Double]" in {
      val field = testImplicitBindingType[Double]
      val num = random.toDouble
      setAndCheckValue(field, num)
    }

    "provide Binding[Int]" in {
      val field = testImplicitBindingType[Int]
      val num = random.toInt
      setAndCheckValue(field, num)
    }

    "provide Binding[Byte]" in {
      val field = testImplicitBindingType[Byte]
      val num = random.toByte
      setAndCheckValue(field, num)
    }

    "provide Binding[Short]" in {
      val field = testImplicitBindingType[Short]
      val num = random.toShort
      setAndCheckValue(field, num)
    }

    "provide Binding[Long]" in {
      val field = testImplicitBindingType[Long]
      val num = random.toLong
      setAndCheckValue(field, num)
    }

    "provide Binding[String] that should treat blank strings an None" in {
      val field = newBinding[String](asString(_))
      field("   ")
      field.converted must beNone
    }

    "provide Binding[String] that should treat blank strings as Some() if required" in {
      val field = newBinding[String](asString(_, false))
      field("   ")
      field.converted must beSome[String]
      field.converted.get must_== "   "
    }

    "provide Binding[String] with an equivalent Tuple argument syntax" in {
      val field = newBinding[String]((s: String) => asString(s -> false))
      field("   ")
      field.converted must beSome[String]
      field.converted.get must_== "   "
    }

    "provide Binding[Date] with a default DateFormat" in {
      val field = newBinding[Date](asDate(_))
      val now = newDateWithFormattedString(dateFormatFor())
      field(now._2)
      field.converted must beSome[Date]
      field.converted.get must_== now._1
    }

    "provide Binding[Date] with a given date format" in {
      val format = "yyyyMMddHHmmsss"
      val field = newBinding[Date](asDate(_, format))
      val now = newDateWithFormattedString(dateFormatFor(format))
      field(now._2)
      field.converted must beSome[Date]
      field.converted.get must_== now._1
    }

    "provide Binding[Date] with an equivalent Tuple-based argument syntax" in {
      val format = "yyyyMMddHHmmsss"
      val field = newBinding[Date]((s: String) => ExistingBindings.asDateWithStringFormat(s -> format))
      val now = newDateWithFormattedString(dateFormatFor(format))
      field(now._2)
      field.converted must beSome[Date]
      field.converted.get must_== now._1
    }

  }

  def dateFormatFor(format: String = null): DateFormat = if (format == null) DateFormat.getInstance() else new SimpleDateFormat(format)

  def newDateWithFormattedString(format: DateFormat) = {
    val date = new Date
    (format.parse(format.format(date)) -> format.format(date))
  }


  def testImplicitBindingType[T: TypeConverter] = {

    import ExistingBindings._

    val fieldname = randomFieldName

    val field: Binding[T] = fieldname

    field.name must_== fieldname
    field must beAnInstanceOf[Binding[T]]

    field
  }

  def newBinding[T](f: (String) => Binding[T]): Binding[T] = f(randomFieldName)

  def setAndCheckValue[T](field: Binding[T], value: T) = {
    field.original must beNull
    field(value.toString)
    field.converted.get must_== value
  }

  def randomFieldName = "field_" + random
}