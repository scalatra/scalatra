package org.scalatra.swagger.reflect

import tools.scalap.scalax.rules.scalasig._
import annotation.tailrec

private[reflect] object ScalaSigReader {
  def readConstructor(argName: String, clazz: ScalaType, typeArgIndex: Int, argNames: List[String]): Class[_] = {
    val cl = findClass(clazz.erasure)
    val cstr = findConstructor(cl, argNames).getOrElse(sys.error("Can't find constructor for " + clazz))
    findArgType(cstr, argNames.indexOf(argName), typeArgIndex)
  }

  def readConstructor(argName: String, clazz: ScalaType, typeArgIndexes: List[Int], argNames: List[String]): Class[_] = {
    val cl = findClass(clazz.erasure)
    val cstr = findConstructor(cl, argNames).getOrElse(sys.error("Can't find constructor for " + clazz))
    findArgType(cstr, argNames.indexOf(argName), typeArgIndexes)
  }

  def findArgType(s: MethodSymbol, argIdx: Int, typeArgIndex: Int): Class[_] = {
    def findPrimitive(t: Type): Symbol = {
      t match {
        case TypeRefType(ThisType(_), symbol, _) if isPrimitive(symbol) => symbol
        case TypeRefType(_, _, TypeRefType(ThisType(_), symbol, _) :: xs) => symbol
        case TypeRefType(_, symbol, Nil) => symbol
        case TypeRefType(_, _, args) if typeArgIndex >= args.length =>
          findPrimitive(args(0))
        case TypeRefType(_, _, args) =>
          val ta = args(typeArgIndex)
          ta match {
            case ref @ TypeRefType(_, _, _) => findPrimitive(ref)
            case x => sys.error("Unexpected type info " + x)
          }
        case x => sys.error("Unexpected type info " + x)
      }
    }
    toClass(findPrimitive(s.children(argIdx).asInstanceOf[SymbolInfoSymbol].infoType))
  }

  private[this] def toClass(s: Symbol) = s.path match {
    case "scala.Short"   => classOf[Short]
    case "scala.Int"     => classOf[Int]
    case "scala.Long"    => classOf[Long]
    case "scala.Boolean" => classOf[Boolean]
    case "scala.Float"   => classOf[Float]
    case "scala.Double"  => classOf[Double]
    case "scala.Byte"    => classOf[Byte]
    case _               => classOf[AnyRef]
  }
  private[this] def isPrimitive(s: Symbol) = toClass(s) != classOf[AnyRef]
  def findArgType(s: MethodSymbol, argIdx: Int, typeArgIndexes: List[Int]): Class[_] = {
    @tailrec def findPrimitive(t: Type, curr: Int): Symbol = {
      val ii = (typeArgIndexes.length - 1) min curr
      t match {
        case TypeRefType(ThisType(_), symbol, _) if isPrimitive(symbol) => symbol
        case TypeRefType(_, symbol, Nil) => symbol
        case TypeRefType(_, _, args) if typeArgIndexes(ii) >= args.length =>
          findPrimitive(args(0 max args.length - 1), curr + 1)
        case TypeRefType(_, _, args) =>
          val ta = args(typeArgIndexes(ii))
          ta match {
            case ref @ TypeRefType(_, _, _) => findPrimitive(ref, curr + 1)
            case x => sys.error("Unexpected type info " + x)
          }
        case x => sys.error("Unexpected type info " + x)
      }
    }
    toClass(findPrimitive(s.children(argIdx).asInstanceOf[SymbolInfoSymbol].infoType, 0))
  }

  def findConstructor(c: ClassSymbol, argNames: List[String]): Option[MethodSymbol] = {
    val ms = c.children collect {
      case m: MethodSymbol if m.name == "<init>" => m
    }
    ms.find(m => m.children.map(_.name) == argNames)
  }

  def findClass(clazz: Class[_]): ClassSymbol = {
    val sig = findScalaSig(clazz).getOrElse(sys.error("Can't find ScalaSig for " + clazz))
    findClass(sig, clazz).getOrElse(sys.error("Can't find " + clazz + " from parsed ScalaSig"))
  }

  def findClass(sig: ScalaSig, clazz: Class[_]): Option[ClassSymbol] = {
    sig.symbols.collect { case c: ClassSymbol if !c.isModule => c }.find(_.name == clazz.getSimpleName).orElse {
      sig.topLevelClasses.find(_.symbolInfo.name == clazz.getSimpleName).orElse {
        sig.topLevelObjects.map { obj =>
          val t = obj.infoType.asInstanceOf[TypeRefType]
          t.symbol.children collect { case c: ClassSymbol => c } find(_.symbolInfo.name == clazz.getSimpleName)
        }.head
      }
    }
  }

  def findScalaSig(clazz: Class[_]): Option[ScalaSig] =
    parseClassFileFromByteCode(clazz).orElse(findScalaSig(clazz.getDeclaringClass))

  private[this] def parseClassFileFromByteCode(clazz: Class[_]): Option[ScalaSig] = try {
    // taken from ScalaSigParser parse method with the explicit purpose of walking away from NPE
    val byteCode = ByteCode.forClass(clazz)
    Option(ClassFileParser.parse(byteCode)) flatMap ScalaSigParser.parse
  } catch {
    case e: NullPointerException => None // yes, this is the exception, but it is totally unhelpful to the end user
  }
}