package org.scalatra
package liftjson

import net.liftweb.json._
import scala.io.Codec.UTF8
import net.liftweb.json.Xml.toXml
import xml.XML
import xml.Utility.trimProper
import io.Codec

@deprecated("Use LiftJsonSupport instead", "2.1.0")
trait JsonSupport extends LiftJsonOutput

object LiftJsonOutput {
  val VulnerabilityPrelude = ")]}',\n"
}
private[liftjson] trait LiftJsonOutput extends ApiFormats {

  import LiftJsonOutput._
  /**
   * If a request is made with a parameter in jsonpCallbackParameterNames it will
   * be assumed that it is a JSONP request and the json will be returned as the
   * argument to a function with the name specified in the corresponding parameter.
   *
   * By default no parameterNames will be checked
   */
  def jsonpCallbackParameterNames:  Iterable[String] = Nil

  /**
   * Whether or not to apply the jsonVulnerabilityGuard when rendering json.
   * @see http://haacked.com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx
   */
  protected lazy val jsonVulnerabilityGuard = false

  protected lazy val xmlRootNode = <resp></resp>

  // override protected def contentTypeInferrer = ({
  //   case _ : JValue => "application/json; charset=utf-8"
  // }: ContentTypeInferrer) orElse super.contentTypeInferrer


  override protected def renderPipeline = ({

    case jv: JValue if format == "xml" =>
      contentType = formats("xml")
      XML.write(response.writer, xmlRootNode.copy(child = toXml(jv)), response.characterEncoding.get, xmlDecl = true, null)

    case jv : JValue =>
      // JSON is always UTF-8
      response.characterEncoding = Some(Codec.UTF8.name)
      val writer = response.writer

      val jsonpCallback = for {
        paramName <- jsonpCallbackParameterNames
        callback <- params.get(paramName)
      } yield callback

      jsonpCallback match {
        case some :: _ =>
          // JSONP is not JSON, but JavaScript.
          contentType = formats("js")
          // Status must always be 200 on JSONP, since it's loaded in a <script> tag.
          status = 200
          writer write some
          writer write '('
          Printer.compact(render(jv), writer)
          writer write ");"
        case _ =>
          contentType = formats("json")
          if(jsonVulnerabilityGuard) writer.write(VulnerabilityPrelude)
          Printer.compact(render(jv), writer)
          ()
      }
  }: RenderPipeline) orElse super.renderPipeline
}
