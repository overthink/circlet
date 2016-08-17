name := "circlet"
version := "0.2.0-SNAPSHOT"
organization := "com.markfeeney"
scalaVersion := "2.11.8"
scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-Xfatal-warnings",
  "-Xlint",
  "-Ywarn-unused-import"
)
libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-server" % "9.2.18.v20160721", // 9.3 needs Java 8, so stick with 9.2 for now
  "org.eclipse.jetty.websocket" % "websocket-server" % "9.2.18.v20160721",
  "commons-fileupload" % "commons-fileupload" % "1.3.1",
  "joda-time" % "joda-time" % "2.9.4",
  "org.joda" % "joda-convert" % "1.8.1",
  "org.scalatest" % "scalatest_2.11" % "2.2.6" % "test",
  "com.mashape.unirest" % "unirest-java" % "1.4.9" % "test",
  "org.eclipse.jetty.websocket" % "websocket-client" % "9.2.18.v20160721" % "test"
)

// Stuff related to publishing to sonatype
// Reference: http://www.loftinspace.com.au/blog/publishing-scala-libraries-to-sonatype.html
// Also useful: http://central.sonatype.org/pages/releasing-the-deployment.html
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
