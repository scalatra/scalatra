package org.scalatra.servlet

import scala.collection.JavaConverters._
import java.util.{HashMap => JHashMap, Map => JMap}
import org.scalatra.ScalatraBase
import org.scalatra.util._
import java.io.{ File, FileOutputStream }
import javax.servlet.http._
import org.scalatra.util.RicherString._
import scala.io.Codec
import org.scalatra.util.io

/** FileUploadSupport can be mixed into a [[org.scalatra.ScalatraFilter]]
  * or [[org.scalatra.ScalatraServlet]] to provide easy access to data
  * submitted as part of a multipart HTTP request.  Commonly this is used for
  * retrieving uploaded files.
  *
  * Once the trait has been mixed into your handler, you need to enable multipart
  * configuration in your ''web.xml'' or by using `@MultipartConfig` annotation. To
  * configure in ''web.xml'' add `<multipart-config />` to your `<servlet>` element. If you
  * prefer annotations instead, place `@MultipartConfig` to your handler. Both ways
  * provide some further configuration options, such as specifying the max total request size
  * and max size for invidual files in the request. You might want to set these to prevent
  * users from uploading too large files.
  *
  * When the configuration has been done, you can access any files using
  * `fileParams("myFile")` where ''myFile'' is the name
  * of the parameter used to upload the file being retrieved. If you are
  * expecting multiple files with the same name, you can use
  * `fileMultiParams("files[]")` to access them all.
  *
  * To handle any errors that are caused by multipart handling, you need
  * to configure an error handler to your handler class:
  *
  * {{{
  * import org.scalatra.servlet.SizeLimitExceededException
  * import org.scalatra.servlet.FileUploadSupport
  *
  * @MultipartConfig(maxFileSize=1024*1024)
  * class FileEaterServlet extends ScalatraServlet with FileUploadSupport {
  *   error {
  *     case e: SizeConstrainttExceededException => "Oh, too much! Can't take it all."
  *     case e: IOException                      => "Server denied me my meal, thanks anyway."
  *   }
  *
  *   post("/eatfile") {
  *     "Thanks! You just provided me " + fileParams("lunch").size + " bytes for a lunch."
  *   }
  * }
  * }}}
  *
  }}* @note Once any handler with FileUploadSupport has accessed the request, the
  *       fileParams returned by FileUploadSupport will remain fixed for the
  *       lifetime of the request.
  *
  * @note Will not work on Jetty versions prior to 8.1.3.  See
  * https://bugs.eclipse.org/bugs/show_bug.cgi?id=376324.  The old
  * scalatra-fileupload module still works for earlier versions
  * of Jetty.
  */
trait FileUploadSupport extends ServletBase with HasMultipartConfig {

  import FileUploadSupport._

  /* Called for any exceptions thrown by handling file uploads
   * to detect whether it signifies a too large file being
   * uploaded or a too large request in general.
   *
   * This can be overriden for the container being used if it
   * doesn't throw `IllegalStateException` or if it throws
   * `IllegalStateException` for some other reason.
   */
  protected def isSizeConstraintException(e: Exception) = e match {
    case _: IllegalStateException => true
    case _ => false
  }

  override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    val req2 = try {
      if (isMultipartRequest(req)) {
        val bodyParams = extractMultipartParams(req)
        val mergedFormParams = mergeFormParamsWithQueryString(req, bodyParams)

        wrapRequest(req, mergedFormParams)
      } else req
    } catch {
      case e: Exception => {
        req.setAttribute(ScalatraBase.PrehandleExceptionKey, e)
        req
      }
    }

