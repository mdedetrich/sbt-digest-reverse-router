val currentScalaVersion = "2.12.12"
val akkaHttpVersion     = "10.2.2"

name in ThisBuild := "sbt-digest-reverse-router"
organization in ThisBuild := "org.mdedetrich"
scalaVersion in ThisBuild := currentScalaVersion
crossScalaVersions in ThisBuild := Seq(currentScalaVersion, "2.13.4")
version in ThisBuild := "0.2.0"

libraryDependencies += "com.typesafe" % "config" % "1.4.1"

val flagsFor12 = Seq(
  "-Xlint:_",
  "-Ywarn-infer-any",
  "-opt:l:project"
)

val flagsFor13 = Seq(
  "-Xlint:_",
  "-opt-inline-from:<sources>"
)

scalacOptions in ThisBuild ++= {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, n)) if n == 13 =>
      flagsFor13
    case Some((2, n)) if n == 12 =>
      flagsFor12
  }
}

homepage := Some(url("https://github.com/mdedetrich/sbt-digest-reverse-router"))
scmInfo := Some(
  ScmInfo(
    url("https://github.com/mdedetrich/sbt-digest-reverse-router"),
    "git@github.com:mdedetrich/sbt-digest-reverse-router.git"
  )
)
developers := List(
  Developer("mdedetrich", "Matthew de Detrich", "mdedetrich@gmail.com", url("https://github.com/mdedetrich"))
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
