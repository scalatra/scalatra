package org.scalatra
package util
package conversion

import java.util.{ Calendar, Date }

import org.specs2.mutable.Specification

class ConversionsSpecs extends Specification {

  "The TypeConverterSupport trait" should {

    object WithImplicit extends TypeConverterSupport

    "provide an exception-safe TypeConverter that return None in case of exceptions" in {

      import WithImplicit._

      val converter = safe[String, String]((s) => throw new Exception(s))

      converter("anything") must beNone
    }

    "implicitly convert a (String)=>T function into a TypeConveter[T]" in {

      import WithImplicit._

      case class A(i: Int)

      val stringToA = (s: String) => A(s.toInt)

      val converted: TypeConverter[String, A] = stringToA

      converted("10") must_== Some(A(10))
    }

    "implicitly convert a (String)=>Option[T] function into a TypeConveter[T]" in {

      case class A(i: Int)

      val stringToOptionA = new TypeConverter[String, A] {
        def apply(s: String) = Option(s).map(v => A(v.toInt))
      }

      val converted: TypeConverter[String, A] = stringToOptionA

      converted("15") must_== Some(A(15))
    }
  }

  "The DefaultImplicitConversions trait" should {

    object Impl extends DefaultImplicitConversions {

      def testFor[T](source: String, expected: Option[T])(implicit t: TypeConverter[String, T]) = {
        t(source) must_== expected
      }
    }

    "provide implicit VALs for basic types" in {
      import Impl._
      testFor("Hello", Some("Hello"))
      testFor("1.34d", Some(1.34d))
      testFor("1.23f", Some(1.23f))
      testFor("true", Some(true))
      testFor[Long]("12345678", Some(12345678L))
      testFor[Short]("1", Some(1.toShort))
      testFor("1234567890", Some(1234567890))
    }

    "provide DEF conversion for Date" in {

      val dateConverter = Impl.stringToDate("S") // milliseconds

      val cal = Calendar.getInstance
      val currentMs = cal.get(Calendar.MILLISECOND)
      val converted: Option[Date] = dateConverter(currentMs.toString)

      converted aka "The converted Date value" must beSome[Date]

      cal.setTime(converted.get)

      cal.get(Calendar.MILLISECOND) aka "The extracted milliseconds from converted Date" must_== currentMs
    }

    "provide DEF conversion for Seq" in {

      import Impl._

      def testConversion[T](args: (String, Seq[T]))(implicit mf: Manifest[T], t: TypeConverter[String, T]) = {
        val (source, expected) = args
        Impl.stringToSeq(t).apply(source).get must containAllOf(expected).inOrder
      }

      testConversion("1,2,3" -> List(1, 2, 3))
      testConversion("a,b,c,,e" -> List("a", "b", "c", "", "e"))

      case class B(v: Int)
      implicit val bConv: TypeConverter[String, B] = (s: String) => Some(B(s.toInt * 2))

      testConversion("1,2,3" -> List(B(2), B(4), B(6)))
    }
  }

  "The Conversions object" should {

    import org.scalatra.util.conversion.Conversions._

    "Pimp String type with as[T]" in {

      // Some value tests
      "2".as[Int] must beSome(2)
      "2.0".as[Int] must beNone
      "2.0".as[Double] must beSome(2.0)

      "false".as[Boolean] must beSome(false)
    }

    "Pimp String type with as[T] that delegates to implicit TypeConverter[T]" in {

      // A type and its type converter
      case class B(v: Int)
      implicit val bConv: TypeConverter[String, B] = (s: String) => Some(B(s.toInt * 2))

      "10".as[B] should beSome(B(20))
    }

    "Pimp String type with asDate implicit that require a format" in {
      "20120101".asDate("yyyyMMdd") must beSome[Date]
    }

    "Pimp String type with asSeq[T]" in {
      val b = "1,2,3,4,5".asSeq[Int](",")
      b must beSome[Seq[Int]]
      b.get must containAllOf(List(1, 2, 3, 4, 5))
    }

    "Pimp String type with asSeq[T] with separator" in {
      val b = "1 2 3 4 5".asSeq[Int](" ")
      b must beSome[Seq[Int]]
      b.get must containAllOf(List(1, 2, 3, 4, 5))
    }

    "Pimp String type with asSeq[T] with an implicit TypeConverter" in {
      case class C(s: String)
      implicit val cconv: TypeConverter[String, C] = (s: String) => Some(C(s))

      val b = "1,2,3".asSeq[C](",")
      b must beSome[Seq[C]]
      b.get must containAllOf(List(C("1"), C("2"), C("3")))
    }

  }
}