    super.handle(req2, res)
  }

  private def isMultipartRequest(req: HttpServletRequest): Boolean = {
    val isPostOrPut = Set("POST", "PUT", "PATCH").contains(req.getMethod)
    isPostOrPut && (req.contentType match {
      case Some(contentType) => contentType.startsWith("multipart/")
      case _ => false
    })
  }

  private def extractMultipartParams(req: HttpServletRequest): BodyParams = {
    req.get(BodyParamsKey).asInstanceOf[Option[BodyParams]] match {
      case Some(bodyParams) =>
        bodyParams

      case None => {
        val bodyParams = getParts(req).foldRight(BodyParams(FileMultiParams(), Map.empty)) {
          (part, params) =>
            val item = FileItem(part)

            if (!(item.isFormField)) {
              BodyParams(params.fileParams + ((
                item.getFieldName, item +: params.fileParams.getOrElse(item.getFieldName, List[FileItem]())
                )), params.formParams)
            } else {
              BodyParams(params.fileParams, params.formParams + (
                (item.getFieldName, fileItemToString(item) ::
                  params.formParams.getOrElse(item.getFieldName, List[String]())
                  )
                ))
            }
        }

        req.setAttribute(BodyParamsKey, bodyParams)
        bodyParams
      }
    }
  }

  private def getParts(req: HttpServletRequest) = {
    try {
      if (isMultipartRequest(req)) req.getParts.asScala else Seq.empty[Part]
    } catch {
      case e: Exception if isSizeConstraintException(e) => throw new SizeConstraintExceededException("Too large request or file", e)
    }
  }

  private def fileItemToString(item: FileItem): String = {
    val charset = item.charset getOrElse defaultCharacterEncoding
    new String(item.get(), charset)
  }

  private def mergeFormParamsWithQueryString(req: HttpServletRequest, bodyParams: BodyParams): Map[String, List[String]] = {
    var mergedParams = bodyParams.formParams
    req.getParameterMap.asScala foreach {
      case (name, values) =>
        val formValues = mergedParams.getOrElse(name, List.empty)
        mergedParams += name -> (values.toList ++ formValues)
    }

    mergedParams
  }

  private def wrapRequest(req: HttpServletRequest, formMap: Map[String, Seq[String]]) = {
    val wrapped = new HttpServletRequestWrapper(req) {
      override def getParameter(name: String) = formMap.get(name) map {
        _.head
      } getOrElse null

      override def getParameterNames = formMap.keysIterator.asJavaEnumeration

      override def getParameterValues(name: String) = formMap.get(name) map {
        _.toArray
      } getOrElse null

      override def getParameterMap = (new JHashMap[String, Array[String]].asScala ++ (formMap transform {
        (k, v) => v.toArray
      })).asJava
    }
    wrapped
  }

  def fileMultiParams(implicit request: HttpServletRequest): FileMultiParams = extractMultipartParams(request).fileParams
  def fileMultiParams(key: String)(implicit request: HttpServletRequest): Seq[FileItem] = fileMultiParams(request)(key)

  /**
   * @return a Map, keyed on the names of multipart file upload parameters,
   *         of all multipart files submitted with the request
   */
  def fileParams(implicit request: HttpServletRequest) = new MultiMapHeadView[String, FileItem] {
    protected def multiMap = fileMultiParams
  }

  def fileParams(key: String)(implicit request: HttpServletRequest): FileItem = fileParams(request)(key)
}

object FileUploadSupport {
  private val BodyParamsKey = "org.scalatra.fileupload.bodyParams"

  case class BodyParams(fileParams: FileMultiParams, formParams: Map[String, List[String]])

}

class FileMultiParams(wrapped: Map[String, Seq[FileItem]] = Map.empty) extends Map[String, Seq[FileItem]] {

  def get(key: String): Option[Seq[FileItem]] = {
    (wrapped.get(key) orElse wrapped.get(key + "[]"))
  }

  def get(key: Symbol): Option[Seq[FileItem]] = get(key.name)

  def +[B1 >: Seq[FileItem]](kv: (String, B1)) =
    new FileMultiParams(wrapped + kv.asInstanceOf[(String, Seq[FileItem])])

  def -(key: String) = new FileMultiParams(wrapped - key)

  def iterator = wrapped.iterator

  override def default(a: String): Seq[FileItem] = wrapped.default(a)
}

object FileMultiParams {
  def apply() = new FileMultiParams

  def apply[SeqType <: Seq[FileItem]](wrapped: Map[String, Seq[FileItem]]) =
    new FileMultiParams(wrapped)
}

case class FileItem(part: Part) {
  val size = part.getSize
  val fieldName = part.getName
  val name = Util.partAttribute(part, "content-disposition", "filename")
  val contentType: Option[String] = part.getContentType.blankOption
  val charset: Option[String] = Util.partAttribute(part, "content-type", "charset").blankOption

  def getName = name

  def getFieldName = fieldName

  def getSize = size

  def getContentType = contentType.orElse(null)

  def getCharset = charset.orElse(null)

  def write(file: File) {
    using(new FileOutputStream(file)) { out =>
      io.copy(getInputStream, out)
    }
  }

  def write(fileName: String) {
    part.write(fileName)
  }

  def get() = org.scalatra.util.io.readBytes(getInputStream)

  def isFormField = (name == null)

  def getInputStream = part.getInputStream
}

object Util {
  def partAttribute(part: Part,
                    headerName: String, attributeName: String,
                    defaultValue: String = null) = Option(part.getHeader(headerName)) match {
    case Some(value) => {
      value.split(";").find(_.trim().startsWith(attributeName)) match {
        case Some(attributeValue) => attributeValue.substring(attributeValue.indexOf('=') + 1).trim().replace("\"", "")
        case _ => defaultValue
      }
    }

    case _ => defaultValue
  }
}
