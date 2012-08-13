package org.scalatra
package databinding

import net.liftweb.json._

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
trait Command extends BindingImplicits {

  self: Command =>

  type Params = Map[String, String]

  type BindingAction = () => Any

  private[this] var preBindingActions: List[BindingAction] = Nil

  private[this] var postBindingActions: List[BindingAction] = Nil

  private[scalatra] var bindings: List[Binding[_]] = List.empty

  /**
   * Create a binding with the given [[mm.scalatra.command.field.Field]].
   */
  def bind[T: Manifest](field: Binding[T]): Binding[T] = {
    bindings = bindings :+ field
    field
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

  /**
   * Bind all registered [[org.scalatra.command.Binding]] with values
   * taken from Scalatra [[org.scalatra.ScalatraBase.params]].
   *
   * Also execute any ''before'' and ''after'' action eventually registered.
   *
   */
   def doBinding(params: Params = Map.empty, paramsOnly: Boolean = false): this.type = {
    doBeforeBindingActions
    bindings foreach { binding =>
      this match {
        case d: ForceFromParams if d.namesToForce.contains(binding.name) => bindFromParams(params, binding)
        case _ if paramsOnly => bindFromParams(params, binding)
        case _ => bindFromParams(params, binding)
      }

    }
    doAfterBindingActions
    this
  }

  private def bindFromParams(params: Params, binding: Binding[_]) = {
    params.get(binding.name) foreach binding.apply
  }

  private def doBeforeBindingActions = preBindingActions.foreach(_.apply())

  private def doAfterBindingActions = postBindingActions.foreach(_.apply())

  override def toString: String = "%s(bindings: [%s])".format(getClass.getName, bindings.mkString(", "))
}

trait ForceFromParams { self: Command =>

  def namesToForce: Set[String]
}

