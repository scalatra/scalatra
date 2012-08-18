package org.scalatra
package validation

case class ValidationError(message: String, field: Option[FieldName], args: Any*)
case class FieldName(name: String)

object ValidationError {
  def apply(msg: String, arguments: Any*): ValidationError = {
    val field = arguments collectFirst {
      case Some(f: FieldName) => f
    }
    val args = if (field.isDefined) arguments filterNot {
      case _: FieldName => true
      case _ => false
    } else arguments
    new ValidationError(msg, field, args)
  }
}