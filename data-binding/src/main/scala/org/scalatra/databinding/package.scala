package org.scalatra

import validation.Validator

package object databinding {

  type BindingValidator[T] = (String) => Validator[T]
}

