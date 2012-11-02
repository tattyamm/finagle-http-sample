import sbt._
import sbt.Keys._
import scala._

object FinaglesampleBuild extends Build {

  lazy val finaglesample = Project(
    id = "finagle-sample",
    base = file("."),
    settings = Project.defaultSettings ++ Seq(
      name := "finagle-sample",
      organization := "jp.tattyamm.finagle.sample",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.9.2",
      // add other settings here
      resolvers ++= Seq(
        "Twitter Repository" at "http://maven.twttr.com/"
      ),
      libraryDependencies ++= Seq(
        "com.twitter" % "finagle-core" % "5.3.20",
        "com.twitter" % "finagle-http" % "5.3.20"
      )
    )
  )
}
