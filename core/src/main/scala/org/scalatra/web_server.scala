package org.scalatra

import _root_.akka.util.Switch
import scalax.file._
import collection.mutable.ListBuffer
import java.security.KeyStore
import java.io.FileInputStream
import javax.net.ssl.{KeyManagerFactory, SSLContext}
import scalax.file.ImplicitConversions._

object ServerCapability {
  val DefaultPublicDirectory = PublicDirectory("public")
  val DefaultDataDirectory = DataDirectory(DefaultPublicDirectory.path / "uploads")
}

trait ServerCapability
case class SslSupport(
             keystorePath: Path = sys.props("keystore.file.path"),
             keystorePassword: String = sys.props("keystore.file.password"),
             algorithm: String = sys.props.get("ssl.KeyManagerFactory.algorithm").flatMap(_.blankOption) getOrElse "SunX509") extends ServerCapability
case class ContentCompression(level: Int = 6) extends ServerCapability
case class PublicDirectory(path: Path, cacheFiles: Boolean = true) extends ServerCapability
case class TempDirectory(path: Path) extends ServerCapability
case class DataDirectory(path: Path) extends ServerCapability

case class ServerInfo(
              name: String,
              version: String = BuildInfo.version,
              port: Int = 8765,
              base: String = "/",
              capabilities: Seq[ServerCapability] = Seq.empty) {

  val publicDirectory = (capabilities find {
    case _: PublicDirectory => true
    case _ => false
  }) map (_.asInstanceOf[PublicDirectory]) getOrElse ServerCapability.DefaultPublicDirectory

  val dataDirectory = (capabilities find {
    case _: DataDirectory => true
    case _ => false
  }) map (_.asInstanceOf[DataDirectory]) getOrElse ServerCapability.DefaultDataDirectory

  val tempDirectory = (capabilities find {
    case _: TempDirectory => true
    case _ => false
  }) map (_.asInstanceOf[TempDirectory]) getOrElse TempDirectory(Path.createTempDirectory(prefix = "scalatra-tmp"))

  val sslContext = (capabilities find {
    case _: SslSupport => true
    case _ => false
  }) map {
    case cfg: SslSupport => {
      val ks = KeyStore.getInstance("JKS")
      val fin = new FileInputStream(cfg.keystorePath.toAbsolute.path)
      ks.load(fin, cfg.keystorePassword.toCharArray)
      val kmf = KeyManagerFactory.getInstance(cfg.algorithm)
      kmf.init(ks, cfg.keystorePassword.toCharArray)
      val context = SSLContext.getInstance("TLS")
      context.init(kmf.getKeyManagers, null, null)
      context
    }
  }

  val contentCompression = (capabilities find {
    case _: ContentCompression => true
    case _ => false
  }) map (_.asInstanceOf[ContentCompression])
}


trait WebServerFactory {

  def DefaultServerName: String

  def apply(capabilities: ServerCapability*): WebServer

  def apply(port: Int,  capabilities: ServerCapability*): WebServer

  def apply(base: String, capabilities: ServerCapability*): WebServer

  def apply(port: Int, base: String, capabilities: ServerCapability*): WebServer

}

object WebServer {
  val DefaultPath = "/"
  val DefaultPathName = ""
}
trait WebServer extends ScalatraLogging with AppMounterLike {

  def info: ServerInfo
  def capabilities = info.capabilities
  def name = info.name
  def version = info.version
  def port = info.port

  def basePath = info.base
  final val pathName = WebServer.DefaultPathName

  implicit lazy val appContext =
    DefaultAppContext(info, AppMounter.newAppRegistry)

  def mount[TheApp <: Mountable](app: => TheApp): AppMounter = mount("/", app)

  protected lazy val started = new Switch
  final def start() {
    started switchOn {
      initializeApps() // If we don't initialize the apps here there are race conditions
      startCallbacks foreach (_.apply())
      sys.addShutdownHook(stop())
    }
  }

  def initializeApps() {
    applications.values foreach (_.mounted)
  }

  private val startCallbacks = ListBuffer[() => Any]()
  private val stopCallbacks = ListBuffer[() => Any]()

  def onStart(thunk: => Any) = startCallbacks += { () => thunk }
  def onStop(thunk: => Any) = stopCallbacks += { () => thunk }

  final def stop() {
    started switchOff {
      stopCallbacks foreach (_.apply())
      appContext.actorSystem.shutdown()
    }
  }

  def apply[TheApp <: Mountable](app: => TheApp) = {
    mount("/", app)
    this
  }
  def apply[TheApp <: Mountable](path: String, app: => TheApp) = {
    mount(path, app)
    this
  }


}