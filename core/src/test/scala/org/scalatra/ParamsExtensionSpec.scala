package org.scalatra

import java.text.SimpleDateFormat
import java.util.Date

import org.scalatra.util.conversion.TypeConverter
import org.scalatra.util.MultiMapHeadView
import org.specs2.mutable.Specification

class ParamsExtensionSpec extends Specification {

  import org.scalatra.ScalatraParamsImplicits.*

  case class FakeParams(params: Map[String, String]) extends MultiMapHeadView[String, String] {
    protected def multiMap = params.map(e => (e._1, Seq(e._2)))
  }

  "Scalatra 'Params pimping'" should {

    "add a getAs[T] method to Scalatra Params that returns Option[T]" in {

      val params: Params = FakeParams(Map("a" -> "1", "b" -> "", "c" -> null))

      params.getAs[Int]("a") must beSome(1)

      params.getAs[Int]("b") must beNone
      params.getAs[Int]("c") must beNone

      params.getAs[Int]("unexistent") must beNone
    }

    "add a getAs[Date] method to Scalatra Params that returns Option[Date]" in {

      val (format, dateAsText) = ("dd/MM/yyyy", "9/11/2001")

      val params: Params = FakeParams(Map("TwinTowers" -> dateAsText))

      val expectedDate = new SimpleDateFormat(format).parse(dateAsText)

      params.getAs[Date]("TwinTowers" -> format) must beSome(expectedDate)

    }

    "return None if a conversion is invalid" in {
      val params: Params = FakeParams(Map("a" -> "hello world"))
      params.getAs[Int]("a") must beNone
    }

    case class Bogus(name: String)

    "implicitly find TypeConverter(s) for a custom type" in {

      implicit val bogusConverter: TypeConverter[String, Bogus] = (s: String) => Some(Bogus(s))

      val params: Params = FakeParams(Map("a" -> "buffybuffy"))

      params.getAs[Bogus]("a") must beSome

      params.getAs[Bogus]("a").get aka "The bogus value" must_== Bogus("buffybuffy")

    }

    "explicitely receive a custom TypeConverter" in {

      val params: Params = FakeParams(Map("a" -> "buffybuffy"))

      params.getAs[Bogus]("a")(using ((s: String) => Some(Bogus(s.toUpperCase)))) must beSome(Bogus("BUFFYBUFFY"))

    }
  }

  "Scalatra 'MultiParams' pimping" should {

    "add a getAs[T] method" in {

      val multiParams: MultiParams = Map("CODES" -> Seq("1", "2"), "invalids" -> Seq("a", "b"))

      multiParams.getAs[Int]("CODES") must beSome[Seq[Int]]
      multiParams.getAs[Int]("CODES").get must containAllOf(Seq(1, 2)).inOrder
    }

    "return None for unexistent parameters" in {
      val multiParams: MultiParams = Map("invalids" -> Seq("1", "a", "2"))
      multiParams.getAs[Int]("blah") must beNone
    }

    "return Empty list if some conversion is invalid" in {
      val multiParams: MultiParams = Map("invalids" -> Seq("1", "a", "2"))
      multiParams.getAs[Int]("invalids") must_== Some(Seq(1, 2))
    }

    "return Empty list if all conversions are invalid" in {
      val multiParams: MultiParams = Map("invalids" -> Seq("a", "b"))
      multiParams.getAs[Int]("invalids") must_== Some(Nil)
    }

    "add a getAs[Date] method" in {

      val (format, datesAsText) = ("dd/MM/yyyy", Seq("20/12/2012", "10/02/2001"))

      val multiParams: MultiParams = Map("DATES" -> datesAsText)

      val expectedDates = datesAsText.map {
        new SimpleDateFormat(format).parse(_)
      }

      multiParams.getAs[Date]("DATES" -> format) must beSome[Seq[Date]]
      multiParams.getAs[Date]("DATES" -> format).get must containAllOf(expectedDates).inOrder
    }
  }
}
