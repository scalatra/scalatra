import scala.xml._
import Dependencies._
import com.typesafe.tools.mima.core._

val unusedOptions = Seq("-Ywarn-unused:imports")

val javax   = ServletCross("-javax", "-javax")
val jakarta = ServletCross("-jakarta", "-jakarta")

val scala3migration = Def.settings(
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "3") {
      Seq(
        "-source:3.0-migration",
      )
    } else {
      Nil
    }
  }
)

def Scala213      = "2.13.18"
val scalaVersions = Seq(Scala213, "3.3.7")

lazy val scalatraSettings = Seq(
  organization := "org.scalatra",
  mimaPreviousArtifacts ++= Set("3.1.2").map(
    organization.value %% moduleName.value % _
  ),
  mimaBinaryIssueFilters ++= Seq(
    ProblemFilters.exclude[DirectMissingMethodProblem](
      "org.scalatra.test.EmbeddedJettyContainer.servletContextHandler"
    ),
    ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.JettyContainer.addServlet"),
    ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.JettyContainer.addFilter"),
    ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.JettyContainer.mount"),
    ProblemFilters.exclude[IncompatibleResultTypeProblem]("org.scalatra.jetty.JettyServer.context"),
    ProblemFilters.exclude[IncompatibleResultTypeProblem]("org.scalatra.test.JettyContainer.servletContextHandler"),
    ProblemFilters.exclude[ReversedMissingMethodProblem]("org.scalatra.test.JettyContainer.servletContextHandler")
  ),
  Test / fork          := true,
  Test / baseDirectory := (ThisBuild / baseDirectory).value,
  Test / testOptions ++= {
    if (scalaBinaryVersion.value == "3") {
      Seq(
        Tests.Exclude(
          Set(
            "org.scalatra.swagger.ModelSpec",
            "org.scalatra.swagger.SwaggerSpec2",
            "org.scalatra.swagger.ModelCollectionSpec",
          )
        ),
      )
    } else {
      Nil
    }
  },
  name := {
    if (baseDirectory.value == (LocalRootProject / baseDirectory).value) {
      name.value
    } else {
      val axes = virtualAxes.?.value.getOrElse(Nil)
      (axes.contains(javax), axes.contains(jakarta)) match {
        case (true, false) => s"${name.value}-javax"
        case (false, true) => s"${name.value}-jakarta"
        case _             => sys.error(axes.toString)
      }
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.13" =>
        Seq("-Xsource:3-cross")
      case _ =>
        Nil
    }
  },
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, _)) =>
        unusedOptions ++ Seq(
          "-Xlint",
          "-Xcheckinit",
        )
      case _ =>
        Nil
    }
  },
  javacOptions ++= Seq(
    "-source",
    "17",
    "-target",
    "17",
  ),
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    /*"-Yinline-warnings",*/
    "-encoding",
    "utf8",
    "-feature",
    "-language:implicitConversions",
    "-language:existentials",
    "-release:17"
  ),
  manifestSetting,
  publishConfiguration      := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
) ++ mavenCentralFrouFrou ++ Seq(Compile, Test).flatMap(c => c / console / scalacOptions --= unusedOptions)

scalatraSettings
scalaVersion          := Scala213
name                  := "scalatra-unidoc"
mimaPreviousArtifacts := Set.empty
mimaFailOnNoPrevious  := false
artifacts             := Classpaths.artifactDefs(Seq(Compile / packageDoc, Compile / makePom)).value
packagedArtifacts     := Classpaths.packaged(Seq(Compile / packageDoc, Compile / makePom)).value
description           := "A tiny, Sinatra-like web framework for Scala"
shellPrompt           := { state =>
  s"sbt:${Project.extract(state).currentProject.id}" + Def.withColor("> ", Option(scala.Console.CYAN))
}
Defaults.packageTaskSettings(
  Compile / packageDoc,
  (Compile / unidoc).map(_.flatMap(Path.allSubpaths))
)
ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
  Seq(
    scalatra,
    `scalatra-auth`,
    `scalatra-forms`,
    `scalatra-twirl`,
    `scalatra-json`,
    `scalatra-test`,
    `scalatra-scalatest`,
    `scalatra-specs2`,
    `scalatra-swagger`,
    `scalatra-jetty`,
    `scalatra-common`,
    `scalatra-metrics`,
    `scalatra-cache`,
    `scalatra-compat`,
  ).map(_.finder(jakarta, VirtualAxis.jvm)(Scala213): ProjectReference) *
)
enablePlugins(ScalaUnidocPlugin)

