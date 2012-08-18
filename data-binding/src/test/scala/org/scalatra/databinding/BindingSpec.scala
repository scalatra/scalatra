package org.scalatra
package databinding

import org.scalatra.util.conversion._
import org.scalatra.validation._

import scala.math._
import org.specs2.mutable.Specification
import java.util.Date
import scalaz._
import Scalaz._
import Conversions._
import org.joda.time.{DateTimeZone, DateTime}
import com.fasterxml.jackson.databind.{ObjectMapper, JsonNode}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.node.TextNode
import net.liftweb.json._
import Imports._

class BindingSpec extends Specification {

  implicit val formats: Formats = DefaultFormats

  "A BasicField" should {
    "have a name" in {
      Field[String]("blah").name must_== "blah"
    }
    "begin the building process with a value of None" in {
      newBinding[String].value must_== "".success[ValidationError]
    }
    "begin the building process with empty validators" in {
      newBinding[String].validator must beEmpty
    }
    "allow adding validators" in {
      val b = newBinding[String].validateWith(_ => identity)
      b.validator must not(beEmpty)
    }
    "bind to a string" in {
      val b = newBinding[String]
      b("Hey".some).value must_== "Hey".success[ValidationError]
    }
  }

  "A Binding" should {
    import TypeConverterFactoryImplicits._
    "construct containers by name" in {
      val cont = Binding("login", implicitly[TypeConverter[String, String]], stringTypeConverterFactory)
      cont.name must_== "login"
      cont.original must beAnInstanceOf[Option[String]]
    }

    "construct containers by binding" in {
      val binding = newBinding[String]
      val cont = Binding(binding, implicitly[TypeConverter[Seq[String], String]], stringSeqTypeConverterFactory)
      cont.name must_== binding.name
      cont.original must beAnInstanceOf[Option[Seq[String]]]
    }

    "bind to the data" in {
      val cont = Binding("login", implicitly[TypeConverter[String, String]], stringTypeConverterFactory)
      cont.name must_== "login"
      cont.original must beAnInstanceOf[Option[String]]
      val bound = cont(Option("joske".asInstanceOf[cont.S]))
      bound.name must_== "login"
      bound.original must_== Some("joske")
      bound.value must_== "joske".success
    }
    
  }
  
  "A BindingBuilder" should {
    import TypeConverterFactoryImplicits._
    "start the build process by taking a Field[T]" in {
      import BindingSyntax._
      val b = Binding(asString("login"))
      b.field.name must_== "login"
      
    }
    
    "build a Binding with Map[String, String]" in {

      val builder = Binding(Field[String]("login"))
      val conv = implicitly[TypeConverter[String, String]].asInstanceOf[TypeConverter[String, builder.T]]
      val container = Binding(builder.field, conv, stringTypeConverterFactory)(manifest[String], implicitly[Zero[String]], builder.valueManifest, builder.valueZero)
      container(Some("joske".asInstanceOf[container.S])).value must_== "joske".success
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
      b.validator must_== binding.validator
    }

    "have the bound value" in {
      val b = binding("blah".some)
      b.value must_== "blah".success[ValidationError]
    }

    "allow adding validators" in {
      val b = binding("blah".some)
      b.validateWith(_ => identity).validator must beSome[Validator[String]]
    }
  }

