package org.scalatra

import java.util.Date

import org.json4s.JsonDSL._
import org.json4s._
import org.scalatra.atmosphere._
import org.scalatra.json.JacksonJsonSupport

import scala.concurrent.ExecutionContext

class AtmosphereChat extends ScalatraServlet with JacksonJsonSupport with AtmosphereSupport {
  implicit protected val jsonFormats: Formats = DefaultFormats

  import scala.concurrent.ExecutionContext.Implicits.global

  get("/") {
    Template.page(
      title = "Scalatra Atmosphere Chat",
      content = bodyHtml,
      url = url(_, includeServletPath = false),
      scripts = "/jquery/jquery.atmosphere.js" :: "/jquery/application.js" :: Nil,
      defaultScripts = "/assets/js/jquery.min.js" :: "/assets/js/bootstrap.min.js" :: Nil
    )
  }

  get("/print-broadcasters") {
    val bcs = AtmosphereClient.lookupAll()
    bcs foreach println
    bcs.mkString("[", ", ", "]")
  }

  get("/broadcast") {
    val jv = ("author" -> "System") ~ ("message" -> "big brother speaking") ~ ("time" -> (new Date().getTime.toString))
    AtmosphereClient.broadcast(routeBasePath + "/the-chat", jv)

  }

  get("/broadcast-all") {
    val jv = ("author" -> "System") ~ ("message" -> "big brother speaking") ~ ("time" -> (new Date().getTime.toString))
    AtmosphereClient.broadcastAll(jv)
  }

  atmosphere("/the-chat") {
    new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          println("Client %s is connected" format uuid)
          broadcast(("author" -> "Someone") ~ ("message" -> "joined the room") ~ ("time" -> (new Date().getTime.toString)), Everyone)

        case Disconnected(ClientDisconnected, _) =>
          broadcast(("author" -> "Someone") ~ ("message" -> "has left the room") ~ ("time" -> (new Date().getTime.toString)), Everyone)

        case Disconnected(ServerDisconnected, _) =>
          println("Server disconnected the client %s" format uuid)
        case _: TextMessage =>
          send(("author" -> "system") ~ ("message" -> "Only json is allowed") ~ ("time" -> (new Date().getTime.toString)))

        case JsonMessage(json) =>
          println("Got message %s from %s".format((json \ "message").extract[String], (json \ "author").extract[String]))
          val msg = json merge (("time" -> (new Date().getTime.toString)): JValue)
          broadcast(msg) // by default a broadcast is to everyone but self
        //          send(msg) // also send to the sender
      }
    }
  }

  atmosphere("/multiroom/:id") {
    println("id: " + params("id"))
    val room = params("id")
    new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          println("Client %s is connected" format uuid)
          broadcast(("author" -> "Someone") ~ ("message" -> ("joined the room: " + room)) ~ ("time" -> (new Date().getTime.toString)), Everyone)

        case Disconnected(ClientDisconnected, _) =>
          broadcast(("author" -> "Someone") ~ ("message" -> ("left the room: " + room)) ~ ("time" -> (new Date().getTime.toString)), Everyone)

        case Disconnected(ServerDisconnected, _) =>
          println("Server disconnected the client %s" format uuid)
        case _: TextMessage =>
          send(("author" -> "system") ~ ("message" -> "Only json is allowed") ~ ("time" -> (new Date().getTime.toString)))

        case JsonMessage(json) =>
          println("Got message %s from %s in room: %s".format((json \ "message").extract[String], (json \ "author").extract[String], room))
          val msg = json merge (("time" -> (new Date().getTime.toString)): JValue)
          broadcast(msg) // by default a broadcast is to everyone but self
        //          send(msg) // also send to the sender
      }
    }
  }
  error {
    case t: Throwable => t.printStackTrace()
  }

  val bodyHtml =
    <div class="row">
      <div id="header" class="span6 offset3"><h5>Atmosphere Chat. Default transport is WebSocket, fallback is long-polling</h5></div>
      <div id="detect" class="span6 offset3"><h5>Detecting what the browser and server are supporting</h5></div>
      <div id="content" class="span6 offset3"></div>
      <div class="span6 offset3">
        <span id="status">Connecting...</span>
        <input type="text" id="input" disabled="disabled"/>
      </div>
    </div>
}
