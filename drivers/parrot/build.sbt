name := """cros-parrot-drivers"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

lazy val parrotdrivers= (project in file("."))

javaSource in Compile := baseDirectory.value / "src"

libraryDependencies ++= Seq(
  // add dependeny on drone-api here
	"com.typesafe.akka" % "akka-actor_2.11" % "2.3.4",
	"joda-time" % "joda-time" % "2.3",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.3.2"
)
