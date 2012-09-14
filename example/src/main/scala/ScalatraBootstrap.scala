import org.atmosphere.cpr.MeteorServlet
import org.scalatra._

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new BasicAuthExample, "/auth")
    context.mount(new CookiesExample, "/cookies-example")
    context.mount(new FileUploadExample, "/upload")
    context.mount(new FilterExample, "/")
    context.mount(new Servlet30ChatExample, "/chat30")
    context.mount(new TemplateExample, "/")

    val reg = context.addServlet("MeteorServlet", classOf[MeteorServlet])
    reg.setAsyncSupported(true)
    reg.setLoadOnStartup(0)
    reg.setInitParameter("org.atmosphere.servlet", "org.scalatra.MeteorChatExample")
    reg.addMapping("/meteor/*")
  }
}
