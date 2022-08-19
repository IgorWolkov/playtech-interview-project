import sbt._

object Dependencies {
  
  object Version {
    val akka      = "2.6.19"
    val scalaTest = "3.2.13"
    val logback   = "1.3.0-alpha16"
  }

  lazy val akka: Seq[ModuleID] = Seq(
    "com.typesafe.akka" %% "akka-actor",
    "com.typesafe.akka" %% "akka-testkit"
  ).map(_ % Version.akka)

  lazy val scalaTest: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest"
  ).map(_ % Version.scalaTest % Test)

  lazy val logback: Seq[ModuleID] = Seq(
    "ch.qos.logback" % "logback-classic"
  ).map(_ % Version.logback)

}
