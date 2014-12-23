package org.scalatra
package validation

import org.json4s.JsonDSL._
import org.json4s._

/**
 * Encapsulates errors in an API
 * @param message The message for this error
 * @param field An optional field name, useful when it applies to a particular field
 * @param code Decouple business logic error types from http errors
 * @param args Optional args, these need to be serializable to json if you're using the [[org.scalatra.validation.ValidationErrorSerializer]]
 */
case class ValidationError(message: String, field: Option[FieldName], code: Option[ErrorCode], args: Seq[Any])

/**
 * Encapsulates a field name for use in a validation error
 *
 * @param name The name of the field
 */
case class FieldName(name: String)

/**
 * A base trait for error codes, all error codes need to extend this.
 * It's available for you so you can add more in your app
 */
trait ErrorCode
case object NotFound extends ErrorCode
case object UnknownError extends ErrorCode
case object NotImplemented extends ErrorCode
case object BadGateway extends ErrorCode
case object ValidationFail extends ErrorCode
case object ServiceUnavailable extends ErrorCode
case object GatewayTimeout extends ErrorCode

/**
 * Allows for unordered building of [[org.scalatra.validation.ValidationError]]
 */
object ValidationError {
  /**
   * Allows for unordered building of [[org.scalatra.validation.ValidationError]]
   * You can add do `ValidationError("the message", ValidationFail, FieldName("email"))`
   * or `ValidationError("the message", FieldName("email"), ValidationFail)`
   * and also `ValidationError("the message", FieldName("email"), Some(ValidationFail))`
   *
   * This method will create a correct ValidationError that looks like:
   * `ValidationError("the message", FieldName("email"), Some(ValidationFail), Seq())`
   */
  def apply(msg: String, arguments: Any*): ValidationError = {
    val field = arguments collectFirst {
      case f: FieldName => f
      case Some(f: FieldName) => f
    }
    val code = arguments collectFirst {
      case f: ErrorCode => f
      case Some(f: ErrorCode) => f
    }
    val args = arguments filter {
      case _: FieldName | _: ErrorCode | Some(_: FieldName) | Some(_: ErrorCode) => false
      case _ => false
    }
    new ValidationError(msg, field, code, args)
  }
}

/**
 * Assumes your error codes will always be case objects.
 *
 * @param knownCodes A list of known error codes for your system
 */
class ErrorCodeSerializer(knownCodes: ErrorCode*) extends Serializer[ErrorCode] {
  val ecs = Map(knownCodes map { c ⇒ c.getClass.getSimpleName.replaceAll("\\$$", "").toUpperCase -> c }: _*)
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

/**
 * Serializes a validation error into a structure:
 * <pre>
 *   {
 *     "message": "the error message",
 *     "field": "field_name",
 *     "code": "ValidationFail",
 *     "args": []
 *   }
 * </pre>
 *
 * You can configure whether or not to include the args. And on the way in it assumes args are JValues.
 * The fields: field, code and args are only added if they have actual data.
 *
 * @param includeCode Include the code field if an error code is provided
 * @param includeArgs Include the args field when args are provided
 */
class ValidationErrorSerializer(includeCode: Boolean = true, includeArgs: Boolean = true) extends CustomSerializer[ValidationError](
  (formats: Formats) ⇒ ({
    case jo @ JObject(JField("message", _) :: _) ⇒
      implicit val fmts = formats
      new ValidationError(
        (jo \ "message").extractOrElse(""),
        (jo \ "field").extractOpt[String] map FieldName,
        (jo \ "code").extractOpt[ErrorCode],
        (jo \ "args").children)
  }, {
    case ValidationError(message, fieldName, code, args) ⇒
      implicit val fmts = formats
      val jv: JValue = ("message" -> message)
      val wf: JValue = fieldName map (fn ⇒ ("field" -> fn.name): JValue) getOrElse JNothing
      val ec: JValue = if (includeCode && code.isDefined) ("code" -> (code map (Extraction.decompose(_)(formats)))) else JNothing
      val arg: JValue = if (includeArgs && args.nonEmpty) ("args" -> Extraction.decompose(args)(formats)) else JNothing
      jv merge wf merge ec merge arg
  }))