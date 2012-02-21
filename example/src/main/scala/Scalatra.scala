import org.scalatra._
import javax.servlet.ServletContext

class Scalatra extends LifeCycle {
  override def init(servletContext: ServletContext) {
    servletContext.mount(new TemplateExample, "/*")
    servletContext.mount(new BasicAuthExample, "/auth/*")
    servletContext.mount(new DocumentExample, "/docs/*")
    servletContext.mount(new Servlet30ChatExample, "/chat_30/*")
    // TODO servlet init params
    // servletContext.mount(new MeteorServlet, "/meteor/*")
    // org.atmosphere.servlet -> org.scalatra.MeteorChatExample
    servletContext.mount(new CookiesExample, "/cookies-example/*")
    servletContext.mount(new FilterExample, "/*")
  }
}
