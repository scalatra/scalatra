package org.scalatra

import scalaz.Validation

package object validation {
  type FieldValidation[T] = Validation[ValidationError, T]

  type Validator[T] = FieldValidation[T] => FieldValidation[T]
}

