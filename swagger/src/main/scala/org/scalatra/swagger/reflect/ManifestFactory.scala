package org.scalatra.swagger.reflect

import java.lang.reflect.{GenericArrayType, ParameterizedType, Type, TypeVariable, WildcardType}

import scala.reflect.Manifest

private[swagger] object ManifestFactory {
  def manifestOf(t: Type): Manifest[?] = t match {

    case pt: ParameterizedType =>
      val clazz    = manifestOf(pt.getRawType).runtimeClass
      val typeArgs = pt.getActualTypeArguments map manifestOf

      if (pt.getOwnerType == null) {
        manifestOf(clazz, typeArgs.toIndexedSeq)
      } else {
        Manifest.classType(manifestOf(pt.getOwnerType), clazz, typeArgs.toIndexedSeq*)
      }

    case at: GenericArrayType =>
      val componentManifest = manifestOf(at.getGenericComponentType)
      val arrayManifest     = componentManifest.arrayManifest // strips component type args off
      Manifest.classType(arrayManifest.runtimeClass, componentManifest)

    case wt: WildcardType =>
      val upper = wt.getUpperBounds
      if (upper != null && upper.nonEmpty) manifestOf(upper(0))
      else manifestOf(classOf[AnyRef])

    case wt: TypeVariable[?] =>
      val upper = wt.getBounds
      if (upper != null && upper.nonEmpty) manifestOf(upper(0))
      else manifestOf(classOf[AnyRef])

    case c: Class[?] => fromClass(c)

  }

  def manifestOf(erasure: Class[?], typeArgs: Seq[Manifest[?]]): Manifest[?] = {
    if (typeArgs.isEmpty) {
      fromClass(erasure)
    } else {
      val normalizedErasure =
        if (erasure.getName == "scala.Array")
          typeArgs(0).arrayManifest.runtimeClass
        else
          erasure

      Manifest.classType(normalizedErasure, typeArgs.head, typeArgs.tail*)
    }
  }

  def manifestOf(st: ScalaType): Manifest[?] = st match {
    case t: ManifestScalaType => t.manifest
    case _ =>
      val typeArgs = st.typeArgs map manifestOf
      manifestOf(st.erasure, typeArgs)
  }

  private def fromClass(clazz: Class[?]): Manifest[?] = clazz match {
    case java.lang.Byte.TYPE      => Manifest.Byte
    case java.lang.Short.TYPE     => Manifest.Short
    case java.lang.Character.TYPE => Manifest.Char
    case java.lang.Integer.TYPE   => Manifest.Int
    case java.lang.Long.TYPE      => Manifest.Long
    case java.lang.Float.TYPE     => Manifest.Float
    case java.lang.Double.TYPE    => Manifest.Double
    case java.lang.Boolean.TYPE   => Manifest.Boolean
    case java.lang.Void.TYPE      => Manifest.Unit
    case _                        => Manifest.classType(clazz)
  }
}