lazy val `scalatra-common` = projectMatrix
  .in(file("common"))
  .settings(
    scalatraSettings
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(
    `scalatra-compat` % "compile;test->test;provided->provided"
  )

lazy val scalatra = projectMatrix
  .in(file("core"))
  .settings(
    scalatraSettings,
    libraryDependencies ++= Seq(
      slf4jApi,
      jUniversalChardet,
      commonsText,
      parserCombinators,
      xml,
    ),
    description := "The core Scalatra framework"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
      libraryDependencies += servletApiJavax % "provided;test",
    )
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
      libraryDependencies += servletApiJakarta % "provided;test",
    )
  )
  .dependsOn(
    `scalatra-specs2`    % "test->compile",
    `scalatra-scalatest` % "test->compile",
    `scalatra-common`    % "compile;test->test",
    `scalatra-compat`    % "compile;test->test"
  )

lazy val `scalatra-auth` = projectMatrix
  .in(file("auth"))
  .settings(
    scalatraSettings,
    description := "Scalatra authentication module",
    scala3migration,
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(
    scalatra % "compile;test->test;provided->provided"
  )

lazy val `scalatra-twirl` = projectMatrix
  .in(file("twirl"))
  .settings(
    scalatraSettings,
    libraryDependencies += twirlApi,
    description := "Twirl integration with Scalatra"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(
    scalatra % "compile;test->test;provided->provided"
  )

lazy val `scalatra-json` = projectMatrix
  .in(file("json"))
  .settings(
    scalatraSettings,
    description := "JSON support for Scalatra",
    libraryDependencies ++= Seq(
      json4sJackson % "provided",
      json4sNative  % "provided",
      json4sCore,
      json4sXml,
      json4sScalap,
    )
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(scalatra % "compile;test->test;provided->provided")

lazy val `scalatra-forms` = projectMatrix
  .in(file("forms"))
  .settings(
    scalatraSettings,
    description := "Data binding and validation for Scalatra"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(scalatra % "compile;test->test;provided->provided")

lazy val `scalatra-jetty` = projectMatrix
  .in(file("jetty"))
  .settings(
    scalatraSettings,
    description := "Embedded Jetty server for Scalatra apps"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
      libraryDependencies ++= Seq(
        servletApiJavax,
        jettyWebappJavax
      ),
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
      libraryDependencies ++= Seq(
        servletApiJakarta,
        jettyWebappJakarta
      ),
    ),
  )
  .dependsOn(
    `scalatra` % "compile;test->test;provided->provided"
  )

lazy val `scalatra-test` = projectMatrix
  .in(file("test"))
  .settings(
    scalatraSettings,
    libraryDependencies ++= Seq(
      mockitoAll,
      httpclient,
    ) ++ specs2.map(_ % "test"),
    description := "The abstract Scalatra test framework"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings()
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings()
  )
  .dependsOn(
    `scalatra-common` % "compile;test->test;provided->provided",
    `scalatra-compat` % "compile;test->test;provided->provided",
  )

lazy val `scalatra-scalatest` = projectMatrix
  .in(file("scalatest"))
  .settings(
    scalatraSettings,
    libraryDependencies ++= scalatest,
    libraryDependencies ++= Seq(
      scalatestJunit,
    ),
    description := "ScalaTest support for the Scalatra test framework"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(`scalatra-test` % "compile;test->test;provided->provided")

lazy val `scalatra-specs2` = projectMatrix
  .in(file("specs2"))
  .settings(
    scalatraSettings,
    libraryDependencies ++= specs2,
    description := "Specs2 support for the Scalatra test framework"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(`scalatra-test` % "compile;test->test;provided->provided")

lazy val `scalatra-swagger` = projectMatrix
  .in(file("swagger"))
  .settings(
    scalatraSettings,
    libraryDependencies ++= Seq(
      parserCombinators,
      logbackClassic % "provided"
    ),
    description := "Scalatra integration with Swagger"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(
    scalatra        % "compile;test->test;provided->provided",
    `scalatra-json` % "compile;test->test;provided->provided",
    `scalatra-auth` % "compile;test->test"
  )

lazy val `scalatra-metrics` = projectMatrix
  .in(file("metrics"))
  .settings(
    scalatraSettings,
    description := "Scalatra integration with Metrics",
    libraryDependencies ++= Seq(
      metricsScala,
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
      libraryDependencies ++= Seq(
        metricsServletsJavax,
        metricsServletJavax
      ),
    )
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
      libraryDependencies ++= Seq(
        metricsServletsJakarta,
        metricsServletJakarta
      ),
    )
  )
  .dependsOn(scalatra % "compile;test->test;provided->provided")

lazy val `scalatra-cache` = projectMatrix
  .in(file("cache"))
  .settings(
    scalatraSettings,
    libraryDependencies ++= Seq(
      googleGuava
    ),
    description := "Scalatra Cache support"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
    ),
  )
  .dependsOn(scalatra % "compile;test->test;provided->provided")

lazy val `scalatra-compat` = projectMatrix
  .in(file("compat"))
  .settings(
    scalatraSettings,
    description := "Scalatra Compatibility module"
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(javax),
    settings = Def.settings(
      libraryDependencies ++= Seq(
        servletApiJavax,
        jettyWebappJavax,
      ),
    ),
  )
  .jvmPlatform(
    scalaVersions = scalaVersions,
    axisValues = Seq(jakarta),
    settings = Def.settings(
      libraryDependencies ++= Seq(
        servletApiJakarta,
        jettyWebappJakarta
      ),
    ),
  )

lazy val manifestSetting = packageOptions += {
  Package.ManifestAttributes(
    "Created-By"               -> "Simple Build Tool",
    "Built-By"                 -> System.getProperty("user.name"),
    "Build-Jdk"                -> System.getProperty("java.version"),
    "Specification-Title"      -> name.value,
    "Specification-Version"    -> version.value,
    "Specification-Vendor"     -> organization.value,
    "Implementation-Title"     -> name.value,
    "Implementation-Version"   -> version.value,
    "Implementation-Vendor-Id" -> organization.value,
    "Implementation-Vendor"    -> organization.value
  )
}

// Things we care about primarily because Maven Central demands them
lazy val mavenCentralFrouFrou = Seq(
  homepage  := Some(url("https://www.scalatra.org/")),
  startYear := Some(2009),
  licenses  := Seq(("BSD", url("https://github.com/scalatra/scalatra/raw/HEAD/LICENSE"))),
  pomExtra  := pomExtra.value ++ Group(
    <scm>
      <url>https://github.com/scalatra/scalatra</url>
      <connection>scm:git:git://github.com/scalatra/scalatra.git</connection>
    </scm>
    <developers>
      <developer>
        <id>riffraff</id>
        <name>Gabriele Renzi</name>
        <url>http://www.riffraff.info</url>
      </developer>
      <developer>
        <id>alandipert</id>
        <name>Alan Dipert</name>
        <url>http://alan.dipert.org</url>
      </developer>
      <developer>
        <id>rossabaker</id>
        <name>Ross A. Baker</name>
        <url>http://www.rossabaker.com/</url>
      </developer>
      <developer>
        <id>chirino</id>
        <name>Hiram Chirino</name>
        <url>http://hiramchirino.com/blog/</url>
      </developer>
      <developer>
        <id>casualjim</id>
        <name>Ivan Porto Carrero</name>
        <url>http://flanders.co.nz/</url>
      </developer>
      <developer>
        <id>jlarmstrong</id>
        <name>Jared Armstrong</name>
        <url>http://www.jaredarmstrong.name/</url>
      </developer>
      <developer>
        <id>mnylen</id>
        <name>Mikko Nylen</name>
        <url>https://github.com/mnylen/</url>
      </developer>
      <developer>
        <id>dozed</id>
        <name>Stefan Ollinger</name>
        <url>https://github.com/dozed/</url>
      </developer>
      <developer>
        <id>sdb</id>
        <name>Stefan De Boey</name>
        <url>https://github.com/sdb/</url>
      </developer>
      <developer>
        <id>ymasory</id>
        <name>Yuvi Masory</name>
        <url>https://github.com/ymasory/</url>
      </developer>
      <developer>
        <id>jfarcand</id>
        <name>Jean-François Arcand</name>
        <url>https://github.com/jfarcand/</url>
      </developer>
      <developer>
        <id>ceedubs</id>
        <name>Cody Alen</name>
        <url>https://github.com/ceedubs/</url>
      </developer>
      <developer>
        <id>BowlingX</id>
        <name>David Heidrich</name>
        <url>https://github.com/BowlingX/</url>
      </developer>
      <developer>
        <id>ayush</id>
        <name>Ayush Gupta</name>
        <url>https://github.com/ayush</url>
      </developer>
      <developer>
        <id>seratch</id>
        <name>Kazuhiro Sera</name>
        <url>https://github.com/seratch</url>
      </developer>
    </developers>
  )
)
