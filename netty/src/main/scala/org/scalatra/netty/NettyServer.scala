package org.scalatra
package netty

import java.util.concurrent.Executors
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory
import org.jboss.netty.bootstrap.ServerBootstrap
import org.jboss.netty.channel.group.DefaultChannelGroup
import java.net.{URL, InetSocketAddress}
import org.jboss.netty.handler.codec.http2.{DiskFileUpload, DiskAttribute}

object NettyServer extends WebServerFactory {
  val DefaultServerName = "ScalatraNettyHttpServer"
  
  def apply(capabilities: ServerCapability*): WebServer = {
    NettyServer(ServerInfo(DefaultServerName, capabilities = capabilities))  
  }
  
  def apply(port: Int,  capabilities: ServerCapability*): WebServer = {
    NettyServer(ServerInfo(DefaultServerName, port = port, capabilities = capabilities))
  }
  
  def apply(base: String, capabilities: ServerCapability*): WebServer = {
    NettyServer(ServerInfo(DefaultServerName, base = base, capabilities = capabilities))
  }
  
  def apply(port: Int, base: String, capabilities: ServerCapability*): WebServer = {
    NettyServer(ServerInfo(DefaultServerName, port = port, base = base, capabilities = capabilities))
  }
  
  private[netty] val allChannels = new DefaultChannelGroup()
  
  
}
case class NettyServer(info: ServerInfo) extends WebServer {


  implicit lazy val appContext: AppContext = NettyAppContext(info, AppMounter.newAppRegistry)

  private val bossThreadPool = Executors.newCachedThreadPool()
  private val workerThreadPool = Executors.newCachedThreadPool()

//  protected def keepAlive = true

  private val bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(bossThreadPool, workerThreadPool))
  bootstrap.setOption("soLinger", 0)
  bootstrap.setOption("reuseAddress", true)
  bootstrap.setOption("child.tcpNoDelay", true)
//  bootstrap.setOption("child.keepAlive", keepAlive)

  val channelFactory = new ScalatraPipelineFactory()

  
  onStart {
    DiskFileUpload.baseDirectory = info.tempDirectory.path.toAbsolute.path
    DiskAttribute.baseDirectory = DiskFileUpload.baseDirectory
    logger info ("Starting Netty HTTP server on %d" format port)
    bootstrap setPipelineFactory channelFactory
    bootstrap.bind(new InetSocketAddress(port))
  }

  onStop {
    appContext.applications.valuesIterator foreach (_.mounted.destroy())
    NettyServer.allChannels.close().awaitUninterruptibly()
    bootstrap.releaseExternalResources()
    workerThreadPool.shutdown()
    bossThreadPool.shutdown()
    logger info ("Netty HTTP server on %d stopped." format port)
  }

}