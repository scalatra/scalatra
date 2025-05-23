package org.scalatra.servlet

import java.io.{InputStream, File, FileOutputStream}
import java.util.{Map as JMap, HashMap as JHashMap}
import org.scalatra.ServletCompat.*
import org.scalatra.ServletCompat.http.*

import org.scalatra.ScalatraBase
import org.scalatra.util.RicherString.*
import org.scalatra.util.*
import org.scalatra.util.io.*

import scala.jdk.CollectionConverters.*

/** FileUploadSupport can be mixed into a [[org.scalatra.ScalatraFilter]] or [[org.scalatra.ScalatraServlet]] to provide
  * easy access to data submitted as part of a multipart HTTP request. Commonly this is used for retrieving uploaded
  * files.
  *
  * Once the trait has been mixed into your handler, you need to enable multipart configuration in your ''web.xml'' or
  * by using `@MultipartConfig` annotation. To configure in ''web.xml'' add `<multipart-config />` to your `<servlet>`
  * element. If you prefer annotations instead, place `@MultipartConfig` to your handler. Both ways provide some further
  * configuration options, such as specifying the max total request size and max size for invidual files in the request.
  * You might want to set these to prevent users from uploading too large files.
  *
  * When the configuration has been done, you can access any files using `fileParams("myFile")` where ''myFile'' is the
  * name of the parameter used to upload the file being retrieved. If you are expecting multiple files with the same
  * name, you can use `fileMultiParams("files[]")` to access them all.
  *
  * To handle any errors that are caused by multipart handling, you need to configure an error handler to your handler
  * class:
  *
  * {{{
  * import org.scalatra.servlet.SizeLimitExceededException
  * import org.scalatra.servlet.FileUploadSupport
  *
  * @MultipartConfig(maxFileSize=1024*1024)
  * class FileEaterServlet extends ScalatraServlet with FileUploadSupport {
  *   error {
  *     case e: SizeConstraintExceededException => "Oh, too much! Can't take it all."
  *     case e: IOException                     => "Server denied me my meal, thanks anyway."
  *   }
  *
  *   post("/eatfile") {
  *     "Thanks! You just provided me " + fileParams("lunch").size + " bytes for a lunch."
  *   }
  * }
  * }}}
  *
  * }}* @note Once any handler with FileUploadSupport has accessed the request, the fileParams returned by
  * FileUploadSupport will remain fixed for the lifetime of the request.
  *
  * @note
  *   Will not work on Jetty versions prior to 8.1.3. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=376324. The old
  *   scalatra-fileupload module still works for earlier versions of Jetty.
  */
trait FileUploadSupport extends ServletBase with HasMultipartConfig {

  import org.scalatra.servlet.FileUploadSupport.*

  /* Called for any exceptions thrown by handling file uploads
   * to detect whether it signifies a too large file being
   * uploaded or a too large request in general.
   *
   * This can be overriden if the container being used has an
   * different behavior than Jetty, which throws either
   * `IllegalStateException` (Jetty 10) or
   * `ServletException` (Jetty 12) in case of an error.
   */
  protected def isSizeConstraintException(e: Exception): Boolean = e match {
    // Jetty 10: see "org.eclipse.jetty.server.MultiPartFormInputStream.MultiPart::write".
    case exc: IllegalStateException => exc.getMessage.matches("^Multipart Mime part .+ exceeds max filesize$")
    // Jetty 12: see "org.eclipse.jetty.ee10.servlet.ServletApiRequest::getParts".
    case exc: ServletException =>
      val rootCause = exc.getRootCause
      rootCause.getMessage == "400: bad multipart" && (rootCause.getCause match {
        case exc2: java.util.concurrent.CompletionException =>
          exc2.getCause match {
            case exc3: IllegalStateException => exc3.getMessage.matches("^max file size exceeded: \\d+$")
            case _                           => false
          }
        case _ => false
      })
    case _ => false
  }

  override def handle(req: HttpServletRequest, res: HttpServletResponse): Unit = {
    val req2 =
      try {
        if (isMultipartRequest(req)) {
          val bodyParams       = extractMultipartParams(req)
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
      case _                 => false
    })
  }

  private def extractMultipartParams(req: HttpServletRequest): BodyParams = {
    req.get(BodyParamsKey).asInstanceOf[Option[BodyParams]] match {
      case Some(bodyParams) =>
        bodyParams

      case None => {
        val bodyParams = getParts(req).foldRight(BodyParams(FileMultiParams(), Map.empty)) { (part, params) =>
          val item = FileItem(part)

          if (!(item.isFormField)) {
            BodyParams(
              params.fileParams + ((
                item.getFieldName,
                item +: params.fileParams.getOrElse(item.getFieldName, List[FileItem]())
              )),
              params.formParams
            )
          } else {
            BodyParams(params.fileParams, params.formParams)
          }
        }

        req.setAttribute(BodyParamsKey, bodyParams)
        bodyParams
      }
    }
  }

  private def getParts(req: HttpServletRequest): Iterable[Part] = {
    try {
      if (isMultipartRequest(req)) req.getParts.asScala else Seq.empty[Part]
    } catch {
      case e: Exception if isSizeConstraintException(e) =>
        throw new SizeConstraintExceededException("Too large request or file", e)
    }
  }

