package org.scalatra
package databinding

import util._
import conversion._
import scalaz._
import Scalaz._
import collection.immutable
import org.joda.time.DateTime
import java.util.Date


trait CommandSupport extends TypeConverterFactories { self: ScalatraBase =>

}
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
trait Command extends BindingSyntax with ParamsValueReaderProperties { self: Command =>


  private[this] var preBindingActions: Seq[BindingAction] = Nil

  private[this] var postBindingActions: Seq[BindingAction] = Nil

  private[scalatra] var bindings: Map[String, NewBindingContainer] = Map.empty

  /**
   * Create a binding with the given [[org.scalatra.command.field.Field]].
   */
  def bind[T : Manifest : Zero : TypeConverterFactory](field: String): Binding[T] = {
    bindings += field -> NewBindingContainer[T](field)
    bindings(field).binding.asInstanceOf[Binding[T]]
  }
  def bind[T : Manifest : Zero : TypeConverterFactory](field: Binding[T]): Binding[T] = {
    bindings += field.name -> NewBindingContainer[T](field)
    field
  }

  def apply(name: String): NewBindingContainer = bindings(name)
  
//  def typeConverterFactory[S, I](tc: TypeConverterFactory[_], reader: ValueReader[S, I]): TypeConverter[I, _] =
//    upcast[I](tc)(reader)

  def typeConverterBuilder[I, T](tc: TypeConverterFactory[T]): PartialFunction[ValueReader[_, _], TypeConverter[I, T]] = {
    case r: MultiParamsValueReader => tc.resolveMultiParams.asInstanceOf[TypeConverter[I, T]]
    case r: StringMapValueReader => tc.resolveStringParams.asInstanceOf[TypeConverter[I, T]]
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

    bindings = bindings map { case (name, b) =>
      val cv = typeConverterBuilder(b.typeConverterFactory)(data).asInstanceOf[TypeConverter[I, b.T]]
      val container = BindingContainer(b.binding, cv, b.typeConverterFactory)(mi, zi, b.valueManifest, b.valueZero)
      val bound = container(data.read(b.name).map(_.asInstanceOf[container.S]))

      this match {
        case d: ForceFromParams if d.namesToForce.contains(name) =>
          container(params.read(b.name).map(_.asInstanceOf[container.S]))
        case d: ForceFromHeaders if d.namesToForce.contains(name) =>
          container(headers.get(name).map(_.asInstanceOf[container.S]))
        case _ if paramsOnly =>
          container(params.read(name).map(_.asInstanceOf[container.S]))
        case _ =>
          container(data.read(name).map(_.asInstanceOf[container.S]))
      }
      name -> bound
    }

    doAfterBindingActions()
    this
  }

  private def doBeforeBindingActions() = preBindingActions.foreach(_.apply())

  private def doAfterBindingActions() = postBindingActions.foreach(_.apply())

  override def toString: String = "%s(bindings: [%s])".format(getClass.getName, bindings.mkString(", "))



}

trait ForceFromParams { self: Command =>

  def namesToForce: Set[String]
}

trait ForceFromHeaders { self: Command =>

  def namesToForce: Set[String]
}

