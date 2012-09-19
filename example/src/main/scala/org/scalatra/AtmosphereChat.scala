package org.scalatra

import atmosphere._
import json.{JValueResult, JacksonJsonSupport}
import org.json4s._
import JsonDSL._

class AtmosphereChat extends ScalatraServlet with JacksonJsonSupport with JValueResult with SessionSupport with AtmosphereSupport {
  implicit protected val jsonFormats: Formats = DefaultFormats

  get("/") {
    "atmosphere chat"
  }

  atmosphere("/the-chat") {
    new AtmosphereClient {
      def receive: AtmoReceive = {
        case Connected => broadcast(("author" -> "Someone") ~ ("body" -> "joined the room"), Everyone)
        case Disconnected(_) => broadcast(("author" -> "Someone") ~ ("body" -> "has left the room"), Everyone)
        case _: TextMessage => send(("author" -> "system") ~ ("body" -> "Only json is allowed"))
        case JsonMessage(json) => broadcast(json) // by default a broadcast is to everyone but self
      }
    }
  }

  error {
    case t: Throwable => t.printStackTrace()
  }
}