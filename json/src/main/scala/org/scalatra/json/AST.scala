package org.scalatra
package json

object AST {
  object JValue extends Merge.Mergeable
  sealed trait JValue extends Diff.Diffable {
    def value: Any

    def valueAs[A]: A = value.asInstanceOf[A]

    def \(fieldName: String): JValue = JNothing

    def apply(idx: Int): JValue = JNothing

    def \\(fieldName: String): Seq[JValue] = Nil

    /** Concatenate with another JSON.
     * This is a concatenation monoid: (JValue, ++, JNothing)
     * <p>
     * Example:<pre>
     * JArray(JInt(1) :: JInt(2) :: Nil) ++ JArray(JInt(3) :: Nil) ==
     * JArray(List(JInt(1), JInt(2), JInt(3)))
     * </pre>
     */
    def ++(other: JValue) = {
      def append(value1: JValue, value2: JValue): JValue = (value1, value2) match {
        case (JNothing, x) => x
        case (x, JNothing) => x
        case (JObject(xs), x: JField) => JObject(xs ::: List(x))
        case (x: JField, JObject(xs)) => JObject(x :: xs)
        case (JArray(xs), JArray(ys)) => JArray(xs ::: ys)
        case (JArray(xs), v: JValue) => JArray(xs ::: List(v))
        case (v: JValue, JArray(xs)) => JArray(v :: xs)
        case (f1: JField, f2: JField) => JObject(f1 :: f2 :: Nil)
        case (JField(n, v1), v2: JValue) => JField(n, append(v1, v2))
        case (x, y) => JArray(x :: y :: Nil)
      }
      append(this, other)
    }

    def merge(other: JValue) = Merge.merge(this, other)
  }

  case object JNull extends JValue {
    def value: Any = null
  }

  case class JBoolean(value: Boolean) extends JValue

  sealed trait JNumber extends JValue
  case class JInt(value: BigInt) extends JNumber

  case class JDouble(value: Double) extends JNumber

  case class JDecimal(value: BigDecimal) extends JNumber

  case class JString(value: String) extends JValue

  case object JNothing extends JValue {
    val value = None
  }

  case class JArray(elements: List[JValue]) extends JValue {
    def value = null

    override def apply(index: Int): JValue = {
      try {
        elements(index)
      } catch {
        case _ => JNothing
      }
    }
  }

  case class JField(name: String, value: JValue) extends JValue

  case class JObject(fields: List[JField]) extends JValue {
    def value = null

    override def \(fieldName: String): JValue = {
      fields collectFirst {
        case JField(`fieldName`, value) => value
      } getOrElse JNothing
    }

    override def \\(fieldName: String): Seq[JValue] = {
      fields.flatMap {
        case JField(name, value) if name == fieldName => Seq(value) ++ (value \\ fieldName)
        case JField(_, value) => value \\ fieldName
      }
    }
  }
}
