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
  override protected[scalatra] def addRoute(protocol: String, routeMatchers: Iterable[RouteMatcher], action: => Any): Unit =
    Routes(protocol) = new FileUploadRoute(routeMatchers, () => action) :: Routes(protocol)

  class FileUploadRoute(routeMatchers: Iterable[RouteMatcher], action: ScalatraKernel.Action) extends Route(routeMatchers, action) {
    override def apply(realPath: String): Option[Any] = RouteMatcher.matchRoute(routeMatchers) flatMap { invokeAction(_) }

    private def invokeAction(routeParams: ScalatraKernel.MultiParams) = {
      val (multipartFileParams, multipartFormParams) = extractMultipartParams
      
      _multiParams.withValue(multiParams ++ routeParams ++ multipartFormParams) {
        _fileMultiParams.withValue(multipartFileParams) {
          try {
            Some(action.apply())
          }
          catch {
            case e: PassException => None
          }
        }
      }
    }

    def extractMultipartParams : (Map[String, List[FileItem]], Map[String, List[String]]) =
      if (ServletFileUpload.isMultipartContent(request)) {
        val upload = new ServletFileUpload(fileItemFactory)
        val items = upload.parseRequest(request).asInstanceOf[JList[FileItem]]
        items.foldRight((Map[String, List[FileItem]](), Map[String, List[String]]())) { (item, acc) =>
          val (fileMap, formMap) = acc
          if (item.isFormField)
            (fileMap, formMap + ((item.getFieldName, item.getString :: formMap.getOrElse(item.getFieldName, List[String]()))))
          else
            (fileMap + ((item.getFieldName, item :: fileMap.getOrElse(item.getFieldName, List[FileItem]()))), formMap)
        }
      } else (Map(), Map())
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