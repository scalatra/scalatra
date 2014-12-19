package org.scalatra
package json

import xml.{ NodeSeq, XML }
import io.Codec
import java.io.{ Writer, StringWriter, PrintWriter }
import org.json4s._
import org.json4s.Xml._
import text.Document

object JsonOutput {
  val VulnerabilityPrelude = ")]}',\n"
  val RosettaPrelude = "/**/"
}

trait JsonOutput[T] extends ApiFormats with JsonMethods[T] {

  import JsonOutput._
  /**
   * If a request is made with a parameter in jsonpCallbackParameterNames it will
   * be assumed that it is a JSONP request and the json will be returned as the
   * argument to a function with the name specified in the corresponding parameter.
   *
   * By default no parameterNames will be checked
   */
  def jsonpCallbackParameterNames: Iterable[String] = Nil

  /**
   * Whether or not to apply the jsonVulnerabilityGuard when rendering json.
   * @see http://haacked.com/archive/2008/11/20/anatomy-of-a-subtle-json-vulnerability.aspx
   */
  protected def jsonVulnerabilityGuard = false

  /**
   * Whether or not to apply the rosetta flash guard when rendering jsonp callbacks.
   * @see http://miki.it/blog/2014/7/8/abusing-jsonp-with-rosetta-flash/
   */
  protected def rosettaFlashGuard = true

  protected lazy val xmlRootNode = <resp></resp>

  protected def transformResponseBody(body: JValue) = body

  override protected def renderPipeline = ({

    case JsonResult(jv) => jv

    case jv: JValue if format == "xml" =>
      contentType = formats("xml")
      writeJsonAsXml(transformResponseBody(jv), response.writer)

    case jv: JValue =>
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
          if (rosettaFlashGuard) writer.write("/**/")
          writer.write("%s(%s);".format(some, compact(render(transformResponseBody(jv)))))
        case _ =>
          contentType = formats("json")
          if (jsonVulnerabilityGuard) writer.write(VulnerabilityPrelude)
          writeJson(transformResponseBody(jv), writer)
          ()
      }
  }: RenderPipeline) orElse super.renderPipeline

  protected def writeJsonAsXml(json: JValue, writer: Writer) {
    if (json != JNothing)
      XML.write(
        response.writer,
        xmlRootNode.copy(child = toXml(json)),
        response.characterEncoding.get,
        xmlDecl = true,
        doctype = null)
  }

  protected def writeJson(json: JValue, writer: Writer)
}
