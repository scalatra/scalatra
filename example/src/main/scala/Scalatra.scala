import org.scalatra._

import javax.servlet.ServletContext

class Scalatra extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(classOf[BasicAuthExample], "/auth")
    context.mount(classOf[CookiesExample], "/cookies-example")
    context.mount(classOf[FileUploadExample], "/upload")
    context.mount(classOf[FilterExample], "/")
    context.mount(classOf[Servlet30ChatExample], "/chat_30")
    context.mount(classOf[TemplateExample], "/")

    /*
    // TODO: make work without web.xml, per servlet init parameters
    val meteor = new ServletHolder(classOf[org.atmosphere.cpr.MeteorServlet])
    meteor.setInitParameter("org.atmosphere.servlet", "org.scalatra.MeteorChatExample")
    meteor.setInitOrder(0)
    context.mount(meteor, "/meteor/*") */
    */
  }
}
