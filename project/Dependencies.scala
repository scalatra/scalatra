import sbt._
import Keys._

object Dependencies {
  lazy val parserCombinators        = Def.setting("org.scala-lang.modules"  %% "scala-parser-combinators"   % parserCombinatorVersion.value)
  lazy val xml                      =  "org.scala-lang.modules"  %% "scala-xml"                  % "1.3.0"
  lazy val akkaActor                =  "com.typesafe.akka"       %% "akka-actor"                 % akkaVersion
  lazy val akkaTestkit              =  "com.typesafe.akka"       %% "akka-testkit"               % akkaVersion
  lazy val atmosphereRuntime        =  "org.atmosphere"          %  "atmosphere-runtime"         % "2.5.15"
  lazy val atmosphereJQuery         =  "org.atmosphere.client"   %  "jquery"                     % "2.2.21" artifacts(Artifact("jquery", "war", "war"))
  lazy val atmosphereClient         =  "org.atmosphere"          %  "wasync"                     % "2.1.7"
  lazy val atmosphereRedis          =  "org.atmosphere"          %  "atmosphere-redis"           % "2.5.2"
  lazy val atmosphereCompatJbossweb =  "org.atmosphere"          %  "atmosphere-compat-jbossweb" % atmosphereCompatVersion
  lazy val atmosphereCompatTomcat   =  "org.atmosphere"          %  "atmosphere-compat-tomcat"   % atmosphereCompatVersion
  lazy val atmosphereCompatTomcat7  =  "org.atmosphere"          %  "atmosphere-compat-tomcat7"  % atmosphereCompatVersion
  lazy val commonsFileupload        =  "commons-fileupload"      %  "commons-fileupload"         % "1.3.3"
  lazy val commonsIo                =  "commons-io"              %  "commons-io"                 % "2.5"
  lazy val commonsText              =  "org.apache.commons"      %  "commons-text"               % "1.8"
  lazy val httpclient               =  "org.apache.httpcomponents" % "httpclient"                % httpcomponentsVersion
  lazy val httpmime                 =  "org.apache.httpcomponents" % "httpmime"                  % httpcomponentsVersion
  lazy val jettyServer              =  "org.eclipse.jetty"       %  "jetty-server"               % jettyVersion
  lazy val jettyPlus                =  "org.eclipse.jetty"       %  "jetty-plus"                 % jettyVersion
  lazy val jettyServlet             =  "org.eclipse.jetty"       %  "jetty-servlet"              % jettyVersion
  lazy val jettyWebsocket           =  "org.eclipse.jetty.websocket" %"websocket-server"         % jettyVersion
  lazy val jettyWebapp              =  "org.eclipse.jetty"       %  "jetty-webapp"               % jettyVersion
  lazy val json4sCore               =  "org.json4s"              %% "json4s-core"                % json4sVersion
  lazy val json4sExt                =  "org.json4s"              %% "json4s-ext"                 % json4sVersion
  lazy val json4sJackson            =  "org.json4s"              %% "json4s-jackson"             % json4sVersion
  lazy val json4sNative             =  "org.json4s"              %% "json4s-native"              % json4sVersion
  lazy val json4sXml                =  "org.json4s"              %% "json4s-xml"                 % json4sVersion
  lazy val junit                    =  "junit"                   %  "junit"                      % "4.13"
  lazy val scalatestJunit           =  "org.scalatestplus"       %% "junit-4-12"                 % "3.1.2.0"
  lazy val jUniversalChardet        =  "com.googlecode.juniversalchardet" % "juniversalchardet"  % "1.0.3"
  lazy val logbackClassic           =  "ch.qos.logback"          %  "logback-classic"            % "1.2.3"
  lazy val mockitoAll               =  "org.mockito"             %  "mockito-core"               % "3.3.3"
  lazy val scalate                  =  "org.scalatra.scalate"    %% "scalate-core"               % scalateVersion
  lazy val scalatest                =  "org.scalatest"           %% "scalatest"                  % scalatestVersion
  lazy val servletApi               =  "javax.servlet"           %  "javax.servlet-api"          % "3.1.0"
  lazy val slf4jApi                 =  "org.slf4j"               %  "slf4j-api"                  % "1.7.30"
  lazy val specs2                   =  Seq(
                                       "org.specs2"              %% "specs2-core",
                                       "org.specs2"              %% "specs2-mock",
                                       "org.specs2"              %% "specs2-matcher-extra"
                                                                                  ).map(_        % specs2Version)
  lazy val metricsScala             =  "nl.grons"                %% "metrics4-scala"             % "4.1.5"
  lazy val metricsServlets          =  "io.dropwizard.metrics"   %  "metrics-servlets"           % "4.1.6"
  lazy val metricsServlet           =  "io.dropwizard.metrics"   %  "metrics-servlet"            % "4.1.6"
  lazy val googleGuava              =  "com.google.guava"        %  "guava"                      % "29.0-jre"
  lazy val twirlApi                 =  "com.typesafe.play"       %% "twirl-api"                  % "1.4.2"

  private val akkaVersion             = "2.5.31"
  private val atmosphereCompatVersion = "2.0.1"
  private val httpcomponentsVersion   = "4.5.6"
  private val jettyVersion            = "9.4.29.v20200521"
  private val json4sVersion           = "3.6.8"
  private val scalateVersion          = "1.9.6"
  private val scalatestVersion        = "3.1.2"
  private val specs2Version           = "4.9.4"
  private val parserCombinatorVersion = Def.setting(
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) =>
        // https://github.com/scala/scala-parser-combinators/issues/197
        "1.1.1"
      case _ =>
        "1.1.2"
    }
  )
}
