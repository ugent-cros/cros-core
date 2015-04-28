name := """cros-drone-api"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.1"

lazy val droneapi = (project in file("."))

javaSource in Compile := baseDirectory.value / "src"

libraryDependencies ++= Seq(
	"com.typesafe.akka" % "akka-actor_2.11" % "2.3.9"
)
