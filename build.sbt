import scala.xml._
import Dependencies._
import com.typesafe.tools.mima.core._

val unusedOptions = Seq("-Ywarn-unused:imports")

val javax = ServletCross("-javax", "-javax")
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

def Scala213 = "2.13.15"
val scalaVersions = Seq("2.12.20", Scala213, "3.3.3")

lazy val scalatraSettings = Seq(
  organization := "org.scalatra",
  mimaPreviousArtifacts ++= Set("3.1.0").map(
    organization.value %% moduleName.value % _
  ),
  mimaBinaryIssueFilters ++= Seq(
    ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.package$"),
  ),
  mimaBinaryIssueFilters ++= {
    scalaBinaryVersion.value match {
      case "2.13" =>
        Seq(
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.ActionResult.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.ActionResult.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.CookieOptions.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.CookieOptions.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.ExtensionMethod.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.ExtensionMethod.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.HaltException.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.HaltException.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.MatchedRoute.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.MatchedRoute.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.PathPattern.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.PathPattern.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.FileItem.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.FileItem.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.HttpServletRequestReadOnly.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.HttpServletRequestReadOnly.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.MultipartConfig.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.MultipartConfig.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.RichResponse.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.RichResponse.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.RichServletContext.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.RichServletContext.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.RichSession.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.servlet.RichSession.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Api.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Api.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ApiInfo.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ApiInfo.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ApiKey.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ApiKey.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ApplicationGrant.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ApplicationGrant.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.AuthorizationCodeGrant.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.AuthorizationCodeGrant.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.BasicAuth.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.BasicAuth.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ContactInfo.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ContactInfo.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Endpoint.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Endpoint.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ImplicitGrant.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ImplicitGrant.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.LicenseInfo.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.LicenseInfo.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.LoginEndpoint.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.LoginEndpoint.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Model.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Model.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ModelProperty.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ModelProperty.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.OAuth.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.OAuth.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Operation.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Operation.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Parameter.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.Parameter.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ResponseMessage.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.ResponseMessage.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.TokenEndpoint.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.TokenEndpoint.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.TokenRequestEndpoint.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.TokenRequestEndpoint.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.ClassDescriptor.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.ClassDescriptor.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.ConstructorDescriptor.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.ConstructorDescriptor.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.ConstructorParamDescriptor.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.ConstructorParamDescriptor.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.ManifestScalaType.typeArgs"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.PrimitiveDescriptor.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.PrimitiveDescriptor.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.PropertyDescriptor.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.PropertyDescriptor.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.SingletonDescriptor.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.swagger.reflect.SingletonDescriptor.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.BytesPart.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.BytesPart.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.FilePart.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.FilePart.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.HttpComponentsClientResponse.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.HttpComponentsClientResponse.compose"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.ResponseStatus.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.ResponseStatus.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.SimpleResponse.curried"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.SimpleResponse.tupled"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.UploadableBody.andThen"),
          ProblemFilters.exclude[DirectMissingMethodProblem]("org.scalatra.test.UploadableBody.compose"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.ActionResult$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.CookieOptions$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.CorsSupport$CORSConfig$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.ExtensionMethod$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.HaltException$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.MatchedRoute$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.PathPattern$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.RailsRouteMatcher$Builder$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.RegexPathPatternParser$PartialPathPattern$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.SinatraRouteMatcher$Builder$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.servlet.FileItem$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.servlet.FileUploadSupport$BodyParams$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.servlet.HttpServletRequestReadOnly$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.servlet.MultipartConfig$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.servlet.RichResponse$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.servlet.RichServletContext$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.servlet.RichSession$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.AllowableValues$AllowableRangeValues$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.Api$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.ApiInfo$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.ApiKey$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.ApplicationGrant$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.AuthorizationCodeGrant$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.BasicAuth$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.ContactInfo$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.DataType$ContainerDataType$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.DataType$ValueDataType$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.Endpoint$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.ImplicitGrant$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.LicenseInfo$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.LoginEndpoint$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.Model$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.ModelProperty$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.OAuth$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.Operation$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.Parameter$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.ResponseMessage$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.SwaggerSupportSyntax$RailsSwaggerGenerator$Builder$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.SwaggerSupportSyntax$SinatraSwaggerGenerator$Builder$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.TokenEndpoint$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.TokenRequestEndpoint$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.reflect.ClassDescriptor$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.reflect.ConstructorDescriptor$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.reflect.ConstructorParamDescriptor$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.reflect.PrimitiveDescriptor$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.reflect.PropertyDescriptor$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.swagger.reflect.SingletonDescriptor$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.test.BytesPart$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.test.FilePart$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.test.HttpComponentsClientResponse$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.test.ResponseStatus$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.test.SimpleResponse$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.test.UploadableBody$"),
          ProblemFilters.exclude[MissingTypesProblem]("org.scalatra.util.conversion.Extractors$DateExtractor$"),
        )
      case _ =>
        Nil
    }
  },
  Test / fork := true,
  Test / baseDirectory := (ThisBuild / baseDirectory).value,
  Test / testOptions ++= {
    if (scalaBinaryVersion.value == "3") {
      Seq(
        Tests.Exclude(Set(
          "org.scalatra.swagger.ModelSpec",
          "org.scalatra.swagger.SwaggerSpec2",
          "org.scalatra.swagger.ModelCollectionSpec",
        )),
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
        case _ => sys.error(axes.toString)
      }
    }
  },
  scalacOptions ++= {
    scalaBinaryVersion.value match {
      case "2.12" =>
        Seq("-Xsource:3")
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
    "-source", "17",
    "-target", "17",
  ),
  scalacOptions ++= Seq(
    "-unchecked",
    "-deprecation",
    /*"-Yinline-warnings",*/
    "-encoding", "utf8",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:existentials",
    "-release:17"
  ),
  manifestSetting,
  publishConfiguration := publishConfiguration.value.withOverwrite(true),
  publishLocalConfiguration := publishLocalConfiguration.value.withOverwrite(true)
) ++ mavenCentralFrouFrou ++ Seq(Compile, Test).flatMap(c =>
  c / console / scalacOptions --= unusedOptions
)

scalatraSettings
scalaVersion := Scala213
name := "scalatra-unidoc"
mimaPreviousArtifacts := Set.empty
mimaFailOnNoPrevious := false
artifacts := Classpaths.artifactDefs(Seq(Compile / packageDoc, Compile / makePom)).value
packagedArtifacts := Classpaths.packaged(Seq(Compile / packageDoc, Compile / makePom)).value
description := "A tiny, Sinatra-like web framework for Scala"
shellPrompt := { state =>
  s"sbt:${Project.extract(state).currentProject.id}" + Def.withColor("> ", Option(scala.Console.CYAN))
}
Defaults.packageTaskSettings(
  Compile / packageDoc, (Compile / unidoc).map(_.flatMap(Path.allSubpaths))
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
  ).map(_.finder(jakarta, VirtualAxis.jvm)(Scala213): ProjectReference): _*
)
enablePlugins(ScalaUnidocPlugin)

lazy val `scalatra-common` = projectMatrix.in(file("common"))
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

lazy val scalatra = projectMatrix.in(file("core"))
  .settings(
    scalatraSettings,
    libraryDependencies ++= Seq(
      slf4jApi,
      jUniversalChardet,
      commonsText,
      parserCombinators,
      xml,
      collectionCompact,
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
  ).dependsOn(
    `scalatra-specs2` % "test->compile",
    `scalatra-scalatest` % "test->compile",
    `scalatra-common` % "compile;test->test",
    `scalatra-compat` % "compile;test->test"
  )

lazy val `scalatra-auth` = projectMatrix.in(file("auth"))
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

lazy val `scalatra-twirl` = projectMatrix.in(file("twirl"))
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

lazy val `scalatra-json` = projectMatrix.in(file("json"))
  .settings(
    scalatraSettings,
    description := "JSON support for Scalatra",
    libraryDependencies ++= Seq(
      json4sJackson % "provided",
      json4sNative % "provided",
      json4sCore,
      json4sXml
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

lazy val `scalatra-forms` = projectMatrix.in(file("forms"))
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

lazy val `scalatra-jetty` = projectMatrix.in(file("jetty"))
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

lazy val `scalatra-test` = projectMatrix.in(file("test"))
  .settings(
    scalatraSettings,
    libraryDependencies ++= Seq(
      mockitoAll,
      httpclient,
      collectionCompact
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

lazy val `scalatra-scalatest` = projectMatrix.in(file("scalatest"))
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

lazy val `scalatra-specs2` = projectMatrix.in(file("specs2"))
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

lazy val `scalatra-swagger` = projectMatrix.in(file("swagger"))
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
    scalatra % "compile;test->test;provided->provided",
    `scalatra-json` % "compile;test->test;provided->provided",
    `scalatra-auth` % "compile;test->test"
  )

lazy val `scalatra-metrics` = projectMatrix.in(file("metrics"))
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

lazy val `scalatra-cache` = projectMatrix.in(file("cache"))
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

lazy val `scalatra-compat` = projectMatrix.in(file("compat"))
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
    "Created-By" -> "Simple Build Tool",
    "Built-By" -> System.getProperty("user.name"),
    "Build-Jdk" -> System.getProperty("java.version"),
    "Specification-Title" -> name.value,
    "Specification-Version" -> version.value,
    "Specification-Vendor" -> organization.value,
    "Implementation-Title" -> name.value,
    "Implementation-Version" -> version.value,
    "Implementation-Vendor-Id" -> organization.value,
    "Implementation-Vendor" -> organization.value
  )
}

// Things we care about primarily because Maven Central demands them
lazy val mavenCentralFrouFrou = Seq(
  homepage := Some(url("https://www.scalatra.org/")),
  startYear := Some(2009),
  licenses := Seq(("BSD", url("https://github.com/scalatra/scalatra/raw/HEAD/LICENSE"))),
  pomExtra := pomExtra.value ++ Group(
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
        <url>hhttps://github.com/ayush</url>
      </developer>
      <developer>
        <id>seratch</id>
        <name>Kazuhiro Sera</name>
        <url>hhttps://github.com/seratch</url>
      </developer>
    </developers>
  )
)
