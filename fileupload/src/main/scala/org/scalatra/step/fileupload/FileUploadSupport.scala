package org.scalatra.fileupload

import org.scalatra.Handler
import org.apache.commons.fileupload.servlet.ServletFileUpload
import org.apache.commons.fileupload.{FileItemFactory, FileItem}
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import collection.JavaConversions._
import util.DynamicVariable
import java.util.{List => JList, HashMap => JHashMap}
import javax.servlet.http.{HttpServletRequestWrapper, HttpServletRequest, HttpServletResponse}

trait FileUploadSupport extends Handler {
  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    if (ServletFileUpload.isMultipartContent(req)) {
      val upload = new ServletFileUpload(fileItemFactory)
      val items = upload.parseRequest(req).asInstanceOf[JList[FileItem]]
      val (fileMap, formMap) = items.foldRight((Map[String, List[FileItem]](), Map[String, List[String]]())) { (item, acc) =>
        val (fileMap, formMap) = acc
        if (item.isFormField)
          (fileMap, formMap + ((item.getFieldName, item.getString :: formMap.getOrElse(item.getFieldName, List[String]()))))
        else
          (fileMap + ((item.getFieldName, item :: fileMap.getOrElse(item.getFieldName, List[FileItem]()))), formMap)
      }
      _fileMultiParams.withValue(fileMap) {
        super.handle(wrapRequest(req, formMap), res)
      }
    } else {
      _fileMultiParams.withValue(Map()) {
        super.handle(req, res)
      }
    }
  }

  private def wrapRequest(req: HttpServletRequest, formMap: Map[String, Seq[String]]) =
    new HttpServletRequestWrapper(req) {
      override def getParameter(name: String) = formMap.get(name) map { _.head } getOrElse null
      override def getParameterNames = formMap.keysIterator
      override def getParameterValues(name: String) = formMap.get(name) map { _.toArray } getOrElse null
      override def getParameterMap = new JHashMap[String, Array[String]] ++ (formMap transform { (k, v) => v.toArray })
    } 

  protected def fileItemFactory: FileItemFactory = new DiskFileItemFactory

  private val _fileMultiParams = new DynamicVariable[Map[String, Seq[FileItem]]](Map())
  protected def fileMultiParams: Map[String, Seq[FileItem]] = (_fileMultiParams.value).withDefaultValue(Seq.empty)

  protected val _fileParams = new collection.Map[String, FileItem] {
    def get(key: String) = fileMultiParams.get(key) flatMap { _.headOption }
    override def size = fileMultiParams.size
    override def iterator = fileMultiParams map { case(k, v) => (k, v.head) } iterator
    override def -(key: String) = Map() ++ this - key
    override def +[B1 >: FileItem](kv: (String, B1)) = Map() ++ this + kv
  }
  protected def fileParams = _fileParams
}