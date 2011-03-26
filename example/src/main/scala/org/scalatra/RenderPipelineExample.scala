package org.scalatra

import java.io.File


class RenderPipelineExample extends ScalatraPipelinedServlet {

  render[String] {
    case s => "rendering: " + s
  }

  render[List[String]] {
    case l => "Rendering list:" + l.mkString(" <br />\n", " <br />\n", " <br />\n")
  }

  get("/?") {
    "hello I'm rendering"
  }

  get("/list") {
    "first" :: "second" :: "third" :: "fourth" :: Nil
  }

  get("/image") {
    contentType = "image/jpeg"
    new File(getClass.getClassLoader.getResource("image.jpg").toURI)
  }
}