package org.scalatra
package commands

import org.scalatra.util.conversion._
import org.scalatra.validation._

import scala.math._
import org.specs2.mutable.Specification
import java.util.Date
import scalaz._
import Scalaz._
import Conversions._
import org.joda.time.{DateTimeZone, DateTime}
import org.json4s._
// import org.scalatra.commands.DefaultValues._
// import JsonZeroes._

class BindingSpec extends Specification {

  implicit val formats: Formats = DefaultFormats

  "A BasicFieldDescriptor" should {
    "have a name" in {
      FieldDescriptor[String]("blah").name must_== "blah"
    }
    "begin the building process with a required field validation error" in {
      newBinding[String].optional("").value must_== "".success[ValidationError]
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
      b(Right("Hey".some)).value must_== "Hey".success[ValidationError]
    }
  }

  "A Binding" should {
    import TypeConverterFactories._
    "construct containers by name" in {
      val cont = Binding("login", implicitly[TypeConverter[String, String]], implicitly[TypeConverterFactory[String]])
      cont.name must_== "login"
      cont.original must beAnInstanceOf[Option[String]]
    }

    "construct containers by binding" in {
      val binding = newBinding[String]
      val cont = Binding(binding, implicitly[TypeConverter[Seq[String], String]], implicitly[TypeConverterFactory[Seq[String]]])
      cont.name must_== binding.name
      cont.original must beAnInstanceOf[Option[Seq[String]]]
    }

    "bind to the data" in {
      val cont = Binding("login", implicitly[TypeConverter[String, String]], implicitly[TypeConverterFactory[String]])
      cont.name must_== "login"
      cont.original must beAnInstanceOf[Option[String]]
      val bound = cont(Right(Option("joske".asInstanceOf[cont.S])))
      bound.name must_== "login"
      bound.original must_== Some(Some("joske"))
      bound.validation must_== "joske".success
    }
    
  }
  
  "A BindingBuilder" should {
    import TypeConverterFactories._

    "start the build process by taking a FieldDescriptor[T]" in {
      import BindingSyntax._
      val b = Binding(asString("login"))
      b.field.name must_== "login"
      
    }
    
    "build a Binding with Map[String, String]" in {

      val builder = Binding(FieldDescriptor[String]("login"))
      val conv = implicitly[TypeConverter[String, String]].asInstanceOf[TypeConverter[String, builder.T]]
      val container = Binding(builder.field, conv, implicitly[TypeConverterFactory[String]])(manifest[String], builder.valueManifest)
      container(Right(Some("joske".asInstanceOf[container.S]))).validation must_== "joske".success
    }

  }

  "A BoundFieldDescriptor" should {
    val binding = newBinding[String]

    "forward the binding name" in {
      val b = binding(Right("blah".some))
      b must beAnInstanceOf[BoundFieldDescriptor[String, String]]
      b.name must_== binding.name
    }

    "forward the validators" in {
      val b = binding(Right("blah".some))
      b must beAnInstanceOf[BoundFieldDescriptor[String, String]]
      b.validator must_== binding.validator
    }

    "have the bound value" in {
      val b = binding(Right("blah".some))
      b.value must_== "blah".success[ValidationError]
    }

    "allow adding validators" in {
      val b = binding(Right("blah".some))
      b.validateWith(_ => identity).validator must beSome[Validator[String]]
    }
  }

