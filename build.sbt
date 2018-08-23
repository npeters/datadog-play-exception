name := """datadog-play-exception"""

scalaVersion := "2.12.4"

version := "1.0i.0"

organization := "fr.canal"

lazy val `datadog-play-exception` = (project in file(".")).enablePlugins(PlayScala).disablePlugins(PlayAkkaHttpServer)

val playVersion = "2.6.11"

libraryDependencies ++= Seq(
  filters,
  nettyServer,
  guice,
  "com.typesafe.play" %% "play" % playVersion  
)
