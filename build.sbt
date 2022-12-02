
val NetgymVersion = "1.0.0-SNAPSHOT"
val catsVersion = "2.9.0"
val catsEffectVersion = "3.4.1"
val fs2Version = "3.2.14"
val circeVersion = "0.14.1"
val scalatestVersion = "3.2.14"
val scalamockVersion = "5.2.0"
val catsEffectScalatestVersion = "1.4.0"

val typelevelStack = Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
  "co.fs2" %% "fs2-core" % fs2Version,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
)

val testDependencies = Seq(
  "org.scalatest" %% "scalatest" % scalatestVersion % Test,
  "org.scalatest" %% "scalatest-flatspec" % scalatestVersion % Test,
  "org.scalamock" %% "scalamock" % scalamockVersion % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % catsEffectScalatestVersion % Test,
)

scalaVersion := "2.13.8"

name := "netgym-scala"

version := "0.5-SNAPSHOT"

organization := "com.github.braginxv"

scmInfo := Some(ScmInfo(url("https://github.com/braginxv/netgym-scala"), "scm:git@github.com:braginxv/netgym-scala.git"))

description :=
  """High performance asynchronous network library for a client side of jvm-apps (including Android).
     It provides handling of a large number of parallel connections (TCP, UDP) using a single client instance."""
  .stripMargin

developers := List(
  Developer(
    id = "braginxv",
    name = "Vladimir Bragin",
    email = "uncloudedvm@gmail.com",
    url = url("https://github.com/braginxv")
  )
)

licenses := List(
  "MIT" -> new URL("https://opensource.org/licenses/MIT")
)

homepage := Some(url("https://github.com/braginxv/netgym-scala"))

pomIncludeRepository := { _ => false }

import xerial.sbt.Sonatype._
sonatypeProjectHosting := Some(GitHubHosting("braginxv", "netgym-scala",
  "Facilities for developing with Netgym network library in Scala", "uncloudedvm@gmail.com"))
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"

publishTo := sonatypePublishToBundle.value

publishMavenStyle := true

addCompilerPlugin("org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full)

resolvers += "Sonatype snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies += "com.github.braginxv" % "netgym" % NetgymVersion
libraryDependencies ++= typelevelStack
libraryDependencies ++= testDependencies
