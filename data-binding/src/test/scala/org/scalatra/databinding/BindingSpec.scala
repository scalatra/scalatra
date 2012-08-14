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
import com.fasterxml.jackson.databind.{ObjectMapper, JsonNode}
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.node.TextNode

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
      testBinding[Float]((random * 100).toFloat)
    }

    "provide Binding[Double]" in {
      testBinding[Double]((random * 100))
    }

    "provide Binding[Int]" in {
      testBinding[Int]((random * 100).toInt)
    }

    "provide Binding[Byte]" in {
      testBinding[Byte]((random * 100).toByte)
    }

    "provide Binding[Short]" in {
      testBinding[Short]((random * 100).toShort)
    }

    "provide Binding[Long]" in {
      testBinding[Long]((random * 100).toLong)
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

  "JacksonBindingImplicits" should {

    import JacksonBindingImplicits._
    "provide Binding[Boolean]" in {
      testJacksonBinding[Boolean](true)
    }

    "provide Binding[Float]" in {
      testJacksonBinding[Float]((random * 100).toFloat)
    }

    "provide Binding[Double]" in {
      testJacksonBinding[Double](random * 100)
    }

    "provide Binding[Int]" in {
      testJacksonBinding[Int]((random * 100).toInt)
    }

    "provide Binding[Byte]" in {
      testJacksonBinding[Byte]((random * 100).toByte)
    }

    "provide Binding[Short]" in {
      testJacksonBinding[Short]((random * 100).toShort)
    }

    "provide Binding[Long]" in {
      testJacksonBinding[Long]((random * 100).toLong)
    }

    "provide Binding[DateTime] for a ISO8601 date" in {
      testJacksonDateTimeBinding(JodaDateFormats.Iso8601)
    }

    "provide Binding[DateTime] for a ISO8601 date without millis" in {
      testJacksonDateTimeBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide Binding[DateTime] for a HTTP date" in {
      testJacksonDateTimeBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

    "provide Binding[Date] for a ISO8601 date" in {
      testJacksonDateBinding(JodaDateFormats.Iso8601)
    }

    "provide Binding[Date] for a ISO8601 date without millis" in {
      testJacksonDateBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide Binding[Date] for a HTTP date" in {
      testJacksonDateBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

  }

  "Defining validations" should {
    import BindingImplicits._
    "have a validation for notBlank" in {
      val field = newBinding[String].notBlank
      field.validators must not(beEmpty)
      field.validators.head.apply(Some("hello")).isSuccess must beTrue
      field.validators.head.apply(Some("")).isSuccess must beFalse
      field.validators.head.apply(None).isSuccess must beFalse
    }

    "have a validation for greater than" in {
      val field = newBinding[Int].greaterThan(6)
      field.validators must not(beEmpty)
      field.validators.head.apply(Some(7)).isSuccess must beTrue
      field.validators.head.apply(Some(6)).isSuccess must beFalse
      field.validators.head.apply(Some(1)).isSuccess must beFalse
    }

    "have a validation for non empty collection" in {
      val field = newBinding[Seq[String]].notEmpty
      field.validators must not(beEmpty)
      field.validators.head.apply(Some(Seq("hello"))).isSuccess must beTrue
      field.validators.head.apply(Some(Seq.empty[String])).isSuccess must beFalse
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

  val jsonMapper = new ObjectMapper()
  jsonMapper.registerModule(DefaultScalaModule)
  def testJacksonBinding[T](value: => T)(implicit mf: Manifest[T], converter: TypeConverter[JsonNode, T]) = {
    val field = newBinding[T]
    field.value must beNone
    val v = value

    field(Some(jsonMapper.readValue(jsonMapper.writeValueAsString(v), classOf[JsonNode]))).value must beSome(v)
  }

  def testJacksonDateTimeBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[DateTime], converter: TypeConverter[JsonNode, DateTime]) = {
    val field = newBinding[DateTime]
    field.value must beNone
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Some(new TextNode(s).asInstanceOf[JsonNode])).value must beSome(v)
  }

  def testJacksonDateBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[Date], converter: TypeConverter[JsonNode, Date]) = {
    val field = newBinding[Date]
    field.value must beNone
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Some(new TextNode(s).asInstanceOf[JsonNode])).value must beSome(v.toDate)
  }

  def newBinding[T:Manifest]: Binding[T] = Binding[T](randomFieldName)

  def randomFieldName = "field_" + random
}