package org.scalatra

import javax.servlet.http.HttpServletRequest
import socketio._


class SocketIOExample extends ScalatraServlet with SocketIOSupport {

  def socketio(req: HttpServletRequest) = new SocketIOClient {

    // during the construction of the object is the last time the request is guaranteed to be there.
    // if you want to access session variables etc then this is the place to copy them to your client connection.
    val userId = req.getSession().get("userId")


    def receive = {
      case Connected =>  {
        println("connecting a client: " + clientId)
      }
      case Message(messageType, message) => {
        println("RECV [%s]: %s".format(messageType, message))
        send("ECHO: " + message)
      }
      case Disconnected(reason, message) => {
        println("Disconnect [%s]: %s".format(reason, message))
      }
    }
  }
}
