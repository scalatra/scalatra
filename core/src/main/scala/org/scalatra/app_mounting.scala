package org.scalatra

import java.net.URI
import collection.mutable.ConcurrentMap
import collection.JavaConverters._
import util.io.PathManipulation
import java.util.concurrent.ConcurrentHashMap
import scalax.file.ImplicitConverters._

trait Mountable extends PathManipulation with Initializable {
  @volatile private[scalatra] var mounter: AppMounter = _
  def basePath = mounter.basePath
  def pathName = mounter.pathName
  implicit def appContext: AppContext = mounter.appContext

  def isEmpty: Boolean
  def isDefined: Boolean = !isEmpty
  def hasMatchingRoute(req: HttpRequest): Boolean

  def destroy() {}
}

trait NullMountable extends Mountable {

  def isEmpty = true

  def initialize(config: AppContext) {}
  def hasMatchingRoute(req: HttpRequest) = false
}
case object NullMountable extends NullMountable

trait AppMounterLike extends PathManipulation { self: ScalatraLogging =>
  implicit def appContext: AppContext

  def applications: AppMounter.ApplicationRegistry = this.appContext.applications
  def get(path: String): Option[AppMounter] = this.appContext.application(normalizePath(path))
  def apply(path: String): AppMounter = (applications get normalizePath(path)).get
  def apply(path: URI): AppMounter = apply(normalizePath(path.getRawPath))

  protected def ensureApps(base: String): AppMounter = {
    val pth = normalizePath(base)
    applications.get(pth) getOrElse {
      val (parent, _) = splitPaths(pth)
      val app = if (pth == "/") {
        new AppMounter("/", "", NullMountable)
      } else {
        ensureApps(parent)
      }
      applications(pth) = app
      app
    }
  }

  def mount(path: String, app: => Mountable): AppMounter = {
    val (longest, name) = splitPaths(path)
    val parent: AppMounter = ensureApps(longest)
    var curr = applications.get(parent.appPath / name)
    if (curr forall (_.isEmpty)) {
      curr = Some(new AppMounter(parent.appPath, name, app))
      logger info ("mounting app at: %s" format (parent.appPath / name))
      applications(parent.appPath / name) = curr.get
    }
    curr.get
  }
}
object AppMounter {
  type ApplicationRegistry = ConcurrentMap[String, AppMounter]
  def newAppRegistry: ApplicationRegistry = new ConcurrentHashMap[String, AppMounter]().asScala
}
final class AppMounter(val basePath: String, val pathName: String, app: => Mountable)(implicit val appContext: AppContext) extends ScalatraLogging with AppMounterLike {
  lazy val mounted = {
    val a = app
    a.mounter = this
    a.initialize(appContext)
    a
  }

  def mount(path: String): AppMounter = {
    val (longest, name) = splitPaths(path)
    val parent: AppMounter = ensureApps(longest)
    mount(parent.appPath / name, NullMountable)
  }

  override def toString = "MountedApp(%s)" format mounted.getClass.getName
}
