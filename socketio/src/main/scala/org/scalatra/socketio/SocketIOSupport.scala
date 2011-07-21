package org.scalatra
package socketio

import com.glines.socketio.server.transport._
import collection.JavaConversions._
import scala.io.Source
import java.lang.String
import com.glines.socketio.common.DisconnectReason
import com.glines.socketio.server.{SocketIOInbound, SocketIOOutbound, Transport, SocketIOSessionManager}
import com.glines.socketio.server.SocketIOFrame.FrameType
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import util.RicherString._
import java.util.concurrent.{ConcurrentSkipListSet, CopyOnWriteArrayList, ConcurrentHashMap}

trait SocketIOClient extends SocketIOSupport.ScalatraSocketIOClient
sealed trait SocketIOMessage
case object Connected extends SocketIOMessage
case class Message(messageType: Int, message: String) extends SocketIOMessage
case class Disconnected(reason: DisconnectReason, message: String) extends SocketIOMessage

object SocketIOSupport {
  val BUFFER_SIZE_INIT_PARAM = "bufferSize"
  val MAX_IDLE_TIME_INIT_PARAM: String = "maxIdleTime"
  val BUFFER_SIZE_DEFAULT: Int = 8192
  val MAX_IDLE_TIME_DEFAULT: Int = 300 * 1000


  type SocketIOReceive = PartialFunction[SocketIOMessage, Unit]

  trait ScalatraSocketIOClient extends SocketIOInbound {

    val clientId = GenerateId()

    final def onConnect(outbound: SocketIOOutbound) {
      _out = Some(outbound)
      clients += this
      receive(Connected)
    }

    final def onDisconnect(reason: DisconnectReason, errorMessage: String) {
      receive(Disconnected(reason, errorMessage))
      clients -= this
    }

    final def onMessage(messageType: Int, message: String) {
      receive(Message(messageType, message))
    }

    def receive: SocketIOReceive

    protected var _out: Option[SocketIOOutbound] = None

    def getProtocol = null

    final def send(messageType: Int, message: String) {
      _out foreach { _.sendMessage(messageType, message) }
    }

    final def send(message: String) {
      _out foreach { _.sendMessage(message) }
    }

    final def broadcast(messageType: Int, message: String) {
      clients.filterNot(_.clientId == clientId).foreach { _.send(messageType, message) }
    }

    final def close() {
      _out foreach { _.close() }
    }

    final def disconnect() {
      _out foreach { _.disconnect() }
    }

  }

  val clients: collection.mutable.Set[ScalatraSocketIOClient] = new ConcurrentSkipListSet[ScalatraSocketIOClient]

}

/**
 * This interface is likely to change before 2.0.0.  Please come to the
 * mailing list or IRC before betting your project on this.
 */
trait SocketIOSupport extends Handler with Initializable {
  self: ScalatraServlet =>

  import SocketIOSupport._

  private val sessionManager: SocketIOSessionManager = new SocketIOSessionManager
  private val transports: collection.mutable.ConcurrentMap[String, Transport] = new ConcurrentHashMap[String, Transport]
//  private var _builder: SocketIOClientBuilder = null
  private val _connections = new CopyOnWriteArrayList[SocketIOClient]

  override def initialize(config: Config) {
    val bufferSize = (Option(getServletConfig.getInitParameter(BUFFER_SIZE_INIT_PARAM)) getOrElse BUFFER_SIZE_DEFAULT.toString).toInt
    val maxIdleTime = (Option(getServletConfig.getInitParameter(MAX_IDLE_TIME_INIT_PARAM)) getOrElse MAX_IDLE_TIME_DEFAULT.toString).toInt

    val websocketTransport = new WebSocketTransport(bufferSize, maxIdleTime)
    val flashsocketTransport = new FlashSocketTransport(bufferSize, maxIdleTime)
    val htmlFileTransport = new HTMLFileTransport(bufferSize, maxIdleTime)
    val xhrMultipartTransport = new XHRMultipartTransport(bufferSize, maxIdleTime)
    val xhrPollingTransport = new XHRPollingTransport(bufferSize, maxIdleTime)
    val jsonpPollingTransport = new JSONPPollingTransport(bufferSize, maxIdleTime)
    transports ++= Seq(websocketTransport.getName -> websocketTransport,
      flashsocketTransport.getName -> flashsocketTransport,
      htmlFileTransport.getName -> htmlFileTransport,
      xhrMultipartTransport.getName -> xhrMultipartTransport,
      xhrPollingTransport.getName -> xhrPollingTransport,
      jsonpPollingTransport.getName -> jsonpPollingTransport)

    transports.values foreach { _.init(getServletConfig) }
  }

  abstract override def handle(req: HttpServletRequest, res: HttpServletResponse) {
    val path = req.getPathInfo
    if(path.isBlank || path == "/") super.handle(req, res)
    val parts = (if (path.startsWith("/")) path.substring(1) else path).split("/")
    val transport = transports.get(parts(0))
    if(transport.isEmpty) {
      super.handle(req, res)
    } else {
      transport.get.handle(req, res, new Transport.InboundFactory {
        def getInbound(p1: HttpServletRequest) = socketio(p1)
      }, sessionManager)
    }
  }

  get("/socket.io.js") {
    contentType = "text/javascript"
    val is = getClass.getClassLoader.getResourceAsStream("com/glines/socketio/socket.io.js")
    val p = request.getServletPath.substring(1)
    Source.fromInputStream(is).getLines foreach {
      line =>
        response.getWriter.println(
          line.replace("'socket.io'", "'%s'" format p).replace("socket.io/WebSocketMain", "%s/WebSocketMain" format p))
    }
  }


  def socketio(req: HttpServletRequest): SocketIOClient

}
