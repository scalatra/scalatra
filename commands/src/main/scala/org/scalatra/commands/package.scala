package org.scalatra

import org.scalatra.validation.ValidationError

import scalaz._

package object commands {

  type FieldValidation[T] = Validation[ValidationError, T]

  type ModelValidation[T] = ValidationNel[ValidationError, T]

  type Validator[T] = FieldValidation[T] => FieldValidation[T]

  type BindingValidator[T] = (String) => Validator[T]

  type BindingAction = () => Any

  //
  //  implicit val minDateDefault: org.scalatra.DefaultValue[Date] = default(new Date(0))
  //  implicit val minDateTimeDefault: org.scalatra.DefaultValue[DateTime] = default(new DateTime(0).withZone(DateTimeZone.UTC))
  //  implicit val jsonDefault: org.scalatra.DefaultValue[JValue] = default(JNothing)

}

