val currentScalaVersion = "2.11.11"
val akkaHttpVersion = "10.0.10"

name in ThisBuild := "sbt-digest-reverse-router"
organization in ThisBuild := "org.mdedetrich"
scalaVersion in ThisBuild := currentScalaVersion
crossScalaVersions in ThisBuild := Seq("2.10.6", currentScalaVersion, "2.12.3")
version in ThisBuild := "0.1.0"

scalafmtVersion in ThisBuild := "1.1.0"

libraryDependencies += "com.typesafe" % "config" % "1.3.2"

val flagsFor10 = Seq(
  "-Xlint",
  "-Yclosure-elim",
  "-Ydead-code"
)

val flagsFor11 = Seq(
  "-Xlint:_",
  "-Yconst-opt",
  "-Ywarn-infer-any",
  "-Yclosure-elim",
  "-Ydead-code"
)

val flagsFor12 = Seq(
  "-Xlint:_",
  "-Ywarn-infer-any",
  "-opt:l:project"
)

scalacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 12 =>
      flagsFor12
    case Some((2, n)) if n == 11 =>
      flagsFor11
    case Some((2, n)) if n == 10 =>
      flagsFor10
  }
}

scalacOptions in ThisBuild += {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n >= 12 =>
      "-target:jvm-1.8"
    case _ =>
      "-target:jvm-1.6"
  }
}

homepage := Some(url("https://github.com/mdedetrich/sbt-digest-reverse-router"))
scmInfo := Some(
  ScmInfo(url("https://github.com/mdedetrich/sbt-digest-reverse-router"),
          "git@github.com:mdedetrich/sbt-digest-reverse-router.git"))
developers := List(
  Developer("mdedetrich",
            "Matthew de Detrich",
            "mdedetrich@gmail.com",
            url("https://github.com/mdedetrich"))
)

licenses += ("Apache 2", url("https://opensource.org/licenses/Apache-2.0"))
pomIncludeRepository := (_ => false)
publishMavenStyle := true
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
publishArtifact in Test := false
pomIncludeRepository := (_ => false)
