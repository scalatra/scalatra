scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

addSbtPlugin("com.github.sbt" % "sbt-pgp"           % "2.3.1")
addSbtPlugin("com.github.sbt" % "sbt-unidoc"        % "0.6.0")
addSbtPlugin("com.eed3si9n"   % "sbt-projectmatrix" % "0.11.0")
addSbtPlugin("com.typesafe"   % "sbt-mima-plugin"   % "1.1.4")
addSbtPlugin("org.scalameta"  % "sbt-scalafmt"      % "2.5.6")