  "BindingImplicits" should {

    import BindingImplicits._
    "provide FieldDescriptor[Boolean]" in {
      testBinding[Boolean](true)
    }

    "provide FieldDescriptor[Float]" in {
      testBinding[Float]((random * 100).toFloat)
    }

    "provide FieldDescriptor[Double]" in {
      testBinding[Double]((random * 100))
    }

    "provide FieldDescriptor[Int]" in {
      testBinding[Int]((random * 100).toInt)
    }

    "provide FieldDescriptor[Byte]" in {
      testBinding[Byte]((random * 100).toByte)
    }

    "provide FieldDescriptor[Short]" in {
      testBinding[Short]((random * 100).toShort)
    }

    "provide FieldDescriptor[Long]" in {
      testBinding[Long]((random * 100).toLong)
    }

    "provide FieldDescriptor[DateTime] for a ISO8601 date" in {
      testDateTimeBinding(JodaDateFormats.Iso8601)
    }

    "provide FieldDescriptor[DateTime] for a ISO8601 date without millis" in {
      testDateTimeBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide FieldDescriptor[DateTime] for a HTTP date" in {
      testDateTimeBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

    "provide FieldDescriptor[Date] for a ISO8601 date" in {
      testDateBinding(JodaDateFormats.Iso8601)
    }

    "provide FieldDescriptor[Date] for a ISO8601 date without millis" in {
      testDateBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide FieldDescriptor[Date] for a HTTP date" in {
      testDateBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

  }

  /*"JacksonBindingImplicits" should {

    import JacksonBindingImplicits._
    "provide FieldDescriptor[Boolean]" in {
      testJacksonBinding[Boolean](true)
    }

    "provide FieldDescriptor[Float]" in {
      testJacksonBinding[Float]((random * 100).toFloat)
    }

    "provide FieldDescriptor[Double]" in {
      testJacksonBinding[Double](random * 100)
    }

    "provide FieldDescriptor[Int]" in {
      testJacksonBinding[Int]((random * 100).toInt)
    }

    "provide FieldDescriptor[Byte]" in {
      testJacksonBinding[Byte]((random * 100).toByte)
    }

    "provide FieldDescriptor[Short]" in {
      testJacksonBinding[Short]((random * 100).toShort)
    }

    "provide FieldDescriptor[Long]" in {
      testJacksonBinding[Long]((random * 100).toLong)
    }

    "provide FieldDescriptor[DateTime] for a ISO8601 date" in {
      testJacksonDateTimeBinding(JodaDateFormats.Iso8601)
    }

    "provide FieldDescriptor[DateTime] for a ISO8601 date without millis" in {
      testJacksonDateTimeBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide FieldDescriptor[DateTime] for a HTTP date" in {
      testJacksonDateTimeBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

    "provide FieldDescriptor[Date] for a ISO8601 date" in {
      testJacksonDateBinding(JodaDateFormats.Iso8601)
    }

    "provide FieldDescriptor[Date] for a ISO8601 date without millis" in {
      testJacksonDateBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide FieldDescriptor[Date] for a HTTP date" in {
      testJacksonDateBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

  }*/

  "JsonBindingImplicits" should {
    val imports = new JsonTypeConverterFactoriesImports
    import imports._
    "provide FieldDescriptor[Boolean]" in {
      testLiftJsonBinding[Boolean](true)
    }

    "provide FieldDescriptor[Float]" in {
      testLiftJsonBinding[Float]((random * 100).toFloat)
    }

    "provide FieldDescriptor[Double]" in {
      testLiftJsonBinding[Double](random * 100)
    }

    "provide FieldDescriptor[Int]" in {
      testLiftJsonBinding[Int]((random * 100).toInt)
    }

    "provide FieldDescriptor[Byte]" in {
      testLiftJsonBinding[Byte]((random * 100).toByte)
    }

    "provide FieldDescriptor[Short]" in {
      testLiftJsonBinding[Short]((random * 100).toShort)
    }

    "provide FieldDescriptor[Long]" in {
      testLiftJsonBinding[Long]((random * 100).toLong)
    }

    "provide FieldDescriptor[DateTime] for a ISO8601 date" in {
      testLiftJsonDateTimeBinding(JodaDateFormats.Iso8601)
    }

    "provide FieldDescriptor[DateTime] for a ISO8601 date without millis" in {
      testLiftJsonDateTimeBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide FieldDescriptor[DateTime] for a HTTP date" in {
      testLiftJsonDateTimeBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

    "provide FieldDescriptor[Date] for a ISO8601 date" in {
      testLiftJsonDateBinding(JodaDateFormats.Iso8601)
    }

    "provide FieldDescriptor[Date] for a ISO8601 date without millis" in {
      testLiftJsonDateBinding(JodaDateFormats.Iso8601NoMillis, _.withMillis(0))
    }

    "provide FieldDescriptor[Date] for a HTTP date" in {
      testLiftJsonDateBinding(JodaDateFormats.HttpDate, _.withMillis(0))
    }

  }

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
      field.validator.get.apply(Success(Seq("hello"))).isSuccess must beTrue
      field.validator.get.apply(Success(Seq.empty[String])).isSuccess must beFalse
    }

    "allow chaining validations" in {
      val field = newBinding[String].notBlank.minLength(6)
      field.validator.get.apply("".success).isFailure must beTrue
      field.validator.get.apply("abc".success).isFailure must beTrue
      field.validator.get.apply("abcdef".success).isFailure must beFalse
    }
  }

  def testDateTimeBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[DateTime], converter: TypeConverter[String, DateTime]) = {
    val field = newBinding[DateTime](mf)
    field.value must_== ValidationError(field.requiredError.format(field.name), FieldName(field.name)).failure
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Right(Some(s))).value must_== v.success[ValidationError]
  }
  def testDateBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[Date], converter: TypeConverter[String, Date]) = {
    val field = newBinding[Date](mf)
    field.value must_== ValidationError(field.requiredError.format(field.name), FieldName(field.name)).failure
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Right(Some(s))).value must_== v.toDate.success[ValidationError]
  }
  def testBinding[T](value: => T)(implicit mf: Manifest[T], converter: TypeConverter[String, T]) = {
    val field = newBinding[T]
    field.value must_== ValidationError(field.requiredError.format(field.name), FieldName(field.name)).failure
    val v = value
    field(Right(Some(v.toString))).value must_== v.success[ValidationError]
  }

