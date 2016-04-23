name := "circlet"
version := "0.1.0-SNAPSHOT"
organization := "com.markfeeney"
scalaVersion := "2.11.8"
scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint"
)
libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-server" % "9.2.16.v20160414", // 9.3 needs Java 8, so stick with 9.2 for now
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "com.mashape.unirest" % "unirest-java" % "1.4.9" % "test"
)
