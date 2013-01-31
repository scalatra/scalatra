package org.scalatra.swagger.reflect

import java.{util => jutil}
import java.lang.reflect.{Type, TypeVariable, ParameterizedType, Modifier}
import scala.util.control.Exception._
import collection.JavaConverters._
import java.util.Date
import java.sql.Timestamp
import org.json4s.ScalaSigReader

object Reflector {

  private[this] val rawClasses = new Memo[Type, Class[_]]
  private[this] val unmangledNames = new Memo[String, String]
  private[this] val types = new Memo[Type, ScalaType]
  private[this] val descriptors = new Memo[ScalaType, Descriptor]

  private[this] val primitives = {
      Set[Type](classOf[String], classOf[Char], classOf[Int], classOf[Long], classOf[Double],
        classOf[Float], classOf[Byte], classOf[BigInt], classOf[Boolean],
        classOf[Short], classOf[java.lang.Integer], classOf[java.lang.Long],
        classOf[java.lang.Double], classOf[java.lang.Float], classOf[java.lang.Character],
        classOf[java.lang.Byte], classOf[java.lang.Boolean], classOf[Number],
        classOf[java.lang.Short], classOf[Date], classOf[Timestamp], classOf[Symbol], classOf[Unit])
  }

  def isPrimitive(t: Type) = primitives contains t

  def scalaTypeOf[T](implicit mf: Manifest[T]): ScalaType = {
    ScalaType(mf)
//    types(mf.erasure, _ => ScalaType(mf))
  }

  def scalaTypeOf(clazz: Class[_]): ScalaType = {
    val mf = ManifestFactory.manifestOf(clazz)
//    types(mf.erasure, _ => ScalaType(mf))
    ScalaType(mf)
  }

  def scalaTypeOf(t: Type): ScalaType = {
    val mf = ManifestFactory.manifestOf(t)
//    types(mf.erasure, _ => ScalaType(mf))
    ScalaType(mf)
  }

  def scalaTypeOf(name: String): Option[ScalaType] = Reflector.resolveClass[AnyRef](name, ClassLoaders) map (c => scalaTypeOf(c))

  def describe[T](implicit mf: Manifest[T]): Descriptor = describe(scalaTypeOf[T])

  def describe(clazz: Class[_]): Descriptor = describe(scalaTypeOf(clazz))

  def describe(fqn: String): Option[Descriptor] =
    Reflector.scalaTypeOf(fqn) map (describe(_: ScalaType))

  def describe(st: ScalaType): Descriptor = {
    if (st.isCollection || st.isOption) describe(st.typeArgs.head)
    descriptors(st, Reflector.createClassDescriptor)
  }


  def resolveClass[X <: AnyRef](c: String, classLoaders: Iterable[ClassLoader]): Option[Class[X]] = classLoaders match {
    case Nil => sys.error("resolveClass: expected 1+ classloaders but received empty list")
    case List(cl) => Some(Class.forName(c, true, cl).asInstanceOf[Class[X]])
    case many => {
      try {
        var clazz: Class[_] = null
        val iter = many.iterator
        while (clazz == null && iter.hasNext) {
          try {
            clazz = Class.forName(c, true, iter.next())
          }
          catch {
            case e: ClassNotFoundException => // keep going, maybe it's in the next one
          }
        }

        if (clazz != null) Some(clazz.asInstanceOf[Class[X]]) else None
      }
      catch {
        case _: Throwable => None
      }
    }
  }