//  val jsonMapper = new ObjectMapper()
//  jsonMapper.registerModule(DefaultScalaModule)
//  def testJacksonBinding[T](value: => T)(implicit mf: Manifest[T], zt: Zero[T], converter: TypeConverter[JsonNode, T]) = {
//    val field = newBinding[T]
//    field.value must_== zt.zero.success
//    val v = value
//
//    field(Some(jsonMapper.readValue(jsonMapper.writeValueAsString(v), classOf[JsonNode]))).value must_== v.success
//  }
//
//  def testJacksonDateTimeBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[DateTime], zt: Zero[DateTime], converter: TypeConverter[JsonNode, DateTime]) = {
//    val field = newBinding[DateTime]
//    field.value must_== zt.zero.success
//    val v = transform(new DateTime(DateTimeZone.UTC))
//    val s = v.toString(format.dateTimeFormat)
//    field(Some(new TextNode(s).asInstanceOf[JsonNode])).value must_== v.success
//  }
//
//  def testJacksonDateBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[Date], zt: Zero[Date], converter: TypeConverter[JsonNode, Date]) = {
//    val field = newBinding[Date]
//    field.value must_== zt.zero.success
//    val v = transform(new DateTime(DateTimeZone.UTC))
//    val s = v.toString(format.dateTimeFormat)
//    field(Some(new TextNode(s).asInstanceOf[JsonNode])).value must_== v.toDate.success
//  }

  def testLiftJsonBinding[T](value: => T)(implicit mf: Manifest[T], converter: TypeConverter[JValue, T]) = {
    val field = newBinding[T]
    field.value must_== ValidationError(field.requiredError.format(field.name), FieldName(field.name)).failure
    val v = value

    field(Right(Some(Extraction.decompose(v)))).value must_== v.success
  }

  def testLiftJsonDateTimeBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[DateTime], converter: TypeConverter[JValue, DateTime]) = {
    val field = newBinding[DateTime](mf)
    field.value must_== ValidationError(field.requiredError.format(field.name), FieldName(field.name)).failure
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Right(Some(Extraction.decompose(s)))).value must_== v.success
  }

  def testLiftJsonDateBinding(format: JodaDateFormats.DateFormat, transform: DateTime => DateTime = identity)(implicit mf: Manifest[Date], converter: TypeConverter[JValue, Date]) = {
    val field = newBinding[Date](mf)
    field.value must_== ValidationError(field.requiredError.format(field.name), FieldName(field.name)).failure
    val v = transform(new DateTime(DateTimeZone.UTC))
    val s = v.toString(format.dateTimeFormat)
    field(Right(Some(Extraction.decompose(s)))).value must_== v.toDate.success
  }

  def newBinding[T:Manifest]: FieldDescriptor[T] = FieldDescriptor[T](randomFieldName)

  def randomFieldName = "field_" + random
}
