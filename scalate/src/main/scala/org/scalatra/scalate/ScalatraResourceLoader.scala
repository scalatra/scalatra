package org.scalatra
package scalate

import org.fusesource.scalate.util._
import org.fusesource.scalate.util.Resource._
import java.net.MalformedURLException
import org.fusesource.scalate.util.FileResourceLoader
import java.io.File

object ScalatraResourceLoader extends Log {
  def apply(context: AppContext) = new ScalatraResourceLoader(context)
}
class ScalatraResourceLoader(context: AppContext, delegate: ResourceLoader = new FileResourceLoader()) extends ResourceLoader {

  import ScalatraResourceLoader._
  def resource(uri: String): Option[Resource] = {
      val file = realFile(uri)
      if (file != null) {
        if( file.isFile )
          Some(fromFile(file))
        else
          None
      }
      else {
        try {
          val url = context.resourceFor(uri)
          if (url!=null) {
            val resource = fromURL(url)
            Some(resource)
          } else {
            delegate.resource(uri)
          }
        } catch {
          case x:MalformedURLException=>
            delegate.resource(uri)
        }
      }
    }


    /**
     * Returns the real path for the given uri.
     */
    def realPath(uri: String): String = {
      // TODO should ideally use the Resource API as then we could try figure out
      // the actual file for URL based resources not using getRealPath
      // (which has issues sometimes with unexpanded WARs and overlays)
      for (r <- resource(uri); f <- r.toFile if f != null) {
        return f.getPath
      }
      val file = realFile(uri)
      if (file != null) file.getPath else null
    }


    override protected def createNotFoundException(uri: String) =  new ResourceNotFoundException(resource = uri, root = context.physicalPath("/"))

    /**
     * Returns the File for the given uri
     */
    protected def realFile(uri: String): File = {
      def findFile(uri: String): File = {
        val path: String = context.physicalPath(uri)
        debug("realPath for: " + uri + " is: " + path)

        var answer: File = null
        if (path != null) {
          val file = new File(path)
          debug("file from realPath for: " + uri + " is: " + file)
          if (file.canRead) { answer = file}
        }
        answer
      }

      findFile(uri) match {
        case file: File => file
        case _ => if (uri.startsWith("/") && !uri.startsWith("/WEB-INF")) {
          findFile("/WEB-INF" + uri)
        }
        else {
          null
        }
      }
    }
}
