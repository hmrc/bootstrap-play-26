import sbt._

object AppDependencies {

  private val playVersion = "2.6.20"

  val compile = Seq(
    "ch.qos.logback"        % "logback-core"         % "1.1.7",
    "com.kenshoo"           %% "metrics-play"        % "2.6.6_0.6.2",
    "com.typesafe.play"     %% "filters-helpers"     % playVersion,
    "com.typesafe.play"     %% "play"                % playVersion,
    "com.typesafe.play"     %% "play-guice"          % playVersion,
    "com.typesafe.play"     %% "play-ahc-ws"         % playVersion,
    "io.dropwizard.metrics" % "metrics-graphite"     % "3.2.5",
    "uk.gov.hmrc"           %% "auth-client"         % "2.27.0-play-26",
    "uk.gov.hmrc"           %% "crypto"              % "5.3.0",
    "uk.gov.hmrc"           %% "http-verbs"          % "10.0.0-play-26",
    "uk.gov.hmrc"           %% "logback-json-logger" % "4.4.0",
    "uk.gov.hmrc"           %% "play-auditing"       % "4.2.0-play-26",
    "uk.gov.hmrc"           %% "play-health"         % "3.14.0-play-26",
    "uk.gov.hmrc"           %% "time"                % "3.6.0",
    // force dependencies due to security flaws found in jackson-databind < 2.9.x using XRay
    "com.fasterxml.jackson.core"     % "jackson-core"            % "2.9.7",
    "com.fasterxml.jackson.core"     % "jackson-databind"        % "2.9.7",
    "com.fasterxml.jackson.core"     % "jackson-annotations"     % "2.9.7",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8"   % "2.9.7",
    "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % "2.9.7"
  )

  val test = Seq(
    "com.github.tomakehurst" % "wiremock-jre8"       % "2.21.0",
    "com.typesafe.play"      %% "play-test"          % playVersion,
    "org.mockito"            % "mockito-all"         % "1.9.5",
    "org.pegdown"            % "pegdown"             % "1.5.0",
    "org.scalacheck"         %% "scalacheck"         % "1.14.0",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"
  ).map(_ % Test)

}
