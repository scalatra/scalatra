package org.scalatra
package json

import xml.{NodeSeq, XML}
import io.Codec
import java.io.PrintWriter

object JsonOutput {
  val VulnerabilityPrelude = ")]}',\n"
}
trait JsonOutput extends ApiFormats {

  protected type JsonType
  protected def jsonClass: Class[_]

  import JsonOutput._
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


  override protected def renderPipeline = ({
    case j if jsonClass.isAssignableFrom(j.getClass) && format == "xml" =>
      val jv = j.asInstanceOf[JsonType]
      contentType = formats("xml")
      XML.write(response.writer, xmlRootNode.copy(child = writeJsonAsXml(jv)), response.characterEncoding.get, xmlDecl = true, null)

    case j if jsonClass.isAssignableFrom(j.getClass) =>
      val jv = j.asInstanceOf[JsonType]
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
          writeJson(jv, writer)
          writer write ");"
        case _ =>
          contentType = formats("json")
          if(jsonVulnerabilityGuard) writer.write(VulnerabilityPrelude)
          writeJson(jv, writer)
          ()
      }
  }: RenderPipeline) orElse super.renderPipeline

  protected def writeJsonAsXml(json: JsonType): NodeSeq
  protected def writeJson(json: JsonType, writer: PrintWriter)
}

