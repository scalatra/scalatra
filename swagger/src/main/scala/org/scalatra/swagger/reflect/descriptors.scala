package org.scalatra.swagger.reflect

import java.lang.reflect.{Field, TypeVariable}

sealed trait Descriptor
object ScalaType {
  def apply[T](mf: Manifest[T]): ScalaType = new ScalaType(mf)

  def apply(erasure: Class[_], typeArgs: Seq[ScalaType] = Seq.empty): ScalaType = {
    val mf = ManifestFactory.manifestOf(erasure, typeArgs.map(_.manifest))
    new ScalaType(mf)
  }
}
class ScalaType(private val manifest: Manifest[_]) extends Descriptor with Equals {

  val erasure: Class[_] = manifest.erasure

  val typeArgs: Seq[ScalaType] = manifest.typeArguments.map(ta => Reflector.scalaTypeOf(ta)) ++ (
    if (erasure.isArray) List(Reflector.scalaTypeOf(erasure.getComponentType)) else Nil
  )

  val typeVars: Map[TypeVariable[_], ScalaType] = Map.empty ++
    erasure.getTypeParameters.map(_.asInstanceOf[TypeVariable[_]]).toList.zip(manifest.typeArguments map (ScalaType(_)))

  val isArray: Boolean = erasure.isArray

  lazy val rawFullName: String = erasure.getName

  lazy val rawSimpleName: String = erasure.getSimpleName

  lazy val simpleName: String =
    rawSimpleName + (if (typeArgs.nonEmpty) typeArgs.map(_.simpleName).mkString("[", ", ", "]") else (if (typeVars.nonEmpty) typeVars.map(_._2.simpleName).mkString("[", ", ", "]") else ""))

  lazy val fullName: String =
    rawFullName + (if (typeArgs.nonEmpty) typeArgs.map(_.fullName).mkString("[", ", ", "]") else "")

  lazy val isPrimitive: Boolean = Reflector.isPrimitive(erasure)

  def isMap = classOf[Map[_, _]].isAssignableFrom(erasure)
  def isCollection = classOf[Iterable[_]].isAssignableFrom(erasure)
  def isOption = classOf[Option[_]].isAssignableFrom(erasure)
  def <:<(that: ScalaType): Boolean = manifest <:< that.manifest
  def >:>(that: ScalaType): Boolean = manifest >:> that.manifest

  override def hashCode(): Int = manifest.##

  override def equals(obj: Any): Boolean = obj match {
    case a: ScalaType => manifest == a.manifest
    case _ => false
  }

  def canEqual(that: Any): Boolean = that match {
    case s: ScalaType => manifest.canEqual(s.manifest)
    case _ => false
  }

  def copy(typeArgs: Seq[ScalaType] = typeArgs, typeVars: Map[TypeVariable[_], ScalaType] = typeVars) = {
    new ScalaType(ManifestFactory.manifestOf(erasure, typeArgs.map(_.manifest)))
  }

  override def toString: String = simpleName
}
case class PropertyDescriptor(name: String, mangledName: String, returnType: ScalaType, field: Field) extends Descriptor {
  def set(receiver: Any, value: Any) = field.set(receiver, value)
  def get(receiver: AnyRef) = field.get(receiver)
  def isPrimitive = returnType.isPrimitive
}
case class ConstructorParamDescriptor(name: String, mangledName: String, argIndex: Int, argType: ScalaType, defaultValue: Option[() => Any]) extends Descriptor {
  lazy val isOptional = defaultValue.isDefined || classOf[Option[_]].isAssignableFrom(argType.erasure)
  def isPrimitive = argType.isPrimitive
  def isMap = argType.isMap
  def isCollection = argType.isCollection
  def isOption = argType.isOption
  def isCustom = !(isPrimitive || isMap || isCollection || isOption)
}
case class ConstructorDescriptor(params: Seq[ConstructorParamDescriptor], constructor: java.lang.reflect.Constructor[_], isPrimary: Boolean) extends Descriptor
case class SingletonDescriptor(simpleName: String, fullName: String, erasure: ScalaType, instance: AnyRef, properties: Seq[PropertyDescriptor]) extends Descriptor
case class ClassDescriptor(simpleName: String, fullName: String, erasure: ScalaType, companion: Option[SingletonDescriptor], constructors: Seq[ConstructorDescriptor], properties: Seq[PropertyDescriptor]) extends Descriptor {
//    def bestConstructor(argNames: Seq[String]): Option[ConstructorDescriptor] = {
//      constructors.sortBy(-_.params.size)
//    }
  lazy val mostComprehensive: Seq[ConstructorParamDescriptor] = if (constructors.isEmpty) Seq.empty else constructors.sortBy(-_.params.size).head.params
}
case class PrimitiveDescriptor(simpleName: String, fullName: String, erasure: ScalaType) extends Descriptor {
}

