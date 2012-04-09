package org.scalatra.fileupload

import scala.collection.JavaConversions._
import javax.servlet.annotation.MultipartConfig
import javax.servlet.http.HttpServletRequest
import org.scalatra.servlet.{ServletRequest, ServletBase}
import java.util.{HashMap => JHashMap, Map => JMap}

@MultipartConfig
trait FileUploadSupport extends ServletBase {
  import FileUploadSupport._

  override def handle(req: RequestT, res: ResponseT) {
    val req2 = try {
      if (isMultipartRequest(req)) {
        val bodyParams       = extractMultipartParams(req)
        val mergedFormParams = mergeFormParamsWithQueryString(req, bodyParams)

        wrapRequest(req, mergedFormParams)
      } else req
    } catch {
      case e: Exception => req
    }

    super.handle(req2, res)
  }

  private def isMultipartRequest(req: RequestT): Boolean = {
    val isPostOrPut = Set("POST", "PUT").contains(req.getMethod)

    isPostOrPut && (req.contentType match {
      case Some(contentType) => contentType.startsWith("multipart/")
      case _                 => false
    })
  }

  private def extractMultipartParams(req: RequestT): BodyParams = {
    req.get(BodyParamsKey).asInstanceOf[Option[BodyParams]] match {
      case Some(bodyParams) =>
        bodyParams

      case None => {
        val bodyParams = req.getParts.foldRight(BodyParams(FileMultiParams(), Map.empty)) { (part, params) =>
          val item  = FileItem(part)

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

  private def fileItemToString(item: FileItem): String = {
    val charset = item.charset.getOrElse(defaultCharacterEncoding)
    new String(item.get(), charset)
  }

  private def mergeFormParamsWithQueryString(req: RequestT, bodyParams: BodyParams): Map[String, List[String]] = {
    var mergedParams = bodyParams.formParams
    req.getParameterMap.asInstanceOf[JMap[String, Array[String]]] foreach {
      case (name, values) =>
        val formValues = mergedParams.getOrElse(name, List.empty)
        mergedParams += name -> (values.toList ++ formValues)
    }

    mergedParams
  }

  private def wrapRequest(req: HttpServletRequest, formMap: Map[String, Seq[String]]) = {
    val wrapped = new ServletRequest(req) {
      override def getParameter(name: String) = formMap.get(name) map { _.head } getOrElse null
      override def getParameterNames = formMap.keysIterator
      override def getParameterValues(name: String) = formMap.get(name) map { _.toArray } getOrElse null
      override def getParameterMap = new JHashMap[String, Array[String]] ++ (formMap transform { (k, v) => v.toArray })
    }
    wrapped
  }

  protected def fileMultiParams: FileMultiParams = extractMultipartParams(request).fileParams

  protected val _fileParams = new collection.Map[String, FileItem] {
    def get(key: String) = fileMultiParams.get(key) flatMap { _.headOption }
    override def size = fileMultiParams.size
    override def iterator = (fileMultiParams map {
      case (k, v) => (k, v.head)
    }).iterator
    override def -(key: String) = Map() ++ this - key
    override def +[B1 >: FileItem](kv: (String, B1)) = Map() ++ this + kv
  }

  /**
   * @return a Map, keyed on the names of multipart file upload parameters,
   *          of all multipart files submitted with the request
   */
  def fileParams = _fileParams
}

object FileUploadSupport {
  private val BodyParamsKey = "org.scalatra.fileupload.bodyParams"
  case class BodyParams(fileParams: FileMultiParams, formParams: Map[String, List[String]])
}