  private[reflect] def createClassDescriptor(tpe: ScalaType): Descriptor = {



    //    val sig = ScalaSigReader.findScalaSig(tpe.erasure).getOrElse(Meta.fail("Can't find ScalaSig for " + tpe.fullName))
    //    val sym = ScalaSigReader.findClass(sig, tpe.erasure).getOrElse(Meta.fail("Can't find " + tpe.fullName + " from parsed ScalaSig"))
    //    val children = sym.children
    //    val ctorChildren =
    //      children.filter(c => c.isCaseAccessor && !c.isPrivate).map(_.asInstanceOf[MethodSymbol]).zipWithIndex map {
    //        case (ms, idx) =>
    //          ConstructorParamDescriptor(unmangleName(ms.name), ms.name, idx, null, )
    //      }


    def properties: Seq[PropertyDescriptor] = {
      def fields(clazz: Class[_]): List[PropertyDescriptor] = {
        val lb = new jutil.LinkedList[PropertyDescriptor]().asScala
        val ls = clazz.getDeclaredFields.toIterator
        while (ls.hasNext) {
          val f = ls.next()
          if (!Modifier.isStatic(f.getModifiers) || !Modifier.isTransient(f.getModifiers) || !Modifier.isPrivate(f.getModifiers)) {
            val st = ScalaType(f.getType, f.getGenericType match {
              case p: ParameterizedType => p.getActualTypeArguments map (c => scalaTypeOf(c))
              case _ => Nil
            }, Map.empty)
            val decoded = unmangleName(f.getName)
            f.setAccessible(true)
            lb += PropertyDescriptor(decoded, f.getName, st, f)
          }
        }
        if (clazz.getSuperclass != null)
          lb ++= fields(clazz.getSuperclass)
        lb.toList
      }
      fields(tpe.erasure)
    }

    def constructors(companion: Option[SingletonDescriptor]): Seq[ConstructorDescriptor] = {
      tpe.erasure.getConstructors.toSeq map {
        ctor =>
          val ctorParameterNames = ParanamerReader.lookupParameterNames(ctor)
          val genParams = Vector(ctor.getGenericParameterTypes: _*)
          val ctorParams = ctorParameterNames.zipWithIndex map {
            cp =>
              val decoded = unmangleName(cp._1)
              val default = companion flatMap {
                comp =>
                  defaultValue(comp.erasure.erasure, comp.instance, cp._2)
              }
              val theType = genParams(cp._2) match {
                case v: TypeVariable[_] =>
                  val a = tpe.typeVars.getOrElse(v, scalaTypeOf(v))
                  if (a.erasure == classOf[java.lang.Object])
                    scalaTypeOf(ScalaSigReader.readConstructor(cp._1, tpe.erasure, cp._2, ctorParameterNames.toList))
                  else a
                case x => scalaTypeOf(x)
              }
              ConstructorParamDescriptor(decoded, cp._1, cp._2, theType, default)
          }
          ConstructorDescriptor(ctorParams.toSeq, ctor, isPrimary = false)
      }
    }

    if (tpe.isPrimitive) PrimitiveDescriptor(tpe.simpleName, tpe.fullName, tpe)
    else {
      val path = if (tpe.rawFullName.endsWith("$")) tpe.rawFullName else "%s$".format(tpe.rawFullName)
      val c = resolveClass(path, Vector(getClass.getClassLoader))
      val companion = c map {
        cl =>
          SingletonDescriptor(cl.getSimpleName, cl.getName, scalaTypeOf(cl), cl.getField(ModuleFieldName).get(null), Seq.empty)
      }
      ClassDescriptor(tpe.simpleName, tpe.fullName, tpe, companion, constructors(companion), properties)
    }
  }

  def defaultValue(compClass: Class[_], compObj: AnyRef, argIndex: Int) = {
    allCatch.withApply(_ => None) {
      Option(compClass.getMethod("%s$%d".format(ConstructorDefault, argIndex + 1))) map {
        meth => () => meth.invoke(compObj)
      }
    }
  }

  def rawClassOf(t: Type): Class[_] = rawClasses(t, _ match {
    case c: Class[_] => c
    case p: ParameterizedType =>
      rawClassOf(p.getRawType)
    case x => sys.error("Raw type of " + x + " not known")
  })

  def unmangleName(name: String) =
    unmangledNames(name, scala.reflect.NameTransformer.decode)


}