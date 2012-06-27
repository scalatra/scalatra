package org

import scalatra.util.RicherString

package object scalatra 
  extends Control // make halt and pass visible to helpers outside the DSL
{
  import util.MultiMap

  type RouteTransformer = (Route => Route)

  @deprecated("Use CsrfTokenSupport", "2.0.0")
  type CSRFTokenSupport = CsrfTokenSupport
  
  type MultiParams = MultiMap

  type Action = () => Any

  type ErrorHandler = PartialFunction[Throwable, Any]

  type ContentTypeInferrer = PartialFunction[Any, String]

  type RenderPipeline = PartialFunction[Any, Any]

  val EnvironmentKey = "org.scalatra.environment"

  val MultiParamsKey = "org.scalatra.MultiParams"
  
//  @deprecated("Use org.scalatra.servlet.ServletBase if you depend on the Servlet API, or org.scalatra.ScalatraApp if you don't.", "2.1.0")
//  type ScalatraKernel = servlet.ServletBase

  private[scalatra] implicit def stringToRicherString(s: String) = new RicherString(s)

  private[scalatra] implicit def extendedByteArray(bytes: Array[Byte]) = new {
    def hexEncode =  ((new StringBuilder(bytes.length * 2) /: bytes) { (sb, b) =>
        if((b.toInt & 0xff) < 0x10) sb.append("0")
        sb.append(Integer.toString(b.toInt & 0xff, 16))
      }).toString
  }

  implicit def appMounter2app(appMounter: AppMounter): Mountable = appMounter.mounted
  implicit def app2AppMounter(app: Mountable): AppMounter = app.mounter
}
