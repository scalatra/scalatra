package org.scalatra

import org.specs2.Specification
import java.util.concurrent.atomic.AtomicInteger
import java.net.{URL, URI}
import collection.JavaConversions._
import com.google.common.collect.MapMaker


class AppMounterSpec extends Specification { def is =

  "AppMounting should" ^
    "allow mounting an app" ^
      "with a basepath" ^
        "starting with a '/'" ! specify.mountsWithBasePathWithSlash ^
        "not starting with a '/'" ! specify.mountsWithBasePathWithoutSlash ^ bt(2) ^
    "when finding applications" ^ t ^
      "throw an error when the application can't be found" ! specify.throwForNonExisting ^ bt ^
      "for an existing application" ^
        "find with absolute path from root mounter" ! specify.findsAbsolutePathFromRoot ^
        "find with absolute path from sub mounter" ! specify.findsAbsolutePathFromSub ^
        "find with a relative path" ! specify.findsRelativePath ^
        "find for an absolute URI" ! specify.findsForAbsoluteUri ^
        "find for a relative URI" ! specify.findsForRelativeUri ^
  end

  def specify = new AppMountingSpecContext
  val counter = new AtomicInteger()

  class AppMountingSpecContext {

    implicit val context = new AppContext {
      def server = ServerInfo("testserver", "0.0.1", 0, "")

      implicit def appContext = this

      implicit val applications: AppMounter.ApplicationRegistry = new MapMaker().makeMap[String, AppMounter]

      def sessions: SessionStore[_ <: HttpSession] = new NoopSessionStore

      def resourceFor(path: String): URL = null

      def physicalPath(uri: String): String = null
    }
    val root = new AppMounter("/", "", NullMountable)

    def mountsWithBasePathWithSlash = testMount("/somepath")

    def mountsWithBasePathWithoutSlash = testMount("apath")

    def throwForNonExisting = {
      root.mount("thepath", NullMountable)
      root.apply("i-don-t-exist") must throwA[NoSuchElementException]
    }

    def findsAbsolutePathFromRoot = {
      val posts = root.mount("posts")
      val comments = posts.mount("comments")
      root("/posts/comments") must_== comments
    }

    def findsAbsolutePathFromSub = {
      val posts = root.mount("posts")
      val comments = posts.mount("comments")
      comments("/posts/comments") must_== comments
    }

    def findsRelativePath = {
      val posts = root.mount("posts")
      val comments = posts.mount("comments")
      posts("comments") must_== comments
    }

    def findsForAbsoluteUri = {
      val posts = root.mount("posts")
      val comments = posts.mount("comments")
      posts(URI.create("/posts/comments")) must_== comments
    }

    def findsForRelativeUri = {
      val posts = root.mount("posts")
      val comments = posts.mount("comments")
      posts(URI.create("comments")) must_== comments
    }

    private def testMount[TA <: Mountable](path: String) = {
      val pth = if (!path.startsWith("/")) "/" + path else path
      val mm = root.mount(path, NullMountable)
      root(pth) must_== mm
    }
  }
}