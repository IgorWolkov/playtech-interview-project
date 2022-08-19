import Dependencies._

ThisBuild / organization := "playtech-interview-project"
ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.8"

lazy val `playtech-interview-project` = (project in file("."))
  .settings(noPublish)
  .settings(commonSettings: _*)

val commonSettings = List(
  libraryDependencies ++= dependencies,
  Test / parallelExecution := false,
  scalacOptions ++= Seq(
        "-deprecation",
        "-unchecked",
        "-Xfatal-warnings"
    ),
  javacOptions ++= Seq(
        "-source", "11",
        "-target", "11"
    )
)

lazy val noPublish = List(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  skip / publish := true
)

lazy val dependencies = akka ++ scalaTest ++ logback

