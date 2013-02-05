package org.scalatra.swagger.reflect

import java.lang.reflect.{Field, TypeVariable}

sealed trait Descriptor
object ScalaType {
  def apply[T](mf: Manifest[T]): ScalaType = {
    val extra = if (mf.erasure.isArray) List(Reflector.scalaTypeOf(mf.erasure.getComponentType)) else Nil
    new ScalaType(
      mf.erasure,
      mf.typeArguments.map(ta => Reflector.scalaTypeOf(ta)) ::: extra,
      Map.empty ++
        mf.erasure.getTypeParameters.map(_.asInstanceOf[TypeVariable[_]]).toList.zip(mf.typeArguments map (ScalaType(_))),
      mf.erasure.isArray)
  }
}
case class ScalaType(erasure: Class[_], typeArgs: Seq[ScalaType], typeVars: Map[TypeVariable[_], ScalaType], isArray: Boolean = false) extends Descriptor {
  lazy val rawFullName: String = erasure.getName
  lazy val rawSimpleName: String = erasure.getSimpleName
  lazy val simpleName: String = rawSimpleName + (if (typeArgs.nonEmpty) typeArgs.map(_.simpleName).mkString("[", ", ", "]") else "")
  lazy val fullName: String = rawFullName + (if (typeArgs.nonEmpty) typeArgs.map(_.fullName).mkString("[", ", ", "]") else "")
  lazy val isPrimitive: Boolean = Reflector.isPrimitive(erasure)
  def isMap = classOf[Map[_, _]].isAssignableFrom(erasure)
  def isCollection = classOf[Iterable[_]].isAssignableFrom(erasure)
  def isOption = classOf[Option[_]].isAssignableFrom(erasure)
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

