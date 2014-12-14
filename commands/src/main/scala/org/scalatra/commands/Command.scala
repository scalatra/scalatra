package org.scalatra
package commands

import org.scalatra.util._
import org.scalatra.util.conversion._

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
 * The binding is typed and for every registered type `T` (see [[org.scalatra.util.conversion.DefaultImplicitConversions]] for
 * a list of all availables) an automatic conversion `(String) => T` will take place during binding phase.
 *
 * After that binding has been performed (i.e. after that [[org.scalatra.commands.Command#bindTo()]] has been called)
 * on a specific instance, it is possible retrieve field values as [[scalaz.Validation]], i.e.:
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

trait Command extends BindingSyntax with ParamsValueReaderProperties {

  type CommandTypeConverterFactory[T] <: TypeConverterFactory[T]
  private[this] var preBindingActions: Seq[BindingAction] = Nil

  private[this] var postBindingActions: Seq[BindingAction] = Nil

  private[commands] var bindings: Map[String, Binding] = Map.empty

  private[this] var _errors: Seq[Binding] = Nil

  var commandName = getClass.getSimpleName

  var commandDescription = ""

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
    _errors = bindings.values.filter(_.isInvalid).toSeq
  }

  implicit def binding2field[T: Manifest: TypeConverterFactory](field: FieldDescriptor[T]): Field[T] = {
    new Field(bind(field), this)
  }

  implicit def autoBind[T: Manifest: TypeConverterFactory](fieldName: String): Field[T] =
    bind[T](FieldDescriptor[T](fieldName))

  implicit def bind[T](field: FieldDescriptor[T])(implicit mf: Manifest[T], conv: TypeConverterFactory[T]): FieldDescriptor[T] = {
    val f: FieldDescriptor[T] =
      if (mf.runtimeClass.isAssignableFrom(classOf[Option[_]])) {
        // Yay! not one but 2 casts in the same line
        field.asInstanceOf[FieldDescriptor[Option[_]]].withDefaultValue(None).asInstanceOf[FieldDescriptor[T]]
      } else field
    val b = Binding(f)
    bindings += b.name -> b
    f
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
    headers: Map[String, String] = Map.empty)(implicit r: S => ValueReader[S, I], mi: Manifest[I], multiParams: MultiParams => ValueReader[MultiParams, Seq[String]]): this.type = {
    doBeforeBindingActions()

    bindings = bindings map {
      case (name, b) =>
        val tcf = b.typeConverterFactory
        val cv = typeConverterBuilder(tcf.asInstanceOf[CommandTypeConverterFactory[_]])(data).asInstanceOf[TypeConverter[I, b.T]]
        val fieldBinding = Binding(b.field, cv, b.typeConverterFactory)(mi, b.valueManifest)

        val result = b.field.valueSource match {
          case ValueSource.Body => fieldBinding(data.read(name).right.map(_ map (_.asInstanceOf[fieldBinding.S])))
          case ValueSource.Header =>
            val tc: TypeConverter[String, _] = tcf.resolveStringParams
            val headersBinding = Binding(b.field, tc.asInstanceOf[TypeConverter[String, b.T]], tcf)(manifest[String], b.valueManifest)
            headersBinding(Right(headers.get(name).map(_.asInstanceOf[headersBinding.S])))
          case ValueSource.Path | ValueSource.Query =>
            val pv = typeConverterBuilder(tcf.asInstanceOf[CommandTypeConverterFactory[_]])(params).asInstanceOf[TypeConverter[Seq[String], b.T]]
            val paramsBinding = Binding(b.field, pv, b.typeConverterFactory)(manifest[Seq[String]], b.valueManifest)
            paramsBinding(params.read(name).right.map(_ map (_.asInstanceOf[paramsBinding.S])))
        }

        name -> result
    }

    // Defer validation until after all the fields have been bound.
    bindings = bindings map { case (k, v) => k -> v.validate }

    doAfterBindingActions()
    this
  }

  private def doBeforeBindingActions() = preBindingActions.foreach(_.apply())

  private def doAfterBindingActions() = postBindingActions.foreach(_.apply())

  override def toString: String = "%s(bindings: [%s])".format(getClass.getName, bindings.mkString(", "))

  private type ExecutorView[S] = (this.type => S) => CommandExecutor[this.type, S]
  def apply[S](handler: this.type => S)(implicit executor: ExecutorView[S]) = handler.execute(this)
  def execute[S](handler: this.type => S)(implicit executor: ExecutorView[S]) = apply(handler)
  def >>[S](handler: this.type => S)(implicit executor: ExecutorView[S]): S = apply(handler)
}

trait ParamsOnlyCommand extends TypeConverterFactories with Command {
  type CommandTypeConverterFactory[T] = TypeConverterFactory[T]
}

//trait ForceFromParams { self: Command =>
//
//  def forceFromParams: Set[String]
//}
//
//trait ForceFromHeaders { self: Command =>
//
//  def forceFromHeaders: Set[String]
//}

