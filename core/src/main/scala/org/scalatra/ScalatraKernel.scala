package org.scalatra

import javax.servlet._
import javax.servlet.http._
import scala.util.DynamicVariable
import scala.util.matching.Regex
import scala.util.control.ControlThrowable
import scala.collection.JavaConversions._
import scala.collection.mutable.{ConcurrentMap, HashMap, ListBuffer, SynchronizedBuffer}
import scala.xml.NodeSeq
import util.io.zeroCopy
import java.io.{File, FileInputStream}
import java.lang.{Integer => JInteger}
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.tailrec
import util.{MultiMap, MapWithIndifferentAccess, MultiMapHeadView, using}

object ScalatraKernel
{
  type MultiParams = MultiMap

  type Action = () => Any

  @deprecated("Use HttpMethods.methods")
  val httpMethods = HttpMethod.methods map { _.toString }

  @deprecated("Use HttpMethods.methods filter { !_.isSafe }")
  val writeMethods = HttpMethod.methods filter { !_.isSafe } map { _.toString }

  @deprecated("Use CsrfTokenSupport.DefaultKey")
  val csrfKey = CsrfTokenSupport.DefaultKey

  val EnvironmentKey = "org.scalatra.environment".intern

  val MultiParamsKey = "org.scalatra.MultiParams".intern
}
import ScalatraKernel._

/**
 * ScalatraKernel is the default implementation of [[org.scalatra.CoreDSL]].
 * It is typically extended by [[org.scalatra.ScalatraServlet]] or
 * [[org.scalatra.ScalatraFilter]] to create a Scalatra application.
 */
trait ScalatraKernel extends ScalatraService with Handler {
  type Request = HttpServletRequest
  type Response = HttpServletResponse

  def handle(req: HttpServletRequest, res: HttpServletResponse) {
    apply(req, res)
  }

  def session = request.getSession

  def sessionOption = Option(request.getSession(false))
}
