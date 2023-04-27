import sbt._
import Keys._

object Dependencies {
  lazy val parserCombinators        =  "org.scala-lang.modules"  %% "scala-parser-combinators"   % "2.2.0"
  lazy val xml                      =  "org.scala-lang.modules"  %% "scala-xml"                  % "2.1.0"
  lazy val collectionCompact        =  "org.scala-lang.modules"  %% "scala-collection-compat"    % "2.9.0"
  lazy val commonsText              =  "org.apache.commons"      %  "commons-text"               % "1.10.0"
  lazy val httpclient               =  "org.apache.httpcomponents.client5" % "httpclient5"       % "5.2.1"
  lazy val jettyServlet             =  "org.eclipse.jetty"       %  "jetty-servlet"              % jettyVersion
  lazy val jettyWebapp              =  "org.eclipse.jetty"       %  "jetty-webapp"               % jettyVersion
  lazy val json4sCore               =  "org.json4s"              %% "json4s-core"                % json4sVersion
  lazy val json4sJackson            =  "org.json4s"              %% "json4s-jackson"             % json4sVersion
  lazy val json4sNative             =  "org.json4s"              %% "json4s-native"              % json4sVersion
  lazy val json4sXml                =  "org.json4s"              %% "json4s-xml"                 % json4sVersion
  lazy val scalatestJunit           =  "org.scalatestplus"       %% "junit-4-13"                 % "3.2.15.0"
  lazy val jUniversalChardet        =  "com.github.albfernandez" %  "juniversalchardet"          % "2.4.0"
  lazy val logbackClassic           =  "ch.qos.logback"          %  "logback-classic"            % "1.4.7"
  lazy val mockitoAll               =  "org.mockito"             %  "mockito-core"               % "5.3.1"
  lazy val scalatest                =  Seq(
                                         "funspec",
                                         "wordspec",
                                         "flatspec",
                                         "freespec",
                                         "featurespec",
                                         "funsuite",
                                         "shouldmatchers",
                                         "mustmatchers",
                                       ).map(x => "org.scalatest" %% s"scalatest-$x" % scalatestVersion)
  lazy val servletApi               =  "javax.servlet"           %  "javax.servlet-api"          % "4.0.1"
  lazy val slf4jApi                 =  "org.slf4j"               %  "slf4j-api"                  % "2.0.7"
  lazy val specs2                   =  Seq(
                                       "org.specs2"              %% "specs2-core",
                                       "org.specs2"              %% "specs2-matcher-extra"
                                       ).map(_ % specs2Version)
  lazy val metricsScala             =  "nl.grons"                %% "metrics4-scala"             % "4.2.9"
  lazy val metricsServlets          =  "io.dropwizard.metrics"   %  "metrics-servlets"           % "4.2.18"
  lazy val metricsServlet           =  "io.dropwizard.metrics"   %  "metrics-servlet"            % "4.2.18"
  lazy val googleGuava              =  "com.google.guava"        %  "guava"                      % "31.1-jre"
  lazy val twirlApi                 =  "com.typesafe.play"       %% "twirl-api"                  % "1.6.0-RC2"

  private val jettyVersion            = "10.0.15"
  private val json4sVersion           = "4.0.6"
  private val specs2Version           = "4.20.0"
  private val scalatestVersion        = "3.2.15"
}
