package org.scalatra

import org.scalatra.validation.ValidationError

import scalaz._

package object commands {

  @deprecated("Use scalatra-forms instead.", "2.6.0")
  type FieldValidation[T] = Validation[ValidationError, T]

  @deprecated("Use scalatra-forms instead.", "2.6.0")
  type ModelValidation[T] = ValidationNel[ValidationError, T]

  @deprecated("Use scalatra-forms instead.", "2.6.0")
  type Validator[T] = FieldValidation[T] => FieldValidation[T]

  @deprecated("Use scalatra-forms instead.", "2.6.0")
  type BindingValidator[T] = (String) => Validator[T]

  @deprecated("Use scalatra-forms instead.", "2.6.0")
  type BindingAction = () => Any

  //
  //  implicit val minDateDefault: org.scalatra.DefaultValue[Date] = default(new Date(0))
  //  implicit val minDateTimeDefault: org.scalatra.DefaultValue[DateTime] = default(new DateTime(0).withZone(DateTimeZone.UTC))
  //  implicit val jsonDefault: org.scalatra.DefaultValue[JValue] = default(JNothing)

}

