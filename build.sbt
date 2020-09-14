val appName = "bootstrap-play-26"

lazy val library = Project(appName, file("."))
  .enablePlugins(SbtAutoBuildPlugin, SbtGitVersioning, SbtArtifactory)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    majorVersion := 1,
    makePublicallyAvailableOnBintray := true
  )
  .settings(
    scalaVersion := "2.11.12",
    crossScalaVersions := List("2.11.12", "2.12.8"),
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    fork in Test := true,
    scalacOptions ++= Seq("-deprecation")
  )
  .settings(
    publishArtifact in Test := true,
    mappings in (Test, packageBin) ~= { ms: Seq[(File, String)] =>
      ms filter {
        case (_, toPath) =>
          toPath startsWith "uk.gov.hmrc.play.bootstrap.tools".replace(".", java.io.File.separator)
      }
    }
  )
