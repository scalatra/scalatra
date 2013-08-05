package org.scalatra.swagger.reflect

import java.lang.reflect.{Field, TypeVariable}

sealed trait Descriptor
object ScalaType {
  private val types = new Memo[Manifest[_], ScalaType]

  def apply[T](mf: Manifest[T]): ScalaType = {
    /* optimization */
    if (mf.erasure == classOf[Int] || mf.erasure == classOf[java.lang.Integer]) ScalaType.IntType
    else if (mf.erasure == classOf[Long] || mf.erasure == classOf[java.lang.Long]) ScalaType.LongType
    else if (mf.erasure == classOf[Byte] || mf.erasure == classOf[java.lang.Byte]) ScalaType.ByteType
    else if (mf.erasure == classOf[Short] || mf.erasure == classOf[java.lang.Short]) ScalaType.ShortType
    else if (mf.erasure == classOf[Float] || mf.erasure == classOf[java.lang.Float]) ScalaType.FloatType
    else if (mf.erasure == classOf[Double] || mf.erasure == classOf[java.lang.Double]) ScalaType.DoubleType
    else if (mf.erasure == classOf[BigInt] || mf.erasure == classOf[java.math.BigInteger]) ScalaType.BigIntType
    else if (mf.erasure == classOf[BigDecimal] || mf.erasure == classOf[java.math.BigDecimal]) ScalaType.BigDecimalType
    else if (mf.erasure == classOf[Boolean] || mf.erasure == classOf[java.lang.Boolean]) ScalaType.BooleanType
    else if (mf.erasure == classOf[String] || mf.erasure == classOf[java.lang.String]) ScalaType.StringType
    else if (mf.erasure == classOf[java.util.Date]) ScalaType.DateType
    else if (mf.erasure == classOf[java.sql.Timestamp]) ScalaType.TimestampType
    else if (mf.erasure == classOf[Symbol]) ScalaType.SymbolType
    else if (mf.erasure == classOf[Number]) ScalaType.NumberType
    /* end optimization */
    else {
      if (mf.typeArguments.isEmpty) types(mf, new ScalaType(_))
      else new ScalaType(mf)
//      if (!mf.erasure.isArray) types(mf, new ScalaType(_))
//      else {
//        val nmf = ManifestFactory.manifestOf(mf.erasure, List(ManifestFactory.manifestOf(mf.erasure.getComponentType)))
//        types(nmf, new ScalaType(_))
//      }
//      new ScalaType(mf)
    }
  }

  def apply(erasure: Class[_], typeArgs: Seq[ScalaType] = Seq.empty): ScalaType = {
    val mf = ManifestFactory.manifestOf(erasure, typeArgs.map(_.manifest))
    ScalaType(mf)
  }

  private val IntType: ScalaType = new PrimitiveScalaType(Manifest.Int)
  private val NumberType: ScalaType = new PrimitiveScalaType(manifest[Number])
  private val LongType: ScalaType = new PrimitiveScalaType(Manifest.Long)
  private val ByteType: ScalaType = new PrimitiveScalaType(Manifest.Byte)
  private val ShortType: ScalaType = new PrimitiveScalaType(Manifest.Short)
  private val BooleanType: ScalaType = new PrimitiveScalaType(Manifest.Boolean)
  private val FloatType: ScalaType = new PrimitiveScalaType(Manifest.Float)
  private val DoubleType: ScalaType = new PrimitiveScalaType(Manifest.Double)
  private val StringType: ScalaType = new PrimitiveScalaType(manifest[java.lang.String])
  private val SymbolType: ScalaType = new PrimitiveScalaType(manifest[Symbol])
  private val BigDecimalType: ScalaType = new PrimitiveScalaType(manifest[BigDecimal])
  private val BigIntType: ScalaType = new PrimitiveScalaType(manifest[BigInt])
  private val DateType: ScalaType = new PrimitiveScalaType(manifest[java.util.Date])
  private val TimestampType: ScalaType = new PrimitiveScalaType(manifest[java.sql.Timestamp])

  private class PrimitiveScalaType(mf: Manifest[_]) extends ScalaType(mf) {
    override val isPrimitive = true
  }
  private class CopiedScalaType(
                    mf: Manifest[_],
                    private[this] var _typeVars: Map[TypeVariable[_], ScalaType],
                    override val isPrimitive: Boolean) extends ScalaType(mf) {
    override def typeVars: Map[TypeVariable[_], ScalaType] = {
      if (_typeVars == null)
        _typeVars = Map.empty ++
          erasure.getTypeParameters.map(_.asInstanceOf[TypeVariable[_]]).toList.zip(manifest.typeArguments map (ScalaType(_)))
      _typeVars
    }
  }
}
class ScalaType(private val manifest: Manifest[_]) extends Equals {

  import ScalaType.{ types, CopiedScalaType }
  private[this] val self = this
  val erasure: Class[_] = manifest.erasure

//  private[this] var _typeArgs: Seq[ScalaType] = null
//  def typeArgs: Seq[ScalaType] = {
//    if (_typeArgs == null)
//      _typeArgs = manifest.typeArguments.map(ta => Reflector.scalaTypeOf(ta)) ++ (
//        if (erasure.isArray) List(Reflector.scalaTypeOf(erasure.getComponentType)) else Nil
//      )
//    _typeArgs
//  }

  val typeArgs: Seq[ScalaType] = manifest.typeArguments.map(ta => Reflector.scalaTypeOf(ta)) ++ (
    if (erasure.isArray) List(Reflector.scalaTypeOf(erasure.getComponentType)) else Nil
  )

