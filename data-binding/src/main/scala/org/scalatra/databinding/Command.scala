package org.scalatra
package databinding

import util._
import conversion._
import scalaz._
import Scalaz._
import collection.immutable
import org.joda.time.DateTime
import java.util.Date

/**
* Trait that identifies a ''Command object'', i.e. a Scala class instance which fields are bound to external parameters
* taken from Scalatra' __params__ object.
*
* An usage example can be seen below:
* {{{
* class PersonForm extends Command {
*
*  import Command._
*
*  val name = bind[String]("f_name")
*  val surname = bind[String]("f_surname")
*  val age = bind[Int]("f_age")
*  val registeredOn = bind[Date]("f_reg_date" -> "yyyyMMdd")
* }
* }}}
*
* In the example above, class field ''name'' will be bound, at runtime, with a parameter named ''f_name'' and so on.
* The binding is typed and for every registered type `T` (see [[org.scalatra.command.field.ImplicitCommonFields]] for
* a list of all availables) an automatic conversion `(String) => T` will take place during binding phase.
*
* After that binding has been performed (i.e. after that [[org.scalatra.command.Command#doBinding()]] has been called)
* on a specific instance, it is possible retrieve field values as [[scala.Option]], i.e.:
*
* {{{
* val form = new PersonForm
* form.doBinding(params)
* val registrationDate = form.registeredOn.value.getOrElse(new Date())
* }}}
*
*
* @author mmazzarolo
* @version 0.1
*
*/

//trait CommandConverterType {
//  type CommandTypeConverterFactory[T] <: TypeConverterFactory[T]
//}
trait Command extends BindingSyntax with ParamsValueReaderProperties { self: TypeConverterFactoryConversions =>

  type CommandTypeConverterFactory[T] <: TypeConverterFactory[T]
  private[this] var preBindingActions: Seq[BindingAction] = Nil

  private[this] var postBindingActions: Seq[BindingAction] = Nil

  private[scalatra] var bindings: Seq[FieldBinding] = Nil

  private var _errors: Seq[Binding] = Nil

  /**
   * Check whether this command is valid.
   */
  def isValid = errors.isEmpty
  def isInvalid = errors.nonEmpty

  /**
   * Return a Map of all field command error keyed by field binding name (NOT the name of the variable in command
   * object).
   */
  def errors: Seq[Binding] = _errors

  /**
   * Perform command as afterBinding task.
   */
  afterBinding {
    _errors = bindings.filter(_.isInvalid).map(_.binding)
  }

  implicit def field2binding[T:Manifest:Zero:TypeConverterFactory](field: Field[T]): FieldBinding = {
    val b = FieldBinding(field)
    bindings :+= b
    b
  }

  def typeConverterBuilder[I](tc: CommandTypeConverterFactory[_]): PartialFunction[ValueReader[_, _], TypeConverter[I, _]] = {
    case r: MultiParamsValueReader => tc.resolveMultiParams.asInstanceOf[TypeConverter[I, _]]
    case r: MultiMapHeadViewValueReader[_] => tc.resolveStringParams.asInstanceOf[TypeConverter[I, _]]
    case r: StringMapValueReader => tc.resolveStringParams.asInstanceOf[TypeConverter[I, _]]
    case r => throw new BindingException("No converter found for value reader: " + r.getClass.getSimpleName)
  }

  /**
   * Add an action that will be evaluated before field binding occurs.
   */
  protected def beforeBinding(action: => Any) {
    preBindingActions = preBindingActions :+ (() => action)
  }

  /**
   * Add an action that will be evaluated after field binding has been done.
   */
  protected def afterBinding(action: => Any) {
    postBindingActions = postBindingActions :+ (() => action)
  }

  def bindTo[S, I](
            data: S,
            params: MultiParams = MultiMap.empty,
            headers: Map[String, String] = Map.empty,
            paramsOnly: Boolean = false)(implicit r: S => ValueReader[S, I], mi: Manifest[I], zi: Zero[I], multiParams: MultiParams => ValueReader[MultiParams, Seq[String]]): this.type = {
    doBeforeBindingActions()

    bindings foreach { bb =>
      val b = bb.binding
      val name = b.name
      val tcf = b.typeConverterFactory
      val cv = typeConverterBuilder(tcf.asInstanceOf[CommandTypeConverterFactory[_]])(data).asInstanceOf[TypeConverter[I, b.T]]
      val bindData = bb.bindData(b.field, cv, b.typeConverterFactory)(mi, zi, b.valueManifest, b.valueZero)
      val fieldBinding = bindData.binding
      fieldBinding(data.read(name).map(_.asInstanceOf[fieldBinding.S]))

      this match {
        case d: ForceFromParams if d.namesToForce.contains(name) =>
          fieldBinding(params.read(name).map(_.asInstanceOf[fieldBinding.S]))
        case d: ForceFromHeaders if d.namesToForce.contains(name) =>
          fieldBinding(headers.get(name).map(_.asInstanceOf[fieldBinding.S]))
        case _ if paramsOnly =>
          fieldBinding(params.read(name).map(_.asInstanceOf[fieldBinding.S]))
        case _ =>
          fieldBinding(data.read(name).map(_.asInstanceOf[fieldBinding.S]))
      }
    }

    doAfterBindingActions()
    this
  }

  private def doBeforeBindingActions() = preBindingActions.foreach(_.apply())

  private def doAfterBindingActions() = postBindingActions.foreach(_.apply())

  override def toString: String = "%s(bindings: [%s])".format(getClass.getName, bindings.mkString(", "))



}

trait ParamsOnlyCommand extends TypeConverterFactoryImplicits with Command {
  type CommandTypeConverterFactory[T] = TypeConverterFactory[T]
}

trait ForceFromParams { self: Command =>

  def namesToForce: Set[String]
}

trait ForceFromHeaders { self: Command =>

  def namesToForce: Set[String]
}

