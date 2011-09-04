package org.scalatra

import javax.servlet.annotation.WebServlet
import java.util.concurrent._
import collection.JavaConversions._
import java.io.IOException
import javax.servlet.http.HttpServletRequest
import javax.servlet._

object Servlet30ChatExample {


  val BEGIN_SCRIPT = "<script type='text/javascript'>\n"
  val END_SCRIPT = "</script>\n"
  def jsonp(script: => String) = "%s%s%s" format (BEGIN_SCRIPT, script, END_SCRIPT)


  object BroadCaster {
    private var _notifier: Future[_] = null

    private def r(thunk: => Unit) = new Runnable {
      def run() {
        thunk
      }
    }

    private val connections = new ConcurrentLinkedQueue[AsyncContext]
    private val messageQueue = new LinkedBlockingQueue[String]()

    private val notifierExecutor = Executors.newSingleThreadExecutor()


    def add(req: HttpServletRequest) = {
      if (!isStarted) start

      val conn = req.startAsync()
      conn.setTimeout(10 * 60 * 1000)
      connections add conn
      conn.addListener(new AsyncListener {
        def onComplete(event: AsyncEvent) {
          connections remove conn
        }

        def onTimeout(event: AsyncEvent) { connections remove conn }

        def onError(event: AsyncEvent) {}

        def onStartAsync(event: AsyncEvent) {}
      })
    }

    def broadcast(message: String) = {
      messageQueue put message
    }

    def isStarted = _notifier != null && !_notifier.isDone
    def stop {
      connections foreach { _.complete() }
      connections.clear
      _notifier.cancel(true)
      notifierExecutor.shutdown()
    }
    def start {
      if (!isStarted) {
        _notifier = notifierExecutor.submit(r {
          var done = false
          while(!done) {
            try {
              val msg = messageQueue.take()
              if(msg != null && msg.trim.nonEmpty) {
                println("Got a message from the queue: " + msg)
                connections foreach { conn =>
                  try {
                    val w = conn.getResponse.getWriter
                    w.println(msg)
                    w.flush()
                  } catch {
                    case e: IOException => {
                      e.printStackTrace()
                      connections remove conn
                    }
                  }
                }
              }
            } catch {
              case e: InterruptedException => {
                done = true
              }
            }
          }
        })
        this
      }
    }
  }

  def xssFilter(orig: String) = {
    val sb = new StringBuilder
    orig foreach {
      case '\b' => sb ++= "\\b"
      case '\f' => sb ++= "\\f"
      case '\n' => sb ++= "\\n"
      case '\r' => // ignore
      case '\t' => sb ++= "\\t"
      case '\'' => sb ++= "\\'"
      case '\"' => sb ++= "\\\""
      case '\\' => sb ++= "\\\\"
      case '<' => sb ++= "&lt;"
      case '>' => sb ++= "&gt;"
      case '&' => sb ++= "&amp;"
      case c => sb += c
    }
    sb.toString()
  }
}

class Servlet30ChatExample extends ScalatraServlet {

  import Servlet30ChatExample._

  override def initialize(config: ServletConfig) {
    super.initialize(config)
    BroadCaster.start
  }

  get("/?*") {
    contentType = "text/html"
    response.setHeader("Cache-Control", "private")
    response.setHeader("Pragma", "no-cache")

    val w = response.getWriter
    w.println("<!-- Comet is a programming technique that enables web servers to send data to the client without having any need for the client to request it. -->")
    w.flush()

    BroadCaster add request
  }

  get("/test") {
    println("tesing")
    "it works!"
  }

  post("/*") {
    contentType = "text/javascript"
    response.setHeader("Cache-Control", "private")
    response.setHeader("Pragma", "no-cache")
    request.setCharacterEncoding("UTF-8")

    params('action) match {
      case "login" => {
        BroadCaster broadcast msg("System Message", "%s has joined." format params('name))
        "success"
      }
      case "post" => {
        BroadCaster broadcast msg(params('name), params('message))
        "success"
      }
      case _ => {
        status = 422
        "Unprocessable entity"
      }
    }
  }

  protected def msg(name: String, message: String) = jsonp {
    "window.parent.app.update({ name: \"%s\", message: \"%s\" });\n" format (xssFilter(name), xssFilter(message))
  }

  override def destroy() {
    BroadCaster.stop
  }
}