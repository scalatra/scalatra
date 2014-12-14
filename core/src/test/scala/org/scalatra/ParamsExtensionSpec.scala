package org.scalatra

import org.specs2.mutable.Specification
import org.scalatra.util.{ MultiMap, MultiMapHeadView, MapWithIndifferentAccess }
import org.scalatra.util.conversion.TypeConverter
import java.util.Date
import java.text.SimpleDateFormat

class ParamsExtensionSpec extends Specification {

  import ScalatraParamsImplicits._

  case class FakeParams(params: Map[String, String]) extends MultiMapHeadView[String, String] with MapWithIndifferentAccess[String] {
    protected def multiMap = MultiMap(params.map(e => (e._1, List(e._2).toSeq)))
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

      params.getAs[Bogus]("a")((s: String) => Some(Bogus(s.toUpperCase))) must beSome(Bogus("BUFFYBUFFY"))

    }
  }

  "Scalatra 'MultiParams' pimping" should {

    "add a getAs[T] method" in {

      val multiParams: MultiMap = Map("CODES" -> List("1", "2").toSeq, "invalids" -> List("a", "b"))

      multiParams.getAs[Int]("CODES") must beSome[Seq[Int]]
      multiParams.getAs[Int]("CODES").get must containAllOf(List(1, 2)).inOrder
    }

    "return None for unexistent parameters" in {
      val multiParams: MultiMap = Map("invalids" -> List("1", "a", "2"))
      multiParams.getAs[Int]("blah") must beNone
    }

    "return Empty list if some conversion is invalid" in {
      val multiParams: MultiMap = Map("invalids" -> List("1", "a", "2"))
      multiParams.getAs[Int]("invalids") must_== Some(List(1, 2))
    }

    "return Empty list if all conversions are invalid" in {
      val multiParams: MultiMap = Map("invalids" -> List("a", "b"))
      multiParams.getAs[Int]("invalids") must_== Some(Nil)
    }

    "add a getAs[Date] method" in {

      val (format, datesAsText) = ("dd/MM/yyyy", List("20/12/2012", "10/02/2001"))

      val multiParams: MultiMap = Map("DATES" -> datesAsText.toSeq)

      val expectedDates = datesAsText.map {
        new SimpleDateFormat(format).parse(_)
      }

      multiParams.getAs[Date]("DATES" -> format) must beSome[Seq[Date]]
      multiParams.getAs[Date]("DATES" -> format).get must containAllOf(expectedDates).inOrder
    }
  }
}

