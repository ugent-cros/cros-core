name := """cros-core"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41"
)

// Code coverage

jacoco.settings

parallelExecution in jacoco.Config := false

jacoco.outputDirectory in jacoco.Config := file("target/jacoco")

jacoco.excludes        in jacoco.Config := Seq(
  "views*", "*Routes*",
  "controllers*routes*",
  "controllers*Reverse*",
  "controllers*javascript*",
  "controller*ref*"
)