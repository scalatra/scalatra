package org.scalatra
package validation

import org.json4s._
import JsonDSL._


case class ValidationError(message: String, field: Option[FieldName], code: Option[ErrorCode], args: Seq[Any])
case class FieldName(name: String)

trait ErrorCode
case object NotFound extends ErrorCode
case object UnknownError extends ErrorCode
case object NotImplemented extends ErrorCode
case object BadGateway extends ErrorCode
case object ValidationFail extends ErrorCode
case object ServiceUnavailable extends ErrorCode
case object GatewayTimeout extends ErrorCode

object ValidationError {
  def apply(msg: String, arguments: Any*): ValidationError = {
    val field = arguments collectFirst {
      case f: FieldName => f
    }
    val code = arguments collectFirst {
      case f: ErrorCode =>  f
    }
    val args = if (field.isDefined) arguments filterNot {
      case _: FieldName | _: ErrorCode => true
      case _ => false
    } else arguments
    new ValidationError(msg, field, code, args)
  }
}

class ValidationErrorSerializer extends CustomSerializer[ValidationError](
  (formats: Formats) ⇒ ({
    case jo @ JObject(JField("message", _) :: _) ⇒
      implicit val fmts = formats
      ValidationError((jo \ "message").extractOrElse(""), (jo \ "fieldName").extractOpt[String], (jo \ "code").extractOpt[String], Nil)
  }, {
    case ValidationError(message, fieldName, code, _) ⇒
      implicit val fmts = formats
      val jv: JValue = ("message" -> message)
      val wf: JValue = fieldName map (fn ⇒ ("field" -> fn.name): JValue) getOrElse JNothing
      jv merge wf
  }))