package org.scalatra
package databinding

import org.scalatra.util.conversion._
import org.scalatra.validation._

import scala.math._
import org.specs2.mutable.Specification
import java.util.Date
import java.text.{SimpleDateFormat, DateFormat}
import scalaz._
import Scalaz._
import Conversions._
import org.joda.time.{DateTimeZone, DateTime}

class BindingSpec extends Specification {

  "A BasicBinding" should {
    "have a name" in {
      Binding[String]("blah").name must_== "blah"
    }
    "begin the building process with a value of None" in {
      newBinding[String].value must beNone
    }
    "begin the building process with empty validators" in {
      newBinding[String].validators must beEmpty
    }
    "begin the building process unvalidatable" in {
      newBinding[String].canValidate must beFalse
    }
    "allow adding validators" in {
      val b = newBinding[String].validateWith(_ => { case s => s.getOrElse("").success[FieldError] })
      b.validators must not(beEmpty)
    }
    "bind to a string" in {
      val b = newBinding[String]
      b("Hey".some).value must beSome("Hey")
    }
  }

  "A BoundBinding" should {
    val binding = newBinding[String]

    "forward the binding name" in {
      val b = binding("blah".some)
      b must beAnInstanceOf[BoundBinding[String, String]]
      b.name must_== binding.name
    }

    "forward the validators" in {
      val b = binding("blah".some)
      b must beAnInstanceOf[BoundBinding[String, String]]
      b.validators must_== binding.validators
    }

    "have the bound value" in {
      val b = binding("blah".some)
      b.value must beSome("blah")
    }

    "allow adding validators" in {
      val b = binding("blah".some)
      val validator: Validator[String] = {case s => s.getOrElse("").success[FieldError]}
      b.validateWith(_ => validator).validators.size must_== (binding.validators.size + 1)
    }

    "indicate that validation is possible" in {
      binding("blah".some).canValidate must beTrue
    }
  }

  "BindingImplicits" should {

    import BindingImplicits._
    "provide Binding[Boolean]" in {
      testBinding[Boolean](true)
    }

    "provide Binding[Float]" in {
      testBinding[Float](random.toFloat)
    }

    "provide Binding[Double]" in {
      testBinding[Double](random)
    }

    "provide Binding[Int]" in {
      testBinding[Int](random.toInt)
    }

    "provide Binding[Byte]" in {
      testBinding[Byte](random.toByte)
    }

    "provide Binding[Short]" in {
      testBinding[Short](random.toShort)
    }

    "provide Binding[Long]" in {
      testBinding[Long](random.toLong)
    }

    "provide Binding[DateTime] for a ISO8601 date" in {
      testDateTimeBinding(JodaDateFormats.Iso8601)
    }

    "provide Binding[DateTime] for a ISO8601 date without millis" in {
      testDateTimeBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide Binding[DateTime] for a HTTP date" in {
      testDateTimeBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

    "provide Binding[Date] for a ISO8601 date" in {
      testDateBinding(JodaDateFormats.Iso8601)
    }

    "provide Binding[Date] for a ISO8601 date without millis" in {
      testDateBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide Binding[Date] for a HTTP date" in {
      testDateBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

  }

  def testDateTimeBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[DateTime], converter: TypeConverter[String, DateTime]) = {
    val field = newBinding[DateTime]
    field.value must beNone
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Some(s)).value must beSome(v)
  }
  def testDateBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[Date], converter: TypeConverter[String, Date]) = {
    val field = newBinding[Date]
    field.value must beNone
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Some(s)).value must beSome(v.toDate)
  }
  def testBinding[T](value: => T)(implicit mf: Manifest[T], converter: TypeConverter[String, T]) = {
    val field = newBinding[T]
    field.value must beNone
    val v = value
    field(Some(v.toString)).value must beSome(v)
  }
  def newBinding[T:Manifest]: Binding[T] = Binding[T](randomFieldName)

  def randomFieldName = "field_" + random
}