  "BindingImplicits" should {

    import BindingImplicits._
    "provide Field[Boolean]" in {
      testBinding[Boolean](true)
    }

    "provide Field[Float]" in {
      testBinding[Float]((random * 100).toFloat)
    }

    "provide Field[Double]" in {
      testBinding[Double]((random * 100))
    }

    "provide Field[Int]" in {
      testBinding[Int]((random * 100).toInt)
    }

    "provide Field[Byte]" in {
      testBinding[Byte]((random * 100).toByte)
    }

    "provide Field[Short]" in {
      testBinding[Short]((random * 100).toShort)
    }

    "provide Field[Long]" in {
      testBinding[Long]((random * 100).toLong)
    }

    "provide Field[DateTime] for a ISO8601 date" in {
      testDateTimeBinding(JodaDateFormats.Iso8601)
    }

    "provide Field[DateTime] for a ISO8601 date without millis" in {
      testDateTimeBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide Field[DateTime] for a HTTP date" in {
      testDateTimeBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

    "provide Field[Date] for a ISO8601 date" in {
      testDateBinding(JodaDateFormats.Iso8601)
    }

    "provide Field[Date] for a ISO8601 date without millis" in {
      testDateBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide Field[Date] for a HTTP date" in {
      testDateBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

  }

//  "JacksonBindingImplicits" should {
//
//    import JacksonBindingImplicits._
//    "provide Field[Boolean]" in {
//      testJacksonBinding[Boolean](true)
//    }
//
//    "provide Field[Float]" in {
//      testJacksonBinding[Float]((random * 100).toFloat)
//    }
//
//    "provide Field[Double]" in {
//      testJacksonBinding[Double](random * 100)
//    }
//
//    "provide Field[Int]" in {
//      testJacksonBinding[Int]((random * 100).toInt)
//    }
//
//    "provide Field[Byte]" in {
//      testJacksonBinding[Byte]((random * 100).toByte)
//    }
//
//    "provide Field[Short]" in {
//      testJacksonBinding[Short]((random * 100).toShort)
//    }
//
//    "provide Field[Long]" in {
//      testJacksonBinding[Long]((random * 100).toLong)
//    }
//
//    "provide Field[DateTime] for a ISO8601 date" in {
//      testJacksonDateTimeBinding(JodaDateFormats.Iso8601)
//    }
//
//    "provide Field[DateTime] for a ISO8601 date without millis" in {
//      testJacksonDateTimeBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
//    }
//
//    "provide Field[DateTime] for a HTTP date" in {
//      testJacksonDateTimeBinding(JodaDateFormats.HttpDate, _.withMillis(0))
//    }
//
//    "provide Field[Date] for a ISO8601 date" in {
//      testJacksonDateBinding(JodaDateFormats.Iso8601)
//    }
//
//    "provide Field[Date] for a ISO8601 date without millis" in {
//      testJacksonDateBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
//    }
//
//    "provide Field[Date] for a HTTP date" in {
//      testJacksonDateBinding(JodaDateFormats.HttpDate, _.withMillis(0))
//    }
//
//  }
//
//  "LiftJsonBindingImplicits" should {
//
//    val imports = new LiftJsonBindingImports
//    import imports._
//    "provide Field[Boolean]" in {
//      testLiftJsonBinding[Boolean](true)
//    }
//
//    "provide Field[Float]" in {
//      testLiftJsonBinding[Float]((random * 100).toFloat)
//    }
//
//    "provide Field[Double]" in {
//      testLiftJsonBinding[Double](random * 100)
//    }
//
//    "provide Field[Int]" in {
//      testLiftJsonBinding[Int]((random * 100).toInt)
//    }
//
//    "provide Field[Byte]" in {
//      testLiftJsonBinding[Byte]((random * 100).toByte)
//    }
//
//    "provide Field[Short]" in {
//      testLiftJsonBinding[Short]((random * 100).toShort)
//    }
//
//    "provide Field[Long]" in {
//      testLiftJsonBinding[Long]((random * 100).toLong)
//    }
//
//    "provide Field[DateTime] for a ISO8601 date" in {
//      testLiftJsonDateTimeBinding(JodaDateFormats.Iso8601)
//    }
//
//    "provide Field[DateTime] for a ISO8601 date without millis" in {
//      testLiftJsonDateTimeBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
//    }
//
//    "provide Field[DateTime] for a HTTP date" in {
//      testLiftJsonDateTimeBinding(JodaDateFormats.HttpDate, _.withMillis(0))
//    }
//
//    "provide Field[Date] for a ISO8601 date" in {
//      testLiftJsonDateBinding(JodaDateFormats.Iso8601)
//    }
//
//    "provide Field[Date] for a ISO8601 date without millis" in {
//      testLiftJsonDateBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
//    }
//
//    "provide Field[Date] for a HTTP date" in {
//      testLiftJsonDateBinding(JodaDateFormats.HttpDate, _.withMillis(0))
//    }
//
//  }

  "Defining validations" should {
    import BindingImplicits._
    "have a validation for notBlank" in {
      val field = newBinding[String].notBlank
      field.validator must not(beEmpty)
      field.validator.get.apply("hello".success).isSuccess must beTrue
      field.validator.get.apply("".success).isSuccess must beFalse
      field.validator.get.apply(null.asInstanceOf[String].success).isSuccess must beFalse
    }

    "have a validation for greater than" in {
      val field = newBinding[Int].greaterThan(6)
      field.validator must not(beEmpty)
      field.validator.get.apply(7.success).isSuccess must beTrue
      field.validator.get.apply(6.success).isSuccess must beFalse
      field.validator.get.apply(1.success).isSuccess must beFalse
    }

    "have a validation for non empty collection" in {
      val field = newBinding[Seq[String]].notEmpty
      field.validator must not(beEmpty)
      field.validator.get.apply(Seq("hello").success).isSuccess must beTrue
      field.validator.get.apply(Seq.empty[String].success).isSuccess must beFalse
    }

    "allow chaining validations" in {
      val field = newBinding[String].notBlank.minLength(6)
      field.validator.get.apply("".success).isFailure must beTrue
      field.validator.get.apply("abc".success).isFailure must beTrue
      field.validator.get.apply("abcdef".success).isFailure must beFalse
    }
  }

  def testDateTimeBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[DateTime], converter: TypeConverter[String, DateTime]) = {
    val field = newBinding[DateTime]
    field.value must_== (new DateTime(0)).success[ValidationError]
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Some(s)).value must_== v.success[ValidationError]
  }
  def testDateBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[Date], converter: TypeConverter[String, Date]) = {
    val field = newBinding[Date]
    field.value must_== (new Date(0)).success[ValidationError]
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Some(s)).value must_== v.toDate.success[ValidationError]
  }
  def testBinding[T](value: => T)(implicit mf: Manifest[T], z: Zero[T], converter: TypeConverter[String, T]) = {
    val field = newBinding[T]
    field.value must_== z.zero.success[ValidationError]
    val v = value
    field(Some(v.toString)).value must_== v.success[ValidationError]
  }

//  val jsonMapper = new ObjectMapper()
//  jsonMapper.registerModule(DefaultScalaModule)
//  def testJacksonBinding[T](value: => T)(implicit mf: Manifest[T], converter: TypeConverter[JsonNode, T]) = {
//    val field = newBinding[T]
//    field.value must beNone
//    val v = value
//
//    field(Some(jsonMapper.readValue(jsonMapper.writeValueAsString(v), classOf[JsonNode]))).value must beSome(v)
//  }
//
//  def testJacksonDateTimeBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[DateTime], converter: TypeConverter[JsonNode, DateTime]) = {
//    val field = newBinding[DateTime]
//    field.value must beNone
//    val v = transform(new DateTime(DateTimeZone.UTC))
//    val s = v.toString(format.dateTimeFormat)
//    field(Some(new TextNode(s).asInstanceOf[JsonNode])).value must beSome(v)
//  }
//
//  def testJacksonDateBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[Date], converter: TypeConverter[JsonNode, Date]) = {
//    val field = newBinding[Date]
//    field.value must beNone
//    val v = transform(new DateTime(DateTimeZone.UTC))
//    val s = v.toString(format.dateTimeFormat)
//    field(Some(new TextNode(s).asInstanceOf[JsonNode])).value must beSome(v.toDate)
//  }
//
//
//  def testLiftJsonBinding[T](value: => T)(implicit mf: Manifest[T], converter: TypeConverter[JValue, T]) = {
//    val field = newBinding[T]
//    field.value must beNone
//    val v = value
//
//    field(Some(Extraction.decompose(v))).value must beSome(v)
//  }
//
//  def testLiftJsonDateTimeBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[DateTime], converter: TypeConverter[JValue, DateTime]) = {
//    val field = newBinding[DateTime]
//    field.value must beNone
//    val v = transform(new DateTime(DateTimeZone.UTC))
//    val s = v.toString(format.dateTimeFormat)
//    field(Some(Extraction.decompose(s))).value must beSome(v)
//  }
//
//  def testLiftJsonDateBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[Date], converter: TypeConverter[JValue, Date]) = {
//    val field = newBinding[Date]
//    field.value must beNone
//    val v = transform(new DateTime(DateTimeZone.UTC))
//    val s = v.toString(format.dateTimeFormat)
//    field(Some(Extraction.decompose(s))).value must beSome(v.toDate)
//  }

  def newBinding[T:Zero]: Field[T] = Field[T](randomFieldName)

  def randomFieldName = "field_" + random
}