package org.scalatra
package validation

object FieldError {
  def apply(message: String, args: Any*) = {
    args.headOption match {
      case Some(a: String) if a != null && a.trim.nonEmpty => ValidationError(message, a, args.drop(1):_*)
      case _ => SimpleError(message, args:_*)
    }
  }
}
trait FieldError {
  def message: String
  def args: Seq[Any]
}
case class ValidationError(message: String, field: String, args: Any*) extends FieldError
case class SimpleError(message: String, args: Any*) extends FieldError

