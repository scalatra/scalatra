package org.scalatra
package json

import org.json4s._
import util._
import io._
import org.scalatra.ActionResult
import java.io.{FileInputStream, File}
import org.scalatra.ActionResult
import xml.{Node, NodeSeq}

trait JValueResult extends ScalatraBase { self: JsonSupport[_] =>

  implicit protected def jsonFormats: Formats

  override protected def renderPipeline: RenderPipeline = renderToJson orElse super.renderPipeline

  // should the result value be converted to a JValue?
  private[this] def isJValueResponse = responseFormat == "json" || responseFormat == "xml"

  private[this] def customSerializer = jsonFormats.customSerializer

  // TODO re-use Formats function
  // TODO cache results
  private[this] def fieldSerializer(clazz: Class[_]): Option[FieldSerializer[_]] = {
    import ClassDelta._

    val ord = Ordering[Int].on[(Class[_], FieldSerializer[_])](x => delta(x._1, clazz))
    jsonFormats.fieldSerializers filter (_._1.isAssignableFrom(clazz)) match {
      case Nil => None
      case xs  => Some((xs min ord)._2)
    }
  }

  private[this] object ClassDelta {
    def delta(class1: Class[_], class2: Class[_]): Int = {
      if (class1 == class2) 0
      else if (class1.getInterfaces.contains(class2)) 0
      else if (class2.getInterfaces.contains(class1)) 0
      else if (class1.isAssignableFrom(class2)) {
        1 + delta(class1, class2.getSuperclass)
      }
      else if (class2.isAssignableFrom(class1)) {
        1 + delta(class1.getSuperclass, class2)
      }
      else sys.error("Don't call delta unless one class is assignable from the other")
    }
  }

  private[this] def renderToJson: RenderPipeline = {
    case JNull | JNothing =>
    case a: JValue => super.renderPipeline(a)
    case status: Int => super.renderPipeline(status)
    case bytes: Array[Byte] => super.renderPipeline(bytes)
    case is: java.io.InputStream => super.renderPipeline(is)
    case file: File => super.renderPipeline(file)
    case a: ActionResult => super.renderPipeline(a)
    case _: Unit | Unit | null =>
    case s: String => super.renderPipeline(s)
    case x: scala.xml.Node if responseFormat == "xml" ⇒
      contentType = formats("xml")
      response.writer.write(scala.xml.Utility.trim(x).toString())
    case x: NodeSeq if responseFormat == "xml" ⇒
      contentType = formats("xml")
      response.writer.write(x.toString)
    case x: NodeSeq ⇒
      response.writer.write(x.toString)
    case a: Any if isJValueResponse && customSerializer.isDefinedAt(a) =>
      customSerializer.lift(a) match {
        case Some(jv: JValue) => jv
        case None => super.renderPipeline(a)
      }
    case a: Any if isJValueResponse && fieldSerializer(a.getClass).isDefined =>
      Extraction.decompose(a)
    case p: Product if isJValueResponse => Extraction.decompose(p)
    case p: TraversableOnce[_] if isJValueResponse => Extraction.decompose(p)
  }

}