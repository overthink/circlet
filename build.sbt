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
  "commons-fileupload" % "commons-fileupload" % "1.3.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "com.mashape.unirest" % "unirest-java" % "1.4.9" % "test",
  "org.antlr" % "antlr4-runtime" % "4.5.3" // route parser needs this; later move to own project
)

// Stuff related to publishing to sonatype
publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
licenses := Seq("MIT" -> url("https://opensource.org/licenses/MIT"))
homepage := Some(url("https://github.com/overthink/circlet"))
pomExtra := (
  <scm>
    <url>git@github.com:overthink/circlet.git</url>
    <connection>scm:git:git@github.com:overthink/circlet.git</connection>
  </scm>
  <developers>
    <developer>
      <id>overthink</id>
      <name>Mark Feeney</name>
      <url>http://proofbyexample.com</url>
    </developer>
  </developers>)

antlr4Settings
antlr4PackageName in Antlr4 := Some("com.markfeeney.poise.parser")
