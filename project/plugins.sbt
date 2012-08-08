scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com"
)

libraryDependencies <+= sbtVersion(v => "com.github.siasia" %% "xsbt-web-plugin" % (v+"-0.2.11.1"))

//addSbtPlugin("net.databinder" % "posterous-sbt" % "0.3.2")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")
