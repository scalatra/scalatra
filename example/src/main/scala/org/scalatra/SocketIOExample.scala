package org.scalatra

import socketio.SocketIOSupport


class SocketIOExample extends ScalatraServlet with SocketIOSupport {

  socketio { builder =>
    builder.onConnect { client =>
      println("connecting a client")
    }

    builder.onMessage { (client, messageType, message) =>
      println("RECV [%s]: %s".format(messageType, message))
      client.send("ECHO: " + message)
    }

    builder.onDisconnect { (client, reason, message) =>
      println("Disconnect [%s]: %s".format(reason, message))
    }
  }
}