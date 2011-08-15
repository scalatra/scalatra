package org.scalatra

import com.glines.socketio.server.SocketIOFrame
import socketio._
import javax.servlet.http.HttpServletRequest

class ChatServlet extends ScalatraServlet with SocketIOSupport {


  def socketio(req: HttpServletRequest) = new SocketIOClient {

    def receive = {
      case Connected => {
        try {
          send(SocketIOFrame.JSON_MESSAGE_TYPE, """{ "welcome": "Welcome to Socket IO chat" }""")
        } catch {
          case _ => disconnect()
        }
        broadcast(SocketIOFrame.JSON_MESSAGE_TYPE,
          """{ "announcement": "New participant [%s]" }""".format(clientId))
      }
      case Message(msgType, message) => {
        println("RECV: [%s]" format message)
        message match {
          case "/rclose" => {
            close()
          }
          case "/rdisconnect" => {
            disconnect()
          }
          case _ => {
            broadcast(SocketIOFrame.JSON_MESSAGE_TYPE,
              """{ "message": ["%s", "%s"] }""".format(clientId, message))
          }
        }
      }
      case Disconnected(reason, message) => {
        println("Disconnecting chat client [%s] (%s)".format(clientId, reason))
        broadcast(SocketIOFrame.JSON_MESSAGE_TYPE,
          """{ "announcement": "Participant [%s] left" }""".format(clientId))
      }
    }
  }
}
