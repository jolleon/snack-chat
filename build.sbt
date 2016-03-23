organization := "com.example"
name := "poke-at-http4s"
version := "0.0.1-SNAPSHOT"
scalaVersion := "2.11.8"

enablePlugins(JavaAppPackaging)

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-blaze-server" % "0.12.4",
  "org.http4s" %% "http4s-dsl"          % "0.12.4",
  "org.http4s" %% "http4s-argonaut"     % "0.12.4"
)

libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.6.4"

libraryDependencies ++= Seq(
  "org.postgresql" % "postgresql" % "9.3-1100-jdbc4",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.1.0",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "org.slf4j" % "slf4j-nop" % "1.6.4"
)