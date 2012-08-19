package org.scalatra

import validation.ValidationError
import com.fasterxml.jackson.databind.JsonNode
import scalaz._
import Scalaz._
import com.fasterxml.jackson.databind.node.MissingNode
import net.liftweb.json.JsonAST.{JValue, JNothing}
import java.util.Date
import org.joda.time.DateTime

package object databinding {

  type FieldValidation[T] = Validation[ValidationError, T]

  type Validator[T] = FieldValidation[T] => FieldValidation[T]

  type BindingValidator[T] = (String) => Validator[T]

  type BindingAction = () => Any

  trait Implicits {
    implicit val jacksonZero: Zero[JsonNode] = zero(MissingNode.getInstance)
    implicit val liftJsonZero: Zero[JValue] = zero(JNothing)
    implicit val minDateZero: Zero[Date] = zero(new Date(0))
    implicit val minDateTimeZero: Zero[DateTime] = zero(new DateTime(0))
    implicit val bigDecimalZero: Zero[BigDecimal] = zero(BigDecimal(0))
  }
  object Imports extends Implicits

}

