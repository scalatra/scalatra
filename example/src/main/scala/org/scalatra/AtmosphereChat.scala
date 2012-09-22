package org.scalatra

import atmosphere._
import json.{JValueResult, JacksonJsonSupport}
import org.json4s._
import JsonDSL._
import java.util.Date
import java.text.SimpleDateFormat
import xml._

class AtmosphereChat extends ScalatraServlet with JacksonJsonSupport with JValueResult with SessionSupport with AtmosphereSupport {
  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    Template.page(
      title = "Scalatra Atmosphere Chat",
      content = bodyHtml,
      url = url(_),
      head = style,
      scripts = "jquery/jquery.atmosphere.js" :: "jquery/application.js" :: Nil
    )
  }

  atmosphere("/the-chat") {
    new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected =>
          println("Client %s is connected" format uuid)
          broadcast(("author" -> "Someone") ~ ("message" -> "joined the room") ~ ("time" -> (new Date().getTime.toString )), Everyone)

        case Disconnected(ClientDisconnected, _) =>
          broadcast(("author" -> "Someone") ~ ("message" -> "has left the room") ~ ("time" -> (new Date().getTime.toString )), Everyone)

        case Disconnected(ServerDisconnected, _) =>
          println("Server disconnected the client %s" format uuid)
        case _: TextMessage =>
          send(("author" -> "system") ~ ("message" -> "Only json is allowed") ~ ("time" -> (new Date().getTime.toString )))

        case JsonMessage(json) =>
          println("Got message %s from %s".format((json \ "message").extract[String], (json \ "author").extract[String]))
          val msg = json merge (("time" -> (new Date().getTime().toString)): JValue)
          broadcast(msg) // by default a broadcast is to everyone but self
//          send(msg) // also send to the sender
      }
    }
  }

  error {
    case t: Throwable => t.printStackTrace()
  }

  val extraCss =
    """
      |
      |p {
      |  line-height: 18px;
      |}
      |
      |div.atmo {
      |  width: 500px;
      |  margin-left: auto;
      |  margin-right: auto;
      |}
      |
      |#detect {
      |  padding: 5px;
      |  background: #ffc0cb;
      |  border-radius: 5px;
      |  border: 1px solid #CCC;
      |  margin-top: 10px;
      |}
      |
      |#content {
      |  padding: 5px;
      |  background: #ddd;
      |  border-radius: 5px;
      |  border: 1px solid #CCC;
      |  margin-top: 10px;
      |}
      |
      |#header {
      |  padding: 5px;
      |  background: #f5deb3;
      |  border-radius: 5px;
      |  border: 1px solid #CCC;
      |  margin-top: 10px;
      |}
      |
      |#input {
      |  border-radius: 2px;
      |  border: 1px solid #ccc;
      |  margin-top: 10px;
      |  padding: 5px;      |
      |}
      |
      |#status {
      |  display: block;
      |  float: left;
      |  margin-top: 15px;
      |}
      |
    """.stripMargin
  val style = <style>{extraCss}</style>

  val bodyHtml = Seq(
    <div class="atmo" id="header"><h3>Atmosphere Chat. Default transport is WebSocket, fallback is long-polling</h3></div>,
    <div class="atmo" id="detect"><h3>Detecting what the browser and server are supporting</h3></div>,
    <div class="atmo" id="content"></div>,
    <div class="atmo">,
      <span id="status">Connecting...</span>,
      <input type="text" id="input" disabled="disabled"/>,
    </div>
  )
}