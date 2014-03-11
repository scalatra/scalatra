import org.scalatra._

import javax.servlet.ServletContext

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {

    //context.setInitParameter(org.scalatra.atmosphere.TrackMessageSize, "true")
    //context.mount(new BasicAuthExample, "/auth")
    context.mount(new CookiesExample, "/cookies-example")
    //context.mount(new BasicAuthExample, "/basic-auth")
    context.mount(new FileUploadExample, "/upload")
    context.mount(new FilterExample, "/")
    //context.mount(new AtmosphereChat, "/atmosphere")
    context.mount(new TemplateExample, "/")

  }
}
