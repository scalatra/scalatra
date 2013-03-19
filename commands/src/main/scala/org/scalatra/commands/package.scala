package org.scalatra

import validation.ValidationError
import scalaz._
import scalaz.syntax.validation._
import org.json4s.JsonAST.{JValue, JNothing}
import java.util.Date
import org.joda.time.{ DateTime, DateTimeZone }

package object commands extends DefaultValues {

  type FieldValidation[T] = Validation[ValidationError, T]
  
  type ModelValidation[T] = ValidationNel[ValidationError, T]

  type Validator[T] = FieldValidation[T] => FieldValidation[T]

  type BindingValidator[T] = (String) => Validator[T]

  type BindingAction = () => Any

 
  implicit val minDateDefault: org.scalatra.DefaultValue[Date] = default(new Date(0))
  implicit val minDateTimeDefault: org.scalatra.DefaultValue[DateTime] = default(new DateTime(0).withZone(DateTimeZone.UTC))
  implicit val jsonDefault: org.scalatra.DefaultValue[JValue] = default(JNothing)
  

}

