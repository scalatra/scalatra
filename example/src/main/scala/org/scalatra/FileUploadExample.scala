package org.scalatra

import javax.servlet.annotation.MultipartConfig
import servlet.{SizeConstraintExceededException, FileUploadSupport}
import xml.Node

@MultipartConfig(maxFileSize = 3*1024*1024)
class FileUploadExample extends ScalatraServlet with FileUploadSupport with FlashMapSupport {
  object Template {
    def page(content: Seq[Node]) = {
      <html>
        <head>
          <title>File Upload</title>
        </head>
        <body>
          <form action={url("/upload")} method="post" enctype="multipart/form-data">
           <p>File to upload: <input type="file" name="file" /></p>
           <p><input type="submit" value="Upload" /></p>
         </form>

          {content}
        </body>
      </html>
    }
  }

  error {
    case e: SizeConstraintExceededException =>
      RequestEntityTooLarge(Template.page(
        <p>The file you uploaded exceeded the 3 MB limit.</p>))
  }

  get("/") {
    Template.page(
      <p>
        Upload a file using the above form. After you hit "Upload"
        the file will be uploaded and your browser will start
        downloading it.
      </p>

      <p>
        The maximum file size accepted is 3 MB.
      </p>)
  }

  post("/") {
    fileParams.get("file") match {
      case Some(file) =>
        Ok(file.get(), Map(
          "Content-Type"        -> (file.contentType.getOrElse("application/octet-stream")),
          "Content-Disposition" -> ("attachment; filename=\"" + file.name + "\"")
        ))

      case None =>
        BadRequest(Template.page(
          <p>
            Hey! You forgot to select a file.
          </p>))
    }
  }
}
