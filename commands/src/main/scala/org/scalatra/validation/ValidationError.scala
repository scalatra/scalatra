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
      case Some(f: FieldName) => f
    }
    val code = arguments collectFirst {
      case f: ErrorCode =>  f
      case Some(f: ErrorCode) => f
    }
    val args = arguments filter {
      case _: FieldName | _: ErrorCode | Some(_: FieldName) | Some(_: ErrorCode) => false
      case _ => false
    }
    new ValidationError(msg, field, code, args)
  }
}

class ErrorCodeSerializer(knownCodes: ErrorCode*) extends Serializer[ErrorCode] {
  val ecs = Map(knownCodes map { c ⇒ c.getClass.getSimpleName.replaceAll("\\$$","").toUpperCase -> c }: _*)
  val Class = classOf[ErrorCode]

  def deserialize(implicit format: Formats): PartialFunction[(TypeInfo, JValue), ErrorCode] = {
    case (TypeInfo(Class, _), JString(c)) if ecs contains c.toUpperCase =>
      ecs get c.toUpperCase getOrElse UnknownError
    case (TypeInfo(Class, _), json) =>
      throw new MappingException("Can't convert " + json + " to " + Class)
  }

  def serialize(implicit format: Formats): PartialFunction[Any, JValue] = {
    case c: ErrorCode => JString(c.getClass.getSimpleName.replaceAll("\\$$", ""))
  }
}

class ValidationErrorSerializer(includeCode: Boolean = true, includeArgs: Boolean = true) extends CustomSerializer[ValidationError](
  (formats: Formats) ⇒ ({
    case jo @ JObject(JField("message", _) :: _) ⇒
      implicit val fmts = formats
      ValidationError(
        (jo \ "message").extractOrElse(""),
        (jo \ "fieldName").extractOpt[String],
        (jo \ "code").extractOpt[ErrorCode],
        (jo \ "args").extractOrElse[List[JValue]](Nil))
  }, {
    case ValidationError(message, fieldName, code, args) ⇒
      implicit val fmts = formats
      val jv: JValue = ("message" -> message)
      val wf: JValue = fieldName map (fn ⇒ ("field" -> fn.name): JValue) getOrElse JNothing
      val ec: JValue = if (includeCode) ("code" -> (code map (Extraction.decompose(_)(formats)))) else JNothing
      val arg: JValue = if (includeArgs) ("args" -> Extraction.decompose(args)(formats)) else JNothing
      jv merge wf merge ec merge arg
  }))