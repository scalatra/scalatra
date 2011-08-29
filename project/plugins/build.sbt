resolvers ++= Seq(
  "Web plugin repo" at "http://siasia.github.com/maven2",
  Resolver.url("Typesafe repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns)
)

libraryDependencies <++= sbtVersion(sv => Seq(
  "com.github.siasia" %% "xsbt-web-plugin" % ("0.1.0-" + sv),
  "net.databinder" %% "posterous-sbt" % ("0.3.0_sbt" + sv)
))
