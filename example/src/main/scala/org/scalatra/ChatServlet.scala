package org.scalatra

import socketio.SocketIOSupport
import com.glines.socketio.server.SocketIOFrame

class ChatServlet extends ScalatraServlet with SocketIOSupport {


  socketio { socket =>
    socket.onConnect { connection =>
      println("Connecting chat client [%s]" format connection.clientId)
      try {
        connection.send(SocketIOFrame.JSON_MESSAGE_TYPE, """{ "welcome": "Welcome to Socket IO chat" }""")
      } catch {
        case _ => connection.disconnect
      }
      connection.broadcast(SocketIOFrame.JSON_MESSAGE_TYPE,
        """{ "announcement": "New participant [%s]" }""".format(connection.clientId))
    }

    socket.onDisconnect { (connection, reason, _) =>
      println("Disconnecting chat client [%s] (%s)".format(connection.clientId, reason))
      connection.broadcast(SocketIOFrame.JSON_MESSAGE_TYPE,
        """{ "announcement": "Participant [%s] left" }""".format(connection.clientId))
    }

    socket.onMessage { (connection, _, message) =>
      println("RECV: [%s]" format message)
      message match {
        case "/rclose" => {
          connection.close
        }
        case "/rdisconnect" => {
          connection.disconnect
        }
        case _ => {
          connection.broadcast(SocketIOFrame.JSON_MESSAGE_TYPE,
            """{ "message": ["%s", "%s"] }""".format(connection.clientId, message))
        }
      }
    }
  }
}