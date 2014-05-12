resolvers ++= Seq(
  Resolver.url("Typesafe repository", new java.net.URL("http://typesafe.artifactoryonline.com/typesafe/ivy-releases/"))(Resolver.defaultIvyPatterns)
)

addSbtPlugin("com.earldouglas" % "xsbt-web-plugin" % "0.4.0")