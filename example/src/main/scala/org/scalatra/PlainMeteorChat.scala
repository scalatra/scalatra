package org.scalatra

import javax.servlet.http.{HttpServletResponse, HttpServletRequest, HttpServlet}
import org.atmosphere.cpr._
import java.util.Date

object PlainMeteorChat {
//  val bcFactory = new DefaultBroadcasterFactory()
}

class PlainMeteorChat extends HttpServlet {
  override def doGet(req: HttpServletRequest, resp: HttpServletResponse) {
    Meteor.build(req).addListener(new AtmosphereResourceEventListenerAdapter).suspend(-1)
  }

  override def doPost(req: HttpServletRequest, resp: HttpServletResponse) {
    val body = req.getReader.readLine.trim
    // Simple JSON -- Use Jackson for more complex structure
    // Message looks like { "author" : "foo", "message" : "bar" }
    val author = body.substring(body.indexOf(":") + 2, body.indexOf(",") - 1)
    val message = body.substring(body.lastIndexOf(":") + 2, body.length() - 2)
    val msg = """{"text":"%s","author":"%s","time":%s}""".format(message, author, new Date().getTime)
    BroadcasterFactory.getDefault.lookup("/*").broadcast(msg)
  }
}