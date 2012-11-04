package org.scalatra
package swagger

import org.scalatra.databinding._
import org.scalatra.util.RicherString._
import scalaz._
import Scalaz._

object SwaggerCommandSupport {
  
  private[this] val paramtypeMapping = 
                        Map(
                            ValueSource.Body -> ParamType.Body, 
                            ValueSource.Header -> ParamType.Header, 
                            ValueSource.Query -> ParamType.Query, 
                            ValueSource.Path -> ParamType.Path)
                            
  def parametersFromCommand[T <: Command](obj: T)(implicit mf: Manifest[T]): (List[Parameter], Option[Model]) = {
    val pars = mf.erasure.getFields().foldLeft(List.empty[Parameter]) { (lst, fld) =>
      if (fld.getType().isAssignableFrom(classOf[Field[_]])) {
	      val f = fld.get(obj).asInstanceOf[Field[_]]
	      Parameter(
	          f.displayName | f.name, 
	          f.description, 
	          DataType(f.binding.valueManifest), 
	          f.notes.blankOption, 
	          paramtypeMapping(f.valueSource), 
	          if (f.isRequired) None else f.defaultValue.toString.blankOption, 
            AllowableValues(f.allowableValues),
	          required = f.isRequired) :: lst
      } else lst

    }
    val (fields, parameters) = pars.partition(_.paramType == ParamType.Body)
    if (fields.nonEmpty) {
      val model = modelFromCommand(obj, fields)
      val bodyParam = 
          Parameter(
              "body", 
              model.description, 
              DataType(model.id), 
              None, 
              ParamType.Body, 
              None, 
              required = false) 
      (bodyParam :: parameters, Some(model)) 
    } else (parameters, None)
  }

  private[this] def modelFromCommand[T <: Command](cmd: T, fields: List[Parameter]) = {
    val modelFields = fields.map(f => f.name -> ModelField(f.name, f.description, f.dataType, f.defaultValue, required = f.required))
    Model(cmd.commandName, cmd.commandDescription, Map(modelFields:_*))
  }
}
trait SwaggerCommandSupport { this: ScalatraBase with SwaggerSupportBase with SwaggerSupportSyntax with CommandSupport =>

  protected def parameters[T <: CommandType : Manifest] = swaggerMeta(Symbols.Parameters, parametersFromCommand[T])
  
  
  private[this] def parametersFromCommand[T <: CommandType](implicit mf: Manifest[T]): List[Parameter] = {
    SwaggerCommandSupport.parametersFromCommand(command[T]) match {
      case (parameters, None) => parameters
      case (parameters, Some(model)) => 
        _models += model
        parameters
    }
  }
   
}
