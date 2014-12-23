package org.scalatra.swagger.reflect

import java.lang.reflect.{ Field, TypeVariable }

sealed trait Descriptor
object ManifestScalaType {
  private val types = new Memo[Manifest[_], ScalaType]

  def apply[T](mf: Manifest[T]): ScalaType = {
    /* optimization */
    if (mf.runtimeClass == classOf[Int] || mf.runtimeClass == classOf[java.lang.Integer]) ManifestScalaType.IntType
    else if (mf.runtimeClass == classOf[Long] || mf.runtimeClass == classOf[java.lang.Long]) ManifestScalaType.LongType
    else if (mf.runtimeClass == classOf[Byte] || mf.runtimeClass == classOf[java.lang.Byte]) ManifestScalaType.ByteType
    else if (mf.runtimeClass == classOf[Short] || mf.runtimeClass == classOf[java.lang.Short]) ManifestScalaType.ShortType
    else if (mf.runtimeClass == classOf[Float] || mf.runtimeClass == classOf[java.lang.Float]) ManifestScalaType.FloatType
    else if (mf.runtimeClass == classOf[Double] || mf.runtimeClass == classOf[java.lang.Double]) ManifestScalaType.DoubleType
    else if (mf.runtimeClass == classOf[BigInt] || mf.runtimeClass == classOf[java.math.BigInteger]) ManifestScalaType.BigIntType
    else if (mf.runtimeClass == classOf[BigDecimal] || mf.runtimeClass == classOf[java.math.BigDecimal]) ManifestScalaType.BigDecimalType
    else if (mf.runtimeClass == classOf[Boolean] || mf.runtimeClass == classOf[java.lang.Boolean]) ManifestScalaType.BooleanType
    else if (mf.runtimeClass == classOf[String] || mf.runtimeClass == classOf[java.lang.String]) ManifestScalaType.StringType
    else if (mf.runtimeClass == classOf[java.util.Date]) ManifestScalaType.DateType
    else if (mf.runtimeClass == classOf[java.sql.Timestamp]) ManifestScalaType.TimestampType
    else if (mf.runtimeClass == classOf[Symbol]) ManifestScalaType.SymbolType
    else if (mf.runtimeClass == classOf[Number]) ManifestScalaType.NumberType
    /* end optimization */
    else {
      if (mf.typeArguments.isEmpty) types(mf, new ManifestScalaType(_))
      else new ManifestScalaType(mf)
      //      if (!mf.runtimeClass.isArray) types(mf, new ScalaType(_))
      //      else {
      //        val nmf = ManifestFactory.manifestOf(mf.runtimeClass, List(ManifestFactory.manifestOf(mf.runtimeClass.getComponentType)))
      //        types(nmf, new ScalaType(_))
      //      }
      //      new ScalaType(mf)
    }
  }

  def apply(erasure: Class[_], typeArgs: Seq[ScalaType] = Seq.empty): ScalaType = {
    val mf = ManifestFactory.manifestOf(erasure, typeArgs.map(ManifestFactory.manifestOf(_)))
    ManifestScalaType(mf)
  }

  private val IntType: ScalaType = new PrimitiveManifestScalaType(Manifest.Int)
  private val NumberType: ScalaType = new PrimitiveManifestScalaType(manifest[Number])
  private val LongType: ScalaType = new PrimitiveManifestScalaType(Manifest.Long)
  private val ByteType: ScalaType = new PrimitiveManifestScalaType(Manifest.Byte)
  private val ShortType: ScalaType = new PrimitiveManifestScalaType(Manifest.Short)
  private val BooleanType: ScalaType = new PrimitiveManifestScalaType(Manifest.Boolean)
  private val FloatType: ScalaType = new PrimitiveManifestScalaType(Manifest.Float)
  private val DoubleType: ScalaType = new PrimitiveManifestScalaType(Manifest.Double)
  private val StringType: ScalaType = new PrimitiveManifestScalaType(manifest[java.lang.String])
  private val SymbolType: ScalaType = new PrimitiveManifestScalaType(manifest[Symbol])
  private val BigDecimalType: ScalaType = new PrimitiveManifestScalaType(manifest[BigDecimal])
  private val BigIntType: ScalaType = new PrimitiveManifestScalaType(manifest[BigInt])
  private val DateType: ScalaType = new PrimitiveManifestScalaType(manifest[java.util.Date])
  private val TimestampType: ScalaType = new PrimitiveManifestScalaType(manifest[java.sql.Timestamp])

  private class PrimitiveManifestScalaType(mf: Manifest[_]) extends ManifestScalaType(mf) {
    override val isPrimitive = true
  }
  private class CopiedManifestScalaType(
      mf: Manifest[_],
      private[this] var _typeVars: Map[TypeVariable[_], ScalaType],
      override val isPrimitive: Boolean) extends ManifestScalaType(mf) {
    override def typeVars = {
      if (_typeVars == null)
        _typeVars = Map.empty[TypeVariable[_], ScalaType] ++
          erasure.getTypeParameters.map(_.asInstanceOf[TypeVariable[_]]).toList.zip(manifest.typeArguments map (ManifestScalaType(_)))
      _typeVars
    }
  }
}

