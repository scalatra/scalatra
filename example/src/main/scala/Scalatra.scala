import org.scalatra._

import javax.servlet.ServletContext

class Scalatra extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new BasicAuthExample, "/auth")
    context.mount(new CookiesExample, "/cookies-example")
    context.mount(new FileUploadExample, "/upload")
    context.mount(new FilterExample, "/")
    context.mount(new Servlet30ChatExample, "/chat_30")
    context.mount(new TemplateExample, "/")
  }
}
