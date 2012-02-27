import org.scalatra._

class Scalatra extends LifeCycle {
  override def init(context: ApplicationContext) {
    context.mount(new TemplateExample, "/*")
    context.mount(new BasicAuthExample, "/auth/*")
    context.mount(new DocumentExample, "/docs/*")
    context.mount(new Servlet30ChatExample, "/chat_30/*")
    // TODO servlet init params
    // servletContext.mount(new MeteorServlet, "/meteor/*")
    // org.atmosphere.servlet -> org.scalatra.MeteorChatExample
    context.mount(new CookiesExample, "/cookies-example/*")
    context.mount(new FilterExample, "/*")
  }
}
