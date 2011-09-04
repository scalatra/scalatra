package org.scalatra

import collection.mutable.LinkedList
import org.atmosphere.util.XSSHtmlFilter
import org.atmosphere.cpr.BroadcastFilter.BroadcastAction
import org.atmosphere.cpr.{AtmosphereResourceEvent, AtmosphereResourceEventListener, Meteor, BroadcastFilter}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import collection.JavaConversions._
import javax.servlet.annotation.WebServlet

class EventsLogger extends AtmosphereResourceEventListener {
  def onThrowable(event: AtmosphereResourceEvent[HttpServletRequest, HttpServletResponse]) {
    println ("onThrowable(): %s" format event)
  }

  def onBroadcast(event: AtmosphereResourceEvent[HttpServletRequest, HttpServletResponse]) {
    println ("onBroadcast(): %s" format event)
  }

  def onDisconnect(event: AtmosphereResourceEvent[HttpServletRequest, HttpServletResponse]) {
    println ("onDisconnect(): %s" format event)
  }

  def onResume(event: AtmosphereResourceEvent[HttpServletRequest, HttpServletResponse]) {
    println ("onResume(): %s" format event)
  }

  def onSuspend(event: AtmosphereResourceEvent[HttpServletRequest, HttpServletResponse]) {
    println ("onSuspend(): %s" format event)
  }
}

class JsonpFilter extends BroadcastFilter {

  import Servlet30ChatExample.jsonp

  def filter(originalMessage: AnyRef, data: AnyRef) = data match {
    case d: String => {
      val (name, message) = if (d.indexOf("__") > 0) (d.substring(0, d.indexOf("__")), d.substring(d.indexOf("__") + 2)) else (d, "")
      new BroadcastAction(jsonp("window.parent.app.update({ name: \"%s\", message: \"%s\"});\n".format(name, message)))
    }
    case _ => new BroadcastAction(data)
  }
}

class MeteorChatExample extends ScalatraServlet {

  val filters = LinkedList(new XSSHtmlFilter, new JsonpFilter)

  get("/?*") {
    val m: Meteor = Meteor.build(request, filters, null)
    m.addListener(new EventsLogger())
    session += "meteor" -> m
    contentType = "text/html;charset=ISO-8859-1"
    m suspend  -1
    m broadcast (request.getServerName + "__has suspended a connection from " + request.getRemoteAddr)
  }

  post("/?*") {
    val m = session("meteor").asInstanceOf[Meteor]
    response.setCharacterEncoding("UTF-8")
    val action = params('action)
    val name = params('name)
    action match {
      case "login" => {
        session += "name" -> name
        m broadcast ("System message from " + request.getServerName + "__" + name + " has joined.")
      }
      case "post" => {
        val msg = params('message)
        m.broadcast(name + "__" + msg)
      }
      case _ => {
        status = 422
      }
    }
    "success"
  }
}