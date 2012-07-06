package org.scalatra

import _root_.akka.actor.ActorSystem
import _root_.akka.util.Duration
import _root_.akka.util.duration._
import util.io.{PathManipulationOps, MimeTypes}
import java.util.concurrent.ConcurrentHashMap
import collection.JavaConverters._
import collection.{Map, mutable}
import java.net.URL
import com.google.common.collect.MapMaker

object AppContext {
  val Production = "production"
  val Development = "development"
  val Staging = "staging"
  val Test = "test"

  private[this] val cloader = getClass.getClassLoader
  private val environment = readEnvironmentKey(System.err.println _)

  private def readEnvironmentKey(failWith: String ⇒ Unit = _ ⇒ null) = {
    (ep("SCALATRA_MODE") orElse sp("scalatra.mode") orElse ep("AKKA_MODE") orElse sp("akka.mode")) getOrElse {
      val inferred = "development"
      failWith("no environment found, defaulting to: " + inferred)
      inferred
    }
  }

  def findInResources(fileName: String, ext: String) = {
    val un = sys.props("user.name")
    if (cloader.getResource("%s.%s.%s.%s".format(fileName, un, environment, ext)) != null)
      Some("%s.%s.%s.%s".format(fileName, un, environment, ext))
    else if (cloader.getResource("%s.%s.%s".format(fileName, un, ext)) != null)
      Some("%s.%s.%s".format(fileName, un, ext))
    else if (cloader.getResource("%s.%s.%s" format (fileName, environment, ext)) != null)
      Some("%s.%s.%s" format (fileName, environment, ext))
    else if (cloader.getResource("%s.%s" format (fileName, ext)) != null)
      Some("%s.%s" format (fileName, ext))
    else None
  }

  private def sp(key: String) = {
    sys.props get key filter (_.nonBlank)
  }
  private def ep(key: String) = {
    sys.env get key filter (_.nonBlank)
  }

}
trait AppContext extends ScalatraLogging {

  implicit def applications: AppMounter.ApplicationRegistry
  def server: ServerInfo

  lazy val attributes: mutable.ConcurrentMap[String, Any] = new MapMaker().makeMap[String, Any].asScala

  lazy val mimes = new MimeTypes

  import AppContext._
  val mode = environment
  def isProduction = isEnvironment(Production)
  def isDevelopment = isEnvironment(Development)
  def isStaging = isEnvironment(Staging)
  def isTest = isEnvironment(Test)
  def isEnvironment(env: String) = mode equalsIgnoreCase env

  var sessionIdKey = "JSESSIONID"
  var sessionTimeout: Duration = 20.minutes

  private[scalatra] val actorSystem = ActorSystem("scalatra")

  def get(key: String) = attributes.get(key)
  def apply(key: String) = attributes(key)
  def update(key: String, value: Any) = { attributes(key) = value; this }

  def sessions: SessionStore[_ <: HttpSession]

  def application(req: HttpRequest): Option[ScalatraApp] = {
    logger.debug("The registered applications:")
    logger.debug("%s" format applications)
    application(req.uri.getPath) map (_.mounted) flatMap {
      case f: ScalatraApp if f.hasMatchingRoute(req) => {
        logger.debug("We found an App")
        Some(f)
      }
      case f: ScalatraApp => {
        logger.debug("We found an App, But no matching route")
        Some(f)
      }
      case _ => {
        logger.debug("No matching route")
        None
      }
    }
  }

  def application(path: String): Option[AppMounter] = {
    val parts = path.split("/")
    if (parts.length == 0) applications get "/"
    else {
      var i = 1
      var curr = "" / parts(i)
      var next: Option[AppMounter] = applications get curr
      var app: Option[AppMounter] = applications get "/"
      while (app.isDefined && next.isDefined) {
        i += 1
        app = next
        next = if(i < parts.length) {
          curr = curr / parts(i)
          applications get curr
        } else None
      }
      app
    }
  }

  def +=(kv: (String, Any)) = {
    attributes += kv
    this
  }

  def -=(key: String) = {
    attributes -= key
    this
  }

  def iterator: Iterator[(String, Any)] = attributes.iterator

  def resourceFor(path: String): URL

  def physicalPath(uri: String): String
}

abstract class AppContextBase(
             val server: ServerInfo,
             val applications: AppMounter.ApplicationRegistry) extends AppContext {

  implicit val appContext = this

  protected def absolutizePath(path: String) =
    PathManipulationOps.ensureSlash(if (path.startsWith("/")) path else server.base / path)

  val sessions: SessionStore[_ <: HttpSession] = server.sessions.map(_.store) getOrElse (new NoopSessionStore)
  sessions.initialize(this)
}