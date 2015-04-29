name := """cros-core"""

version := "1.0-SNAPSHOT"

lazy val droneapi = (project in file("drone-api"))

lazy val dronesimulator = (project in file("drone-simulator"))
  .dependsOn(droneapi % "compile->compile")

lazy val root = (project in file("."))
	.enablePlugins(PlayJava)
	.dependsOn(droneapi % "compile->compile", dronesimulator % "compile->compile")

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  javaWs,
  "org.postgresql" % "postgresql" % "9.4-1200-jdbc41",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.3.9",
  "commons-codec" % "commons-codec" % "1.10",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "com.typesafe.akka" % "akka-stream-experimental_2.11" % "1.0-M5"
)

// Code coverage

jacoco.settings

parallelExecution in jacoco.Config := false

jacoco.outputDirectory in jacoco.Config := file("target/jacoco")

jacoco.excludes        in jacoco.Config := Seq(
  "views*",
  "*Routes*",
  "controllers*routes*",
  "controllers*Reverse*",
  "controllers*javascript*",
  "controller*ref*"
)

