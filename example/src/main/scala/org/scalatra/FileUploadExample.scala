import org.scalatra._

import org.scalatra.fileupload.FileUploadSupport
import java.io.{File,FileOutputStream}
import org.apache.commons.fileupload.FileItem

class FileUploadExample extends ScalatraServlet with FileUploadSupport  {
  
  get("/new") {
    <form method="post" enctype="multipart/form-data" action="create">
      <input type="file" name="foo" />
      <input type="submit" />
    </form>
  }

  post("/create") {
    val message = processFile(fileParams("foo"))
    message
  }

  def processFile(upload:FileItem):String = {
    val filePath:String = "/tmp/"
    val file:File = new File(filePath + upload.getName)
    upload.write(file);
    "Your file was: " + file.length.toString + " bytes"
  }

}