  private def mergeFormParamsWithQueryString(
      req: HttpServletRequest,
      bodyParams: BodyParams
  ): Map[String, List[String]] = {
    var mergedParams = bodyParams.formParams
    req.getParameterMap.asScala foreach { case (name, values) =>
      val formValues = mergedParams.getOrElse(name, List.empty)
      mergedParams += name -> (values.toList ++ formValues)
    }

    mergedParams
  }

  private def wrapRequest(req: HttpServletRequest, formMap: Map[String, Seq[String]]): HttpServletRequestWrapper = {
    val wrapped = new HttpServletRequestWrapper(req) {
      override def getParameter(name: String): String = formMap
        .get(name)
        .map {
          _.head
        }
        .orNull

      override def getParameterNames: java.util.Enumeration[String] = formMap.keysIterator.asJavaEnumeration

      override def getParameterValues(name: String): Array[String] = formMap
        .get(name)
        .map {
          _.toArray
        }
        .orNull

      override def getParameterMap: JMap[String, Array[String]] = {
        (new JHashMap[String, Array[String]].asScala ++ (formMap transform { (k, v) =>
          v.toArray
        })).asJava
      }
    }
    wrapped
  }

  def fileMultiParams(implicit request: HttpServletRequest): FileMultiParams = {
    extractMultipartParams(request).fileParams
  }

  def fileMultiParams(key: String)(implicit request: HttpServletRequest): Seq[FileItem] = {
    fileMultiParams(using request)(key)
  }

  /** @return
    *   a Map, keyed on the names of multipart file upload parameters, of all multipart files submitted with the request
    */
  def fileParams(implicit request: HttpServletRequest): FileSingleParams = {
    new FileSingleParams(fileMultiParams(using request))
  }

  def fileParams(key: String)(implicit request: HttpServletRequest): FileItem = {
    fileParams(using request)(key)
  }
}

object FileUploadSupport {

  private val BodyParamsKey = "org.scalatra.fileupload.bodyParams"

  case class BodyParams(fileParams: FileMultiParams, formParams: Map[String, List[String]])

}

class FileMultiParams(wrapped: Map[String, Seq[FileItem]] = Map.empty) {

  def apply(key: String): Seq[FileItem] = get(key) match {
    case Some(v) => v
    case None    => throw new NoSuchElementException(s"Key ${key} not found")
  }

  def get(key: String): Option[Seq[FileItem]] = {
    (wrapped.get(key) orElse wrapped.get(key + "[]"))
  }

  def getOrElse(key: String, default: => Seq[FileItem]): Seq[FileItem] =
    get(key) getOrElse default

  def foreach(f: ((String, Seq[FileItem])) => Unit): Unit =
    wrapped.foreach { case ((k, v)) => f((k, v)) }

  def +[B1 >: Seq[FileItem]](kv: (String, B1)): FileMultiParams =
    new FileMultiParams(wrapped + kv.asInstanceOf[(String, Seq[FileItem])])

  def -(key: String): FileMultiParams = new FileMultiParams(wrapped - key)

  def iterator: Iterator[(String, Seq[FileItem])] = wrapped.iterator

  def toMap: Map[String, Seq[FileItem]] = iterator.toMap
}

object FileMultiParams {

  def apply(): FileMultiParams = new FileMultiParams

  def apply[SeqType <: Seq[FileItem]](wrapped: Map[String, Seq[FileItem]]): FileMultiParams = {
    new FileMultiParams(wrapped)
  }

}

class FileSingleParams(wrapped: FileMultiParams = FileMultiParams()) {

  def apply(key: String): FileItem = get(key) match {
    case Some(v) => v
    case None    => throw new NoSuchElementException(s"Key ${key} not found")
  }

  def get(key: String): Option[FileItem] = {
    wrapped.get(key) map { _.head }
  }

  def foreach(f: ((String, FileItem)) => Unit): Unit =
    wrapped.foreach { case ((k, v)) => f((k, v.head)) }

  def iterator(): Iterator[(String, Seq[FileItem])] = wrapped.iterator

}

case class FileItem(part: Part) {

  val size: Long                  = part.getSize
  val fieldName: String           = part.getName
  val name: String                = FileItemUtil.partAttribute(part, "content-disposition", "filename")
  val contentType: Option[String] = part.getContentType.blankOption
  val charset: Option[String]     = FileItemUtil.partAttribute(part, "content-type", "charset").blankOption

  def getName: String = name

  def getFieldName: String = fieldName

  def getSize: Long = size

  def getContentType: Option[String] = contentType.orElse(null)

  def getCharset: Option[String] = charset.orElse(null)

  def write(file: File): Unit = {
    using(new FileOutputStream(file)) { out =>
      io.copy(getInputStream, out)
    }
  }

  def write(fileName: String): Unit = {
    part.write(fileName)
  }

  def get(): Array[Byte] = org.scalatra.util.io.readBytes(getInputStream)

  def isFormField: Boolean = (name == null)

  def getInputStream: InputStream = part.getInputStream
}

private object FileItemUtil {

  def partAttribute(part: Part, headerName: String, attributeName: String, defaultValue: String = null): String = {
    Option(part.getHeader(headerName)) match {
      case Some(value) => {
        value.split(";").find(_.trim().startsWith(attributeName)) match {
          case Some(attributeValue) =>
            attributeValue.substring(attributeValue.indexOf('=') + 1).trim().replace("\"", "")
          case _ => defaultValue
        }
      }
      case _ => defaultValue
    }
  }

}