trait ScalaType extends Equals {
  def erasure: Class[_]
  def typeArgs: Seq[ScalaType]
  def typeVars: Map[TypeVariable[_], ScalaType]
  def isArray: Boolean
  def rawFullName: String
  def rawSimpleName: String
  def simpleName: String
  def fullName: String
  def isPrimitive: Boolean
  def isMap: Boolean
  def isCollection: Boolean
  def isOption: Boolean
  def <:<(that: ScalaType): Boolean
  def >:>(that: ScalaType): Boolean
  def copy(erasure: Class[_] = erasure, typeArgs: Seq[ScalaType] = typeArgs, typeVars: Map[TypeVariable[_], ScalaType] = typeVars): ScalaType
}

class ManifestScalaType(val manifest: Manifest[_]) extends ScalaType {

  import org.scalatra.swagger.reflect.ManifestScalaType.{ CopiedManifestScalaType, types }
  private[this] val self = this
  val erasure: Class[_] = manifest.runtimeClass

  //  private[this] var _typeArgs: Seq[ScalaType] = null
  //  def typeArgs: Seq[ScalaType] = {
  //    if (_typeArgs == null)
  //      _typeArgs = manifest.typeArguments.map(ta => Reflector.scalaTypeOf(ta)) ++ (
  //        if (erasure.isArray) List(Reflector.scalaTypeOf(erasure.getComponentType)) else Nil
  //      )
  //    _typeArgs
  //  }

  val typeArgs = manifest.typeArguments.map(ta => Reflector.scalaTypeOf(ta)) ++ (
    if (erasure.isArray) List(Reflector.scalaTypeOf(erasure.getComponentType)) else Nil
  )

  private[this] var _typeVars: Map[TypeVariable[_], ScalaType] = null
  def typeVars = {
    if (_typeVars == null)
      _typeVars = Map.empty ++
        erasure.getTypeParameters.map(_.asInstanceOf[TypeVariable[_]]).toList.zip(manifest.typeArguments map (ManifestScalaType(_)))
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
  def <:<(that: ScalaType): Boolean = that match {
    case t: ManifestScalaType => manifest <:< t.manifest
    case _ => manifest <:< ManifestFactory.manifestOf(that)
  }
  def >:>(that: ScalaType): Boolean = that match {
    case t: ManifestScalaType => manifest >:> t.manifest
    case _ => manifest >:> ManifestFactory.manifestOf(that)
  }

  override def hashCode(): Int = manifest.##

  override def equals(obj: Any): Boolean = obj match {
    case a: ManifestScalaType => manifest == a.manifest
    case _ => false
  }

  def canEqual(that: Any): Boolean = that match {
    case s: ManifestScalaType => manifest.canEqual(s.manifest)
    case _ => false
  }

  def copy(erasure: Class[_] = erasure, typeArgs: Seq[ScalaType] = typeArgs, typeVars: Map[TypeVariable[_], ScalaType] = _typeVars): ScalaType = {
    /* optimization */
    if (erasure == classOf[Int] || erasure == classOf[java.lang.Integer]) ManifestScalaType.IntType
    else if (erasure == classOf[Long] || erasure == classOf[java.lang.Long]) ManifestScalaType.LongType
    else if (erasure == classOf[Byte] || erasure == classOf[java.lang.Byte]) ManifestScalaType.ByteType
    else if (erasure == classOf[Short] || erasure == classOf[java.lang.Short]) ManifestScalaType.ShortType
    else if (erasure == classOf[Float] || erasure == classOf[java.lang.Float]) ManifestScalaType.FloatType
    else if (erasure == classOf[Double] || erasure == classOf[java.lang.Double]) ManifestScalaType.DoubleType
    else if (erasure == classOf[BigInt] || erasure == classOf[java.math.BigInteger]) ManifestScalaType.BigIntType
    else if (erasure == classOf[BigDecimal] || erasure == classOf[java.math.BigDecimal]) ManifestScalaType.BigDecimalType
    else if (erasure == classOf[Boolean] || erasure == classOf[java.lang.Boolean]) ManifestScalaType.BooleanType
    else if (erasure == classOf[String] || erasure == classOf[java.lang.String]) ManifestScalaType.StringType
    else if (erasure == classOf[java.util.Date]) ManifestScalaType.DateType
    else if (erasure == classOf[java.sql.Timestamp]) ManifestScalaType.TimestampType
    else if (erasure == classOf[Symbol]) ManifestScalaType.SymbolType
    else if (erasure == classOf[Number]) ManifestScalaType.NumberType
    /* end optimization */
    else {
      val mf = ManifestFactory.manifestOf(erasure, typeArgs.map(ManifestFactory.manifestOf(_)))
      val st = new CopiedManifestScalaType(mf, typeVars, isPrimitive)
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