  private[this] var _typeVars: Map[TypeVariable[_], ScalaType] = null
  def typeVars: Map[TypeVariable[_], ScalaType] = {
    if (_typeVars == null)
      _typeVars = Map.empty ++
        erasure.getTypeParameters.map(_.asInstanceOf[TypeVariable[_]]).toList.zip(manifest.typeArguments map (ScalaType(_)))
    _typeVars
  }


  val isArray: Boolean = erasure.isArray

  private[this] var _rawFullName: String = null
  def rawFullName: String = {
    if (_rawFullName == null)
      _rawFullName = erasure.getName
    _rawFullName
  }

  private[this] var _rawSimpleName: String = null
  def rawSimpleName: String = {
    if (_rawSimpleName == null)
      _rawSimpleName = erasure.getSimpleName
    _rawSimpleName
  }

  lazy val simpleName: String =
    rawSimpleName + (if (typeArgs.nonEmpty) typeArgs.map(_.simpleName).mkString("[", ", ", "]") else (if (typeVars.nonEmpty) typeVars.map(_._2.simpleName).mkString("[", ", ", "]") else ""))

  lazy val fullName: String =
    rawFullName + (if (typeArgs.nonEmpty) typeArgs.map(_.fullName).mkString("[", ", ", "]") else "")

  val isPrimitive = false

  def isMap = classOf[Map[_, _]].isAssignableFrom(erasure)
  def isCollection = erasure.isArray || classOf[Iterable[_]].isAssignableFrom(erasure)
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

  def copy(erasure: Class[_] = erasure, typeArgs: Seq[ScalaType] = typeArgs, typeVars: Map[TypeVariable[_], ScalaType] = _typeVars): ScalaType = {
    /* optimization */
    if (erasure == classOf[Int] || erasure == classOf[java.lang.Integer]) ScalaType.IntType
    else if (erasure == classOf[Long] || erasure == classOf[java.lang.Long]) ScalaType.LongType
    else if (erasure == classOf[Byte] || erasure == classOf[java.lang.Byte]) ScalaType.ByteType
    else if (erasure == classOf[Short] || erasure == classOf[java.lang.Short]) ScalaType.ShortType
    else if (erasure == classOf[Float] || erasure == classOf[java.lang.Float]) ScalaType.FloatType
    else if (erasure == classOf[Double] || erasure == classOf[java.lang.Double]) ScalaType.DoubleType
    else if (erasure == classOf[BigInt] || erasure == classOf[java.math.BigInteger]) ScalaType.BigIntType
    else if (erasure == classOf[BigDecimal] || erasure == classOf[java.math.BigDecimal]) ScalaType.BigDecimalType
    else if (erasure == classOf[Boolean] || erasure == classOf[java.lang.Boolean]) ScalaType.BooleanType
    else if (erasure == classOf[String] || erasure == classOf[java.lang.String]) ScalaType.StringType
    else if (erasure == classOf[java.util.Date]) ScalaType.DateType
    else if (erasure == classOf[java.sql.Timestamp]) ScalaType.TimestampType
    else if (erasure == classOf[Symbol]) ScalaType.SymbolType
    else if (erasure == classOf[Number]) ScalaType.NumberType
    /* end optimization */
    else {
      val mf = ManifestFactory.manifestOf(erasure, typeArgs.map(_.manifest))
      val st = new CopiedScalaType(mf, typeVars, isPrimitive)
      if (typeArgs.isEmpty) types.replace(mf, st)
      else st
    }
  }

  override def toString: String = simpleName
}
case class PropertyDescriptor(name: String, mangledName: String, returnType: ScalaType, field: Field) extends Descriptor {
  def set(receiver: Any, value: Any) = field.set(receiver, value)
  def get(receiver: AnyRef) = field.get(receiver)

  def isPrimitive = returnType.isPrimitive
}
case class ConstructorParamDescriptor(name: String, mangledName: String, argIndex: Int, argType: ScalaType, defaultValue: Option[() => Any]) extends Descriptor {
  lazy val isOptional = defaultValue.isDefined || isOption
  def isPrimitive = argType.isPrimitive
  def isMap = argType.isMap
  def isCollection = argType.isCollection
  def isOption = argType.isOption
  def isCustom = !(isPrimitive || isMap || isCollection || isOption)
}
case class ConstructorDescriptor(params: Seq[ConstructorParamDescriptor], constructor: java.lang.reflect.Constructor[_], isPrimary: Boolean) extends Descriptor
case class SingletonDescriptor(simpleName: String, fullName: String, erasure: ScalaType, instance: AnyRef, properties: Seq[PropertyDescriptor]) extends Descriptor

trait ObjectDescriptor extends Descriptor
case class ClassDescriptor(simpleName: String, fullName: String, erasure: ScalaType, companion: Option[SingletonDescriptor], constructors: Seq[ConstructorDescriptor], properties: Seq[PropertyDescriptor]) extends ObjectDescriptor {
//    def bestConstructor(argNames: Seq[String]): Option[ConstructorDescriptor] = {
//      constructors.sortBy(-_.params.size)
//    }
  lazy val mostComprehensive: Seq[ConstructorParamDescriptor] = if (constructors.isEmpty) Seq.empty else constructors.sortBy(-_.params.size).head.params
}
case class PrimitiveDescriptor(simpleName: String, fullName: String, erasure: ScalaType) extends ObjectDescriptor {
}

