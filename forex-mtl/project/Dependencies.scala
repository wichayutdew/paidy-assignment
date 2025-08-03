import sbt.*

object Dependencies {

  object Versions {
    val cats       = "2.6.1"
    val catsEffect = "2.5.1"
    val fs2        = "2.5.4"
    val http4s     = "0.22.15"
    val circe      = "0.14.2"
    val pureConfig = "0.17.4"
    val enumeratum = "1.9.0"
    val vault      = "5.1.0"
    val redis      = "6.7.1.RELEASE"

    val kindProjector    = "0.13.2"
    val logback          = "1.2.3"
    val scalaLogging     = "3.9.4"
    val openTelemetry    = "1.24.0"
    val scalaCheck       = "1.15.3"
    val scalaTest        = "3.2.7"
    val catsScalaCheck   = "0.3.2"
    val mockitoScalaTest = "1.17.45"
  }

  object Libraries {
    def circe(artifact: String): ModuleID  = "io.circe"   %% artifact % Versions.circe
    def http4s(artifact: String): ModuleID = "org.http4s" %% artifact % Versions.http4s

    lazy val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
    lazy val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect
    lazy val fs2        = "co.fs2"        %% "fs2-core"    % Versions.fs2

    lazy val http4sDsl       = http4s("http4s-dsl")
    lazy val http4sServer    = http4s("http4s-blaze-server")
    lazy val http4sClient    = http4s("http4s-blaze-client")
    lazy val http4sCirce     = http4s("http4s-circe")
    lazy val circeCore       = circe("circe-core")
    lazy val circeGeneric    = circe("circe-generic")
    lazy val circeGenericExt = circe("circe-generic-extras")
    lazy val circeParser     = circe("circe-parser")
    lazy val pureConfig      = "com.github.pureconfig" %% "pureconfig" % Versions.pureConfig
    lazy val enumeratum      = "com.beachape"          %% "enumeratum" % Versions.enumeratum

    // Compiler plugins
    lazy val kindProjector = "org.typelevel" %% "kind-projector" % Versions.kindProjector cross CrossVersion.full

    // Runtime
    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % Versions.scalaLogging

    // Test
    lazy val scalaTest        = "org.scalatest"     %% "scalatest"               % Versions.scalaTest
    lazy val scalaCheck       = "org.scalacheck"    %% "scalacheck"              % Versions.scalaCheck
    lazy val catsScalaCheck   = "io.chrisdavenport" %% "cats-scalacheck"         % Versions.catsScalaCheck
    lazy val mockitoScalaTest = "org.mockito"       %% "mockito-scala-scalatest" % Versions.mockitoScalaTest
  }

  object JavaLibraries {
    def openTelemetry(artifact: String, isAlphaVersion: Boolean = false): ModuleID =
      "io.opentelemetry" % artifact % (Versions.openTelemetry + (if (isAlphaVersion) "-alpha" else ""))

    lazy val openTelemetryApi              = openTelemetry("opentelemetry-api")
    lazy val openTelemetrySdkAutoConfigure =
      openTelemetry("opentelemetry-sdk-extension-autoconfigure", isAlphaVersion = true)
    lazy val openTelemetryExporterPrometheus = openTelemetry("opentelemetry-exporter-prometheus", isAlphaVersion = true)
    lazy val openTelemetryJavaAgent          =
      "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % Versions.openTelemetry % "runtime"
    lazy val logback = "ch.qos.logback"  % "logback-classic"   % Versions.logback
    lazy val vault   = "com.bettercloud" % "vault-java-driver" % Versions.vault
    lazy val redis   = "io.lettuce"      % "lettuce-core"      % Versions.redis
  }

}
