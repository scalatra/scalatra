package org.scalatra
package fileupload

import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.{FileItemFactory, FileItem}
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import collection.JavaConversions._
import scala.util.DynamicVariable
import java.util.{List => JList, HashMap => JHashMap}
import javax.servlet.http.{HttpServletRequestWrapper, HttpServletRequest, HttpServletResponse}
import collection.Iterable
import java.lang.String

trait FileUploadSupport extends ScalatraKernel {
  import FileUploadSupport._

  override def handle(req: HttpServletRequest, resp: HttpServletResponse) {
    val bodyParams = extractMultipartParams(req)
    super.handle(wrapRequest(req, bodyParams.formParams), resp)
  }

  private def extractMultipartParams(req: HttpServletRequest): BodyParams =
    // First look for it cached on the request, because we can't parse it twice.  See GH-16.
    req.get(BodyParamsKey).asInstanceOf[Option[BodyParams]] match {
      case Some(bodyParams) =>
        bodyParams
      case None =>
        val bodyParams =
          if (ServletFileUpload.isMultipartContent(req)) {
            val upload = new ServletFileUpload(fileItemFactory)
            val items = upload.parseRequest(req).asInstanceOf[JList[FileItem]]
            items.foldRight(BodyParams(Map.empty, Map.empty)) { (item, params) =>
              if (item.isFormField)
                BodyParams(params.fileParams, params.formParams + ((item.getFieldName, item.getString :: params.formParams.getOrElse(item.getFieldName, List[String]()))))
              else
                BodyParams(params.fileParams + ((item.getFieldName, item :: params.fileParams.getOrElse(item.getFieldName, List[FileItem]()))), params.formParams)
              }
          }
          else
            BodyParams(Map.empty, Map.empty)
        req(BodyParamsKey) = bodyParams
        bodyParams
    }

  private def wrapRequest(req: HttpServletRequest, formMap: Map[String, Seq[String]]) =
    new HttpServletRequestWrapper(req) {
      override def getParameter(name: String) = formMap.get(name) map { _.head } getOrElse null
      override def getParameterNames = formMap.keysIterator
      override def getParameterValues(name: String) = formMap.get(name) map { _.toArray } getOrElse null
      override def getParameterMap = new JHashMap[String, Array[String]] ++ (formMap transform { (k, v) => v.toArray })
    }

  protected def fileItemFactory: FileItemFactory = new DiskFileItemFactory

  protected def fileMultiParams: FileMultiParams = extractMultipartParams(request).fileParams

  protected val _fileParams = new collection.Map[String, FileItem] {
    def get(key: String) = fileMultiParams.get(key) flatMap { _.headOption }
    override def size = fileMultiParams.size
    override def iterator = fileMultiParams map { case(k, v) => (k, v.head) } iterator
    override def -(key: String) = Map() ++ this - key
    override def +[B1 >: FileItem](kv: (String, B1)) = Map() ++ this + kv
  }
  protected def fileParams = _fileParams
}

object FileUploadSupport {
  case class BodyParams(fileParams: FileMultiParams, formParams: Map[String, List[String]])
  type FileMultiParams = Map[String, List[FileItem]]
  private val BodyParamsKey = "org.scalatra.fileupload.bodyParams"
}


