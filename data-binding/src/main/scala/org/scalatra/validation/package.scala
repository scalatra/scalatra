package org.scalatra

import scalaz.Validation

package object validation {
  type FieldValidation[T] = Validation[FieldError, T]

  type Validator[T] = PartialFunction[Option[T], FieldValidation[T]]
}

