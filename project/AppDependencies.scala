import play.sbt.PlayImport.{filters, guice}
import sbt._

object AppDependencies {

  import play.core.PlayVersion

  val compile = Seq(
    guice,
    filters,
    "uk.gov.hmrc"           %% "crypto"              % "4.4.0",
    "uk.gov.hmrc"           %% "http-verbs"          % "7.3.0",
    "uk.gov.hmrc"           %% "http-verbs-play-26"  % "0.2.0",
    "uk.gov.hmrc"           %% "play-auditing"       % "3.3.0",
    "uk.gov.hmrc"           %% "auth-client"         % "2.6.0",
    "uk.gov.hmrc"           %% "logback-json-logger" % "3.1.0",
    "com.typesafe.play"     %% "play"                % PlayVersion.current,
    "io.dropwizard.metrics" % "metrics-graphite"     % "3.2.5",
    "de.threedimensions"    %% "metrics-play"        % "2.5.13",
    "ch.qos.logback"        % "logback-core"         % "1.1.7"
//    "uk.gov.hmrc"           %% "play-health"         % "2.1.0" todo (konrad) needs updating to 2.6
  )

  val test = Seq(
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current % Test,
    "org.scalacheck"         %% "scalacheck"         % "1.14.0"            % Test,
    "org.mockito"            % "mockito-all"         % "1.9.5"             % Test,
    "org.pegdown"            % "pegdown"             % "1.5.0"             % Test,
    "com.github.tomakehurst" % "wiremock"            % "2.7.1"             % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"             % Test,
    "com.typesafe.play"      %% "play-iteratees"     % "2.6.1"             % Test
  )

}
