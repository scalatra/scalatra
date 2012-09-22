package org.scalatra

import servlet.{MultipartConfig, SizeConstraintExceededException, FileUploadSupport}
import xml.Node

class FileUploadExample extends ScalatraServlet with FileUploadSupport with FlashMapSupport {
  configureMultipartHandling(MultipartConfig(maxFileSize = Some(3*1024*1024)))

//  object Template {
//    def page(content: Seq[Node]) = {
//      <html>
//        <head>
//          <title>File Upload</title>
//        </head>
//        <body>
//          <form action={url("/upload")} method="post" enctype="multipart/form-data">
//           <p>File to upload: <input type="file" name="file" /></p>
//           <p><input type="submit" value="Upload" /></p>
//          </form>

//          {content}
//          <hr/>
//          <a href={url("/date/2009/12/26")}>date example</a><br/>
//          <a href={url("/form")}>form example</a><br/>
//          <a href={url("/upload")}>upload</a><br/>
//          <a href={url("/")}>hello world</a><br/>
//          <a href={url("/flash-map/form")}>flash scope</a><br/>
//          <a href={url("/login")}>login</a><br/>
//          <a href={url("/logout")}>logout</a><br/>
//          <a href={url("/filter-example")}>filter example</a><br/>
//          <a href={url("/cookies-example")}>cookies example</a><br/>
//          <a href={url("/chat.html")}>chat demo</a><br/>
//        </body>
//      </html>
//    }
//  }
  
  def displayPage(content: Seq[Node]) = Template.page("File upload example", content, url(_))

  error {
    case e: SizeConstraintExceededException =>
      RequestEntityTooLarge(displayPage(
        <p>The file you uploaded exceeded the 3 MB limit.</p>))
  }

  get("/") {
    displayPage(
      <form action={url("/upload")} method="post" enctype="multipart/form-data">
       <p>File to upload: <input type="file" name="file" /></p>
       <p><input type="submit" value="Upload" /></p>
      </form>
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
        BadRequest(displayPage(
          <p>
            Hey! You forgot to select a file.
          </p>))
    }
  }
}
