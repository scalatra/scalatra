package com.thinkminimo.step

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.collection.mutable.HashSet
import scala.collection.jcl.Conversions._
import scala.xml.NodeSeq

object StepKernel
{
  type MultiParams = Map[String, Seq[String]]
  type Action = () => Any

  val protocols = List("GET", "POST", "PUT", "DELETE")
}
import StepKernel._

trait StepKernel
{
  protected val Routes      = Map(protocols map (_ -> new HashSet[Route]): _*)

  protected def contentType = response.getContentType
  protected def contentType_=(value: String): Unit = response.setContentType(value)

  protected val defaultCharacterEncoding = "UTF-8"
  private val _response   = new DynamicVariable[HttpServletResponse](null)
  private val _request    = new DynamicVariable[HttpServletRequest](null)

  protected implicit def requestWrapper(r: HttpServletRequest) = RichRequest(r)
  protected implicit def sessionWrapper(s: HttpSession) = new RichSession(s)

  protected class Route(val path: String, val action: Action) {
    val pattern = """:\w+"""
    val names = new Regex(pattern) findAllIn path toList
    val re = new Regex("^%s$" format path.replaceAll(pattern, "(.*?)"))

    def apply(realPath: String): Option[MultiParams] =
      re findFirstMatchIn realPath map (x => Map(names zip (x.subgroups map { Seq(_) })  : _*))

    override def toString() = path
  }

  protected def handle(request: HttpServletRequest, response: HttpServletResponse) {
    // As default, the servlet tries to decode params with ISO_8859-1.
    // It causes an EOFException if params are actually encoded with the other code (such as UTF-8)
    if (request.getCharacterEncoding == null)
      request.setCharacterEncoding(defaultCharacterEncoding)

    val realMultiParams = request.getParameterMap.asInstanceOf[java.util.Map[String,Array[String]]]

    def isMatchingRoute(route: Route) = {
      def exec(args: MultiParams) = {
        _multiParams.withValue(args ++ realMultiParams) {
          renderResponse(route.action())
        }
      }
      route(requestPath) map exec isDefined
    }

    response.setCharacterEncoding(defaultCharacterEncoding)

    _request.withValue(request) {
      _response.withValue(response) {
        _multiParams.withValue(Map() ++ realMultiParams) {
          doBefore()
          if (Routes(request.getMethod) find isMatchingRoute isEmpty)
            renderResponse(doNotFound())
        }
      }
    }
  }

  protected def requestPath: String

  private var doBefore: () => Unit = { () => () }
  protected def before(fun: => Any) = doBefore = { () => fun; () }

  protected var doNotFound: Action
  protected def notFound(fun: => Any) = doNotFound = { () => fun }

  protected def renderResponse(actionResult: Any) {
    if (contentType == null)
      contentType = inferContentType(actionResult)
    renderResponseBody(actionResult)
  }

  protected def inferContentType(actionResult: Any): String = actionResult match {
    case _: NodeSeq => "text/html"
    case _: Array[Byte] => "application/octet-stream"
    case _ => "text/plain"
  }

  protected def renderResponseBody(actionResult: Any) {
    actionResult match {
      case bytes: Array[Byte] =>
        response.getOutputStream.write(bytes)
      case _: Unit =>
        // If an action returns Unit, it assumes responsibility for the response
      case x: Any  =>
        response.getWriter.print(x.toString)
	  }
  }

  private val _multiParams = new DynamicVariable[MultiParams](Map()) {
    // Whenever we set _multiParams, set a view for _params as well
    override def withValue[S](newval: MultiParams)(thunk: => S) = {
      super.withValue(newval) {
        _params.withValue(newval transform { (k, v) => v.first }) {
          thunk
        }
      }
    }
  }
  protected def multiParams: MultiParams = (_multiParams.value).withDefaultValue(Seq.empty)

  private val _params = new DynamicVariable[Map[String, String]](Map())
  protected def params = _params value

  protected def redirect(uri: String) = (_response value) sendRedirect uri
  protected def request = _request value
  protected def response = _response value
  protected def session = request.getSession
  protected def sessionOption = request.getSession(false) match {
    case s: HttpSession => Some(s)
    case null => None
  }
  protected def status(code: Int) = (_response value) setStatus code

  protected val List(get, post, put, delete) = protocols map routeSetter

  // functional programming means never having to repeat yourself
  private def routeSetter(protocol: String): (String) => (=> Any) => Unit = {
    def g(path: String, fun: => Any) = Routes(protocol) += new Route(path, () => fun)
    (g _).curry
  }
}
