package org.scalatra
package swagger

import org.scalatra.commands._
import org.scalatra.swagger.SwaggerCommandSupport.CommandOperationBuilder
import org.scalatra.swagger.SwaggerSupportSyntax.SwaggerOperationBuilder
import org.scalatra.util.RicherString._

import scalaz.Scalaz._

object SwaggerCommandSupport {

  private[this] val paramtypeMapping =
    Map(
      ValueSource.Body -> ParamType.Body,
      ValueSource.Header -> ParamType.Header,
      ValueSource.Query -> ParamType.Query,
      ValueSource.Path -> ParamType.Path)

  def parametersFromCommand[T <: Command](obj: T)(implicit mf: Manifest[T]): (List[Parameter], Option[Model]) = {
    addModelFromCommand(obj, createParameterList(obj))
  }

  private[this] def createParameterList[T <: Command](obj: T)(implicit mf: Manifest[T]): List[Parameter] = {
    mf.erasure.getMethods().foldLeft(List.empty[Parameter]) { (lst, fld) =>
      if (fld.getReturnType().isAssignableFrom(classOf[Field[_]]) && fld.getParameterTypes().isEmpty) {
        val f = fld.invoke(obj).asInstanceOf[Field[Any]]
        // remove if statement below to include header params in description again
        if (f.valueSource == ValueSource.Header) lst else {
          Parameter(
            f.displayName | f.name,
            DataType(f.binding.valueManifest),
            f.description.blankOption,
            f.notes.blankOption,
            paramtypeMapping(f.valueSource),
            if (f.isRequired) None else f.defaultValue.flatMap(_.toString.blankOption),
            if (f.allowableValues.nonEmpty) AllowableValues(f.allowableValues) else AllowableValues.AnyValue,
            required = f.isRequired,
            position = f.position) :: lst
        }
      } else lst
    }
  }

  private[this] def addModelFromCommand[T <: Command](obj: T, pars: List[Parameter])(implicit mf: Manifest[T]) = {
    val (fields, parameters) = pars.partition(_.paramType == ParamType.Body)
    if (fields.nonEmpty) {
      val model = modelFromCommand(obj, fields)
      val bodyParam =
        Parameter(
          "body",
          DataType(model.id),
          model.description,
          None,
          ParamType.Body,
          None)
      (bodyParam :: parameters, Some(model))
    } else (parameters, None)
  }

  private[this] def modelFromCommand[T <: Command](cmd: T, fields: List[Parameter]) = {
    val modelFields = fields map { f =>
      f.name -> ModelProperty(f.`type`, f.position, required = f.required, allowableValues = f.allowableValues)
    }
    Model(cmd.commandName, cmd.commandName, None, cmd.commandDescription.blankOption, modelFields)
  }

  class CommandOperationBuilder[B <: SwaggerOperationBuilder[_]](registerModel: Model => Unit, underlying: B) {
    def parametersFromCommand[C <: Command: Manifest]: B =
      parametersFromCommand(manifest[C].erasure.newInstance().asInstanceOf[C])

    def parametersFromCommand[C <: Command: Manifest](cmd: => C): B = {
      SwaggerCommandSupport.parametersFromCommand(cmd) match {
        case (parameters, None) =>
          underlying.parameters(parameters: _*)
        case (parameters, Some(model)) =>
          registerModel(model)
          underlying.parameters(parameters: _*)
      }
      underlying
    }
  }
}
trait SwaggerCommandSupport { this: ScalatraBase with SwaggerSupportBase with SwaggerSupportSyntax with CommandSupport =>

  @deprecated("Use the `apiOperation.parameters` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def parameters[T <: CommandType: Manifest] =
    swaggerMeta(Symbols.Parameters, parametersFromCommand[T])

  @deprecated("Use the `apiOperation.parameters` and `operation` methods to build swagger descriptions of endpoints", "2.2")
  protected def parameters[T <: CommandType: Manifest](cmd: => T) =
    swaggerMeta(Symbols.Parameters, parametersFromCommand(cmd))

  protected implicit def operationBuilder2commandOpBuilder[B <: SwaggerOperationBuilder[_]](underlying: B) =
    new CommandOperationBuilder(registerModel(_), underlying)

  private[this] def parametersFromCommand[T <: CommandType](implicit mf: Manifest[T]): List[Parameter] = {
    parametersFromCommand(mf.erasure.newInstance().asInstanceOf[T])
  }

  private[this] def parametersFromCommand[T <: CommandType](cmd: => T)(implicit mf: Manifest[T]): List[Parameter] = {
    SwaggerCommandSupport.parametersFromCommand(cmd) match {
      case (parameters, None) => parameters
      case (parameters, Some(model)) =>
        _models += model.id -> model
        parameters
    }
  }
}
