package org.scalatra
package databinding

import util.{MultiMap, ValueReader}

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
trait Command extends CommandBindingSyntax {

  self: Command =>

  protected implicit val thisCommand: Command = this
  type Params = Map[String, String]

  type BindingAction = () => Any

  private[this] var preBindingActions: List[BindingAction] = Nil

  private[this] var postBindingActions: List[BindingAction] = Nil

  private[scalatra] var bindings: List[Binding[_]] = List.empty

  def replace[T](b: Binding[T]): Binding[T] = {
    bindings = bindings.filterNot(_.name == b.name) :+ b
    b
  }

  /**
   * Create a binding with the given [[org.scalatra.command.field.Field]].
   */
  def bind[T: Manifest](field: String): Binding[T] = replace(CommandBinding(field))
  def bind[T: Manifest](b: Binding[T]): Binding[T] = {
    val converted: Binding[T] = b match {
      case c: ValidatedBinding[_, _] => new ValidatedCommandBinding(c.binding.asInstanceOf[ValidatableBinding[_, T]])
      case _: ValidatableBinding[_, _] =>
        val c = b.asInstanceOf[ValidatableBinding[_, T]]
        BoundCommandBinding(c.original, c.value, c.binding)
      case _: CommandBinding[_] | _: BoundCommandBinding[_, _] | _: ValidatedCommandBinding[_, _] => b
      case _: Binding[_] => new CommandBinding(b.name, b.validators, b.defaultValue)

    }
    replace(converted)
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
   def bindTo[T](
          data: T,
          params: MultiParams = MultiMap.empty,
          headers: Map[String, String] = Map.empty,
          paramsOnly: Boolean = false)(
          implicit
            mf: Manifest[T],
            reader: T => ValueReader[T],
            multiParams: MultiParams => ValueReader[MultiParams]): this.type = {
    doBeforeBindingActions
    bindings foreach { binding =>
      this match {
        case d: ForceFromParams if d.namesToForce.contains(binding.name) => params.read(binding.name)
        case d: ForceFromHeaders if d.namesToForce.contains(binding.name) => headers.get(binding.name)
        case _ if paramsOnly => params.read(binding.name)
        case _ => data.read(binding.name)
      }
    }
    doAfterBindingActions
    this
  }

//  private def bindData[T](binding: Binding[_])(implicit reader: ValueReader[T]) = {
//    reader.read(binding.name)
//  }

  private def doBeforeBindingActions = preBindingActions.foreach(_.apply())

  private def doAfterBindingActions = postBindingActions.foreach(_.apply())

  override def toString: String = "%s(bindings: [%s])".format(getClass.getName, bindings.mkString(", "))
}

trait ForceFromParams { self: Command =>

  def namesToForce: Set[String]
}

trait ForceFromHeaders { self: Command =>

  def namesToForce: Set[String]
}

