scalacOptions ++= Seq("-unchecked", "-deprecation")

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.plugcodahale.com"
)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.2")
