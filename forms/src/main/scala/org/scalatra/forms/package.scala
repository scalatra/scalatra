package org.scalatra

import org.scalatra.i18n.Messages
import scala.reflect.ClassTag

package object forms {

  private[forms] val RequestAttributeParamsKey = "org.scalatra.forms.params"
  private[forms] val RequestAttributeErrorsKey = "org.scalatra.forms.errors"

  private[forms] def getSingleParam(params: Map[String, Seq[String]], name: String): Option[String] = {
    params.getOrElse(name, Nil).headOption
  }

  /** ValueType for the String property.
    */
  def text(constraints: Constraint*): SingleValueType[String] = new SingleValueType[String](constraints*) {
    def convert(value: String, messages: Messages): String = value
  }

  /** ValueType for the Boolean property.
    */
  def boolean(constraints: Constraint*): SingleValueType[Boolean] = new SingleValueType[Boolean](constraints*) {
    def convert(value: String, messages: Messages): Boolean = value match {
      case null | "" | "false" | "FALSE" => false
      case _                             => true
    }
  }

  /** ValueType for the Int property.
    */
  def number(constraints: Constraint*): SingleValueType[Int] = new SingleValueType[Int](constraints*) {

    def convert(value: String, messages: Messages): Int = value match {
      case null | "" => 0
      case x         => x.toInt
    }

    override def validate(
        name: String,
        value: String,
        params: Map[String, Seq[String]],
        messages: Messages
    ): Seq[(String, String)] = {
      super.validate(name, value, params, messages) match {
        case Nil =>
          try {
            value.toInt
            Nil
          } catch {
            case e: NumberFormatException => Seq(name -> messages("error.number").format(name))
          }
        case errors => errors
      }
    }
  }

  /** ValueType for the Double property.
    */
  def double(constraints: Constraint*): SingleValueType[Double] = new SingleValueType[Double](constraints*) {

    def convert(value: String, messages: Messages): Double = value match {
      case null | "" => 0d
      case x         => x.toDouble
    }

    override def validate(
        name: String,
        value: String,
        params: Map[String, Seq[String]],
        messages: Messages
    ): Seq[(String, String)] = {
      super.validate(name, value, params, messages) match {
        case Nil =>
          try {
            value.toDouble
            Nil
          } catch {
            case e: NumberFormatException => Seq(name -> messages("error.double").format(name))
          }
        case errors => errors
      }
    }
  }

  /** ValueType for the Long property.
    */
  def long(constraints: Constraint*): SingleValueType[Long] = new SingleValueType[Long](constraints*) {

    def convert(value: String, messages: Messages): Long = value match {
      case null | "" => 0L
      case x         => x.toLong
    }

    override def validate(
        name: String,
        value: String,
        params: Map[String, Seq[String]],
        messages: Messages
    ): Seq[(String, String)] = {
      super.validate(name, value, params, messages) match {
        case Nil =>
          try {
            value.toLong
            Nil
          } catch {
            case e: NumberFormatException => Seq(name -> messages("error.long").format(name))
          }
        case errors => errors
      }
    }
  }

  /** ValueType for the java.util.Date property.
    */
  def date(pattern: String, constraints: Constraint*): SingleValueType[java.util.Date] =
    new SingleValueType[java.util.Date](((datePattern(pattern) +: constraints))*) {
      def convert(value: String, messages: Messages): java.util.Date = value match {
        case null | "" => null
        case value     => new java.text.SimpleDateFormat(pattern).parse(value)
      }
    }

  /** ValueType for the dummy property.
    */
  def dummy[T](implicit c: ClassTag[T]): SingleValueType[T] = new SingleValueType[T] {
    def convert(value: String, messages: org.scalatra.i18n.Messages): T = {
      val clazz = c.runtimeClass
      val value = if (clazz == classOf[Int] || clazz == classOf[Short] || clazz == classOf[Char]) {
        0
      } else if (clazz == classOf[Long]) {
        0L
      } else if (clazz == classOf[Double]) {
        0d
      } else if (clazz == classOf[Float]) {
        0f
      } else if (clazz == classOf[Boolean]) {
        false
      } else {
        null
      }
      value.asInstanceOf[T]
    }
  }

  def mapping[T, P1](f1: (String, ValueType[P1]))(factory: (P1) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages)
    )
  }

  def mapping[T, P1, P2](f1: (String, ValueType[P1]), f2: (String, ValueType[P2]))(
      factory: (P1, P2) => T
  ): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) =
      factory(p(f1, name, params, messages), p(f2, name, params, messages))
  }

  def mapping[T, P1, P2, P3](f1: (String, ValueType[P1]), f2: (String, ValueType[P2]), f3: (String, ValueType[P3]))(
      factory: (P1, P2, P3) => T
  ): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) =
      factory(p(f1, name, params, messages), p(f2, name, params, messages), p(f3, name, params, messages))
  }

  def mapping[T, P1, P2, P3, P4](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4])
  )(factory: (P1, P2, P3, P4) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5])
  )(factory: (P1, P2, P3, P4, P5) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6])
  )(factory: (P1, P2, P3, P4, P5, P6) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7])
  )(factory: (P1, P2, P3, P4, P5, P6, P7) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7, f8)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages),
      p(f10, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages),
      p(f10, name, params, messages),
      p(f11, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12) => T): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages),
      p(f10, name, params, messages),
      p(f11, name, params, messages),
      p(f12, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13) => T): MappingValueType[T] =
    new MappingValueType[T] {
      def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13)
      def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
        p(f1, name, params, messages),
        p(f2, name, params, messages),
        p(f3, name, params, messages),
        p(f4, name, params, messages),
        p(f5, name, params, messages),
        p(f6, name, params, messages),
        p(f7, name, params, messages),
        p(f8, name, params, messages),
        p(f9, name, params, messages),
        p(f10, name, params, messages),
        p(f11, name, params, messages),
        p(f12, name, params, messages),
        p(f13, name, params, messages)
      )
    }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14) => T): MappingValueType[T] =
    new MappingValueType[T] {
      def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14)
      def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
        p(f1, name, params, messages),
        p(f2, name, params, messages),
        p(f3, name, params, messages),
        p(f4, name, params, messages),
        p(f5, name, params, messages),
        p(f6, name, params, messages),
        p(f7, name, params, messages),
        p(f8, name, params, messages),
        p(f9, name, params, messages),
        p(f10, name, params, messages),
        p(f11, name, params, messages),
        p(f12, name, params, messages),
        p(f13, name, params, messages),
        p(f14, name, params, messages)
      )
    }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14]),
      f15: (String, ValueType[P15])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15) => T): MappingValueType[T] =
    new MappingValueType[T] {
      def fields: Seq[(String, ValueType[?])] = Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15)
      def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
        p(f1, name, params, messages),
        p(f2, name, params, messages),
        p(f3, name, params, messages),
        p(f4, name, params, messages),
        p(f5, name, params, messages),
        p(f6, name, params, messages),
        p(f7, name, params, messages),
        p(f8, name, params, messages),
        p(f9, name, params, messages),
        p(f10, name, params, messages),
        p(f11, name, params, messages),
        p(f12, name, params, messages),
        p(f13, name, params, messages),
        p(f14, name, params, messages),
        p(f15, name, params, messages)
      )
    }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14]),
      f15: (String, ValueType[P15]),
      f16: (String, ValueType[P16])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16) => T): MappingValueType[T] =
    new MappingValueType[T] {
      def fields: Seq[(String, ValueType[?])] =
        Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16)
      def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
        p(f1, name, params, messages),
        p(f2, name, params, messages),
        p(f3, name, params, messages),
        p(f4, name, params, messages),
        p(f5, name, params, messages),
        p(f6, name, params, messages),
        p(f7, name, params, messages),
        p(f8, name, params, messages),
        p(f9, name, params, messages),
        p(f10, name, params, messages),
        p(f11, name, params, messages),
        p(f12, name, params, messages),
        p(f13, name, params, messages),
        p(f14, name, params, messages),
        p(f15, name, params, messages),
        p(f16, name, params, messages)
      )
    }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14]),
      f15: (String, ValueType[P15]),
      f16: (String, ValueType[P16]),
      f17: (String, ValueType[P17])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17) => T): MappingValueType[T] =
    new MappingValueType[T] {
      def fields: Seq[(String, ValueType[?])] =
        Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17)
      def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
        p(f1, name, params, messages),
        p(f2, name, params, messages),
        p(f3, name, params, messages),
        p(f4, name, params, messages),
        p(f5, name, params, messages),
        p(f6, name, params, messages),
        p(f7, name, params, messages),
        p(f8, name, params, messages),
        p(f9, name, params, messages),
        p(f10, name, params, messages),
        p(f11, name, params, messages),
        p(f12, name, params, messages),
        p(f13, name, params, messages),
        p(f14, name, params, messages),
        p(f15, name, params, messages),
        p(f16, name, params, messages),
        p(f17, name, params, messages)
      )
    }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14]),
      f15: (String, ValueType[P15]),
      f16: (String, ValueType[P16]),
      f17: (String, ValueType[P17]),
      f18: (String, ValueType[P18])
  )(
      factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18) => T
  ): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] =
      Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages),
      p(f10, name, params, messages),
      p(f11, name, params, messages),
      p(f12, name, params, messages),
      p(f13, name, params, messages),
      p(f14, name, params, messages),
      p(f15, name, params, messages),
      p(f16, name, params, messages),
      p(f17, name, params, messages),
      p(f18, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14]),
      f15: (String, ValueType[P15]),
      f16: (String, ValueType[P16]),
      f17: (String, ValueType[P17]),
      f18: (String, ValueType[P18]),
      f19: (String, ValueType[P19])
  )(
      factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19) => T
  ): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] =
      Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages),
      p(f10, name, params, messages),
      p(f11, name, params, messages),
      p(f12, name, params, messages),
      p(f13, name, params, messages),
      p(f14, name, params, messages),
      p(f15, name, params, messages),
      p(f16, name, params, messages),
      p(f17, name, params, messages),
      p(f18, name, params, messages),
      p(f19, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14]),
      f15: (String, ValueType[P15]),
      f16: (String, ValueType[P16]),
      f17: (String, ValueType[P17]),
      f18: (String, ValueType[P18]),
      f19: (String, ValueType[P19]),
      f20: (String, ValueType[P20])
  )(
      factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20) => T
  ): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] =
      Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages),
      p(f10, name, params, messages),
      p(f11, name, params, messages),
      p(f12, name, params, messages),
      p(f13, name, params, messages),
      p(f14, name, params, messages),
      p(f15, name, params, messages),
      p(f16, name, params, messages),
      p(f17, name, params, messages),
      p(f18, name, params, messages),
      p(f19, name, params, messages),
      p(f20, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14]),
      f15: (String, ValueType[P15]),
      f16: (String, ValueType[P16]),
      f17: (String, ValueType[P17]),
      f18: (String, ValueType[P18]),
      f19: (String, ValueType[P19]),
      f20: (String, ValueType[P20]),
      f21: (String, ValueType[P21])
  )(
      factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21) => T
  ): MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] =
      Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages),
      p(f10, name, params, messages),
      p(f11, name, params, messages),
      p(f12, name, params, messages),
      p(f13, name, params, messages),
      p(f14, name, params, messages),
      p(f15, name, params, messages),
      p(f16, name, params, messages),
      p(f17, name, params, messages),
      p(f18, name, params, messages),
      p(f19, name, params, messages),
      p(f20, name, params, messages),
      p(f21, name, params, messages)
    )
  }

  def mapping[T, P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22](
      f1: (String, ValueType[P1]),
      f2: (String, ValueType[P2]),
      f3: (String, ValueType[P3]),
      f4: (String, ValueType[P4]),
      f5: (String, ValueType[P5]),
      f6: (String, ValueType[P6]),
      f7: (String, ValueType[P7]),
      f8: (String, ValueType[P8]),
      f9: (String, ValueType[P9]),
      f10: (String, ValueType[P10]),
      f11: (String, ValueType[P11]),
      f12: (String, ValueType[P12]),
      f13: (String, ValueType[P13]),
      f14: (String, ValueType[P14]),
      f15: (String, ValueType[P15]),
      f16: (String, ValueType[P16]),
      f17: (String, ValueType[P17]),
      f18: (String, ValueType[P18]),
      f19: (String, ValueType[P19]),
      f20: (String, ValueType[P20]),
      f21: (String, ValueType[P21]),
      f22: (String, ValueType[P22])
  )(factory: (P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18, P19, P20, P21, P22) => T)
      : MappingValueType[T] = new MappingValueType[T] {
    def fields: Seq[(String, ValueType[?])] =
      Seq(f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13, f14, f15, f16, f17, f18, f19, f20, f21, f22)
    def convert(name: String, params: Map[String, Seq[String]], messages: Messages) = factory(
      p(f1, name, params, messages),
      p(f2, name, params, messages),
      p(f3, name, params, messages),
      p(f4, name, params, messages),
      p(f5, name, params, messages),
      p(f6, name, params, messages),
      p(f7, name, params, messages),
      p(f8, name, params, messages),
      p(f9, name, params, messages),
      p(f10, name, params, messages),
      p(f11, name, params, messages),
      p(f12, name, params, messages),
      p(f13, name, params, messages),
      p(f14, name, params, messages),
      p(f15, name, params, messages),
      p(f16, name, params, messages),
      p(f17, name, params, messages),
      p(f18, name, params, messages),
      p(f19, name, params, messages),
      p(f20, name, params, messages),
      p(f21, name, params, messages),
      p(f22, name, params, messages)
    )
  }

  private def p[T](
      field: (String, ValueType[T]),
      name: String,
      params: Map[String, Seq[String]],
      messages: Messages
  ): T =
    field match {
      case (fieldName, valueType) =>
        valueType.convert(if (name.isEmpty) fieldName else name + "." + fieldName, params, messages)
    }

  /////////////////////////////////////////////////////////////////////////////////////////////
  // ValueType wrappers to provide additional features.

  private val IndexedSingleParamPattern = "(.+?)\\[([0-9]+)\\]".r
  private val IndexedMapParamPattern    = "(.+?)\\[([0-9]+)\\]\\[(.+?)\\]".r

  /** ValueType for the List property. Parameter name must be "name[index]" or "name[index][subName]".
    */
  def list[T](valueType: ValueType[T]): ValueType[List[T]] = new ValueType[List[T]]() {

    private def extractSingleParams(name: String, params: Map[String, Seq[String]]): List[(Int, String)] = {
      params.toList
        .flatMap { case (key, values) =>
          key match {
            case x @ IndexedSingleParamPattern(_, i) if (x.startsWith(name + "[")) =>
              Seq((i.toInt, getSingleParam(params, key).get))
            case x if x == name => values.zipWithIndex.map { case (x, i) => (i, x) }
            case _              => Nil
          }
        }
        .sortBy(_._1)
    }

    private def extractMapParams(params: Map[String, Seq[String]]): Map[Int, Map[String, Seq[String]]] = {
      params
        .flatMap {
          case (key, value :: _) =>
            key match {
              case IndexedMapParamPattern(_, i, s) => Some((i.toInt, s.replaceAll("\\]\\[", "."), value))
              case _                               => None
            }
          case _ => None
        }
        .groupBy { case (i, key, value) => i }
        .map { case (i, values) =>
          (i -> values.map { case (i, key, value) =>
            key -> Seq(value)
          }.toMap)
        }
    }

    def convert(name: String, params: Map[String, Seq[String]], messages: Messages): List[T] = {
      valueType match {
        case singleValueType: SingleValueType[?] => {
          extractSingleParams(name, params).map { case (i, value) =>
            singleValueType.convert(value, messages)
          }
        }
        case mappingValueType => {
          val listParams = extractMapParams(params)
          val max        = if (listParams.isEmpty) -1 else listParams.keys.max
          (for (i <- 0 to max) yield {
            val rowParams = listParams.getOrElse(i, Map.empty[String, Seq[String]])
            mappingValueType.convert("", rowParams, messages)
          }).toList
        }
      }
    }

    def validate(name: String, params: Map[String, Seq[String]], messages: Messages): Seq[(String, String)] = {
      valueType match {
        case singleValueType: SingleValueType[?] => {
          extractSingleParams(name, params).flatMap { case (i, value) =>
            singleValueType.validate(name, value, params, messages).map { case (key, message) =>
              (key + "_" + i, message)
            }
          }
        }
        case mappingValueType => {
          val listParams = extractMapParams(params)
          val max        = if (listParams.isEmpty) -1 else listParams.keys.max
          (for (i <- 0 to max) yield {
            val rowParams = listParams.getOrElse(i, Map.empty[String, Seq[String]])
            mappingValueType.validate("", rowParams, messages).map { case (key, message) =>
              (key + "_" + i, message)
            }
          }).flatten
        }
      }
    }
  }

  /** ValueType wrapper for the optional property.
    */
  def optional[T](valueType: SingleValueType[T]): SingleValueType[Option[T]] = new SingleValueType[Option[T]]() {
    def convert(value: String, messages: Messages): Option[T] =
      if (value == null || value.isEmpty) None else Some(valueType.convert(value, messages))

    override def validate(
        name: String,
        value: String,
        params: Map[String, Seq[String]],
        messages: Messages
    ): Seq[(String, String)] =
      if (value == null || value.isEmpty) Nil else valueType.validate(name, value, params, messages)
  }

  /** ValueType wrapper for the optional mapping property.
    */
  def optional[T](
      condition: (Map[String, Seq[String]]) => Boolean,
      valueType: MappingValueType[T]
  ): ValueType[Option[T]] = new ValueType[Option[T]]() {
    override def convert(name: String, params: Map[String, Seq[String]], messages: Messages): Option[T] =
      if (condition(params)) Some(valueType.convert(name, params, messages)) else None

    override def validate(name: String, params: Map[String, Seq[String]], messages: Messages): Seq[(String, String)] =
      if (condition(params)) valueType.validate(name, params, messages) else Nil
  }

  /** ValueType wrapper for the optional property which is available if checkbox is checked.
    */
  def optionalIfNotChecked[T](checkboxName: String, valueType: MappingValueType[T]): ValueType[Option[T]] =
    optional({ params => boolean().convert(checkboxName, params, null) }, valueType)

  /** ValueType wrapper for the optional property which is required if condition is true.
    */
  def optionalRequired[T](
      condition: (Map[String, Seq[String]]) => Boolean,
      valueType: SingleValueType[T]
  ): SingleValueType[Option[T]] = new SingleValueType[Option[T]]() {
    def convert(value: String, messages: Messages): Option[T] =
      if (value == null || value.isEmpty) None else Some(valueType.convert(value, messages))

    override def validate(
        name: String,
        value: String,
        params: Map[String, Seq[String]],
        messages: Messages
    ): Seq[(String, String)] =
      required.validate(name, value, messages) match {
        case Some(error) if (condition(params)) => Seq(name -> error)
        case Some(error)                        => Nil
        case None                               => valueType.validate(name, value, params, messages)
      }
  }

  /** ValueType wrapper for the optional property which is required if checkbox is checked.
    */
  def optionalRequiredIfChecked[T](checkboxName: String, valueType: SingleValueType[T]): SingleValueType[Option[T]] =
    optionalRequired(
      params =>
        getSingleParam(params, checkboxName).orNull match {
          case null | "false" | "FALSE" => false
          case _                        => true
        },
      valueType
    )

  /** ValueType wrapper to trim a parameter.
    *
    * {{{
    * val form = mapping(
    *   "name" -> trim(text(required)),
    *   "mail" -> trim(text(required))
    * )
    * }}}
    */
  def trim[T](valueType: SingleValueType[T]): SingleValueType[T] = new SingleValueType[T]() {

    def convert(value: String, messages: Messages): T = valueType.convert(trim(value), messages)

    override def validate(
        name: String,
        value: String,
        params: Map[String, Seq[String]],
        messages: Messages
    ): Seq[(String, String)] =
      valueType.validate(name, trim(value), params, messages)

    private def trim(value: String): String = if (value == null) null else value.trim
  }

  /** ValueType wrapper to specified a property name which is used in the error message.
    *
    * {{{
    * val form = trim(mapping(
    *   "name" -> label("User name"   , text(required)),
    *   "mail" -> label("Mail address", text(required))
    * ))
    * }}}
    */
  def label[T](label: String, valueType: SingleValueType[T]): SingleValueType[T] = new SingleValueType[T]() {

    def convert(value: String, messages: Messages): T = valueType.convert(value, messages)

    override def validate(
        name: String,
        value: String,
        params: Map[String, Seq[String]],
        messages: Messages
    ): Seq[(String, String)] =
      valueType.validate(label, value, params, messages).map { case (label, message) => name -> message }

  }

  /////////////////////////////////////////////////////////////////////////////////////////////
  // Constraints

  def required: Constraint = new Constraint() {
    override def validate(name: String, value: String, messages: Messages): Option[String] = {
      if (value == null || value.isEmpty) Some(messages("error.required").format(name)) else None
    }
  }

  def required(message: String): Constraint = new Constraint() {
    override def validate(name: String, value: String, messages: Messages): Option[String] =
      if (value == null || value.isEmpty) Some(message.format(name)) else None
  }

  def maxlength(length: Int): Constraint = new Constraint() {
    override def validate(name: String, value: String, messages: Messages): Option[String] =
      if (value != null && value.length > length) Some(messages("error.maxlength").format(name, length)) else None
  }

  def minlength(length: Int): Constraint = new Constraint() {
    override def validate(name: String, value: String, messages: Messages): Option[String] =
      if (value != null && value.length < length) Some(messages("error.minlength").format(name, length)) else None
  }

  def length(length: Int): Constraint = new Constraint() {
    override def validate(name: String, value: String, messages: Messages): Option[String] =
      if (value != null && value.length != length) Some(messages("error.length").format(name, length)) else None
  }

  def oneOf(values: Seq[String], message: String = ""): Constraint = new Constraint() {
    override def validate(name: String, value: String, messages: Messages): Option[String] =
      if (value != null && !values.contains(value)) {
        Some(
          (if (message.isEmpty) messages("error.oneOf") else message)
            .format(name, values.map("'" + _ + "'").mkString(", "))
        )
      } else None
  }

  def pattern(pattern: String, message: String = ""): Constraint = new Constraint {
    override def validate(name: String, value: String, messages: Messages): Option[String] =
      if (value != null && !value.matches("^" + pattern + "$")) {
        Some((if (message.isEmpty) messages("error.pattern") else message).format(name, pattern))
      } else None
  }

  def datePattern(pattern: String, message: String = ""): Constraint = new Constraint {
    override def validate(name: String, value: String, messages: Messages): Option[String] =
      if (value != null && value.nonEmpty) {
        try {
          new java.text.SimpleDateFormat(pattern).parse(value)
          None
        } catch {
          case _: java.text.ParseException =>
            Some((if (message.isEmpty) messages("error.datePattern") else message).format(name, pattern))
        }
      } else None
  }

}
