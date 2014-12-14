package org.scalatra
package fileupload

import servlet._

import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.disk.{ DiskFileItem, DiskFileItemFactory }
import collection.JavaConversions._
import scala.util.DynamicVariable
import java.util.{ List => JList, HashMap => JHashMap, Map => JMap }
import javax.servlet.http.{ HttpServletRequestWrapper, HttpServletRequest, HttpServletResponse }
import collection.Iterable
import java.lang.String
import org.apache.commons.fileupload.{ FileUploadException, FileUploadBase, FileItemFactory, FileItem }

/**
 * FileUploadSupport can be mixed into a [[org.scalatra.ScalatraFilter]] or [[org.scalatra.ScalatraServlet]] to provide easy access to data submitted
 * as part of a multipart HTTP request.  Commonly this is used for retrieving uploaded files.
 *
 * Once the trait has been mixed into your handler you can access any files uploaded using {{{ fileParams("myFile") }}} where ''myFile'' is the name
 * of the parameter used to upload the file being retrieved.
 *
 * @note Once any handler with FileUploadSupport has accessed the request, the fileParams returned by FileUploadSupport will remain fixed for
 * the lifetime of the request.
 */
@deprecated(message = "Deprecated in favor of Servlet 3.0 API's multipart features. " +
  "Please use org.scalatra.servlet.FileUploadSupport instead.",
  since = "2.1.0")
trait FileUploadSupport extends ServletBase {
  import FileUploadSupport._

  override def handle(req: HttpServletRequest, resp: HttpServletResponse) {
    val req2 = try {
      if (isMultipartContent(req)) {
        val bodyParams = extractMultipartParams(req)
        var mergedParams = bodyParams.formParams
        // Add the query string parameters
        req.getParameterMap foreach {
          case (name, values) =>
            val formValues = mergedParams.getOrElse(name, List.empty)
            mergedParams += name -> (values.toList ++ formValues)
        }
        wrapRequest(req, mergedParams)
      } else req
    } catch {
      case e: FileUploadException => {
        req.setAttribute(ScalatraBase.PrehandleExceptionKey, e)
        req
      }
    }

    super.handle(req2, resp)
  }

  /*
    ServletFileUpload.isMultipartContent only detects POST requests that have
    Content-Type header starting with "multipart/" as multipart content. This
    allows PUT requests to be also considered as multipart content.
  */
  private def isMultipartContent(req: HttpServletRequest) = {
    val isPostOrPut = Set("POST", "PUT").contains(req.getMethod)

    isPostOrPut && (req.contentType match {
      case Some(contentType) => contentType.startsWith(FileUploadBase.MULTIPART)
      case _ => false
    })
  }

  private def extractMultipartParams(req: HttpServletRequest): BodyParams =
    // First look for it cached on the request, because we can't parse it twice.  See GH-16.
    req.get(BodyParamsKey).asInstanceOf[Option[BodyParams]] match {
      case Some(bodyParams) =>
        bodyParams
      case None =>
        val upload = newServletFileUpload
        val items = upload.parseRequest(req).asInstanceOf[JList[FileItem]]
        val bodyParams = items.foldRight(BodyParams(FileMultiParams(), Map.empty)) { (item, params) =>
          if (item.isFormField)
            BodyParams(params.fileParams, params.formParams + ((item.getFieldName, fileItemToString(req, item) :: params.formParams.getOrElse(item.getFieldName, List[String]()))))
          else
            BodyParams(params.fileParams + ((item.getFieldName, item +: params.fileParams.getOrElse(item.getFieldName, List[FileItem]()))), params.formParams)
        }
        req(BodyParamsKey) = bodyParams
        bodyParams
    }

  /**
   * Converts a file item to a string.
   *
   * Browsers tend to be sloppy about providing content type headers with
   * charset information to form-data parts.  Worse, browsers are
   * inconsistent about how they encode these parameters.
   *
   * The default implementation attempts to use the charset specified on
   * the request.  If that is unspecified, and it usually isn't, then it
   * falls back to the kernel's charset.
   */
  protected def fileItemToString(req: HttpServletRequest, item: FileItem): String = {
    val charset = item match {
      case diskItem: DiskFileItem =>
        // Why doesn't FileItem have this method???
        Option(diskItem.getCharSet())
      case _ =>
        None
    }
    item.getString(charset getOrElse defaultCharacterEncoding)
  }

  private def wrapRequest(req: HttpServletRequest, formMap: Map[String, Seq[String]]) = {
    val wrapped = new HttpServletRequestWrapper(req) {
      override def getParameter(name: String) = formMap.get(name) map { _.head } getOrElse null
      override def getParameterNames = formMap.keysIterator
      override def getParameterValues(name: String) = formMap.get(name) map { _.toArray } getOrElse null
      override def getParameterMap = new JHashMap[String, Array[String]] ++ (formMap transform { (k, v) => v.toArray })
    }
    wrapped
  }

  /**
   * Creates a new file upload handler to parse the request.  By default, it
   * creates a `ServletFileUpload` instance with the file item factory
   * returned by the `fileItemFactory` method.  Override this method to
   * customize properties such as the maximum file size, progress listener,
   * etc.
   *
   * @return a new file upload handler.
   */
  protected def newServletFileUpload: ServletFileUpload =
    new ServletFileUpload(fileItemFactory)

  /**
   * The file item factory used by the default implementation of
   * `newServletFileUpload`.  By default, we use a DiskFileItemFactory.
   */
  /*
   * [non-scaladoc] This method predates newServletFileUpload.  If I had it 
   * to do over again, we'd have that instead of this.  Oops.
   */
  protected def fileItemFactory: FileItemFactory = new DiskFileItemFactory

  protected def fileMultiParams: FileMultiParams = extractMultipartParams(request).fileParams

  protected val _fileParams = new collection.Map[String, FileItem] {
    def get(key: String) = fileMultiParams.get(key) flatMap { _.headOption }
    override def size = fileMultiParams.size
    override def iterator = (fileMultiParams map { case (k, v) => (k, v.head) }).iterator
    override def -(key: String) = Map() ++ this - key
    override def +[B1 >: FileItem](kv: (String, B1)) = Map() ++ this + kv
  }

  /** @return a Map, keyed on the names of multipart file upload parameters, of all multipart files submitted with the request */
  def fileParams = _fileParams
}

object FileUploadSupport {
  case class BodyParams(fileParams: FileMultiParams, formParams: Map[String, List[String]])
  private val BodyParamsKey = "org.scalatra.fileupload.bodyParams"
}

