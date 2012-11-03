package org.scalatra

import validation.ValidationError
import scalaz._
import Scalaz._
import org.json4s.JsonAST.{JValue, JNothing}
import java.util.Date
import org.joda.time.DateTime

package object databinding {

  type FieldValidation[T] = Validation[ValidationError, T]
  
  type ModelValidation[T] = ValidationNEL[ValidationError, T]

  type Validator[T] = FieldValidation[T] => FieldValidation[T]

  type BindingValidator[T] = (String) => Validator[T]

  type BindingAction = () => Any

//  trait DefaultValues {
//    implicit val minDateDefault: org.scalatra.DefaultValue[Date] = default(new Date(0))
//    implicit val minDateTimeDefault: org.scalatra.DefaultValue[DateTime] = default(new DateTime(0))
//    implicit val bigDecimalDefault: org.scalatra.DefaultValue[BigDecimal] = default(BigDecimal(0))
//  }
//
//  object DefaultValues extends org.scalatra.databinding.DefaultValues

}

