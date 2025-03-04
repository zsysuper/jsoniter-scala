import sbt._
import scala.sys.process._

lazy val oldVersion = "git describe --abbrev=0".!!.trim.replaceAll("^v", "")

lazy val commonSettings = Seq(
  organization := "com.github.plokhotnyuk.jsoniter-scala",
  organizationHomepage := Some(url("https://github.com/plokhotnyuk")),
  homepage := Some(url("https://github.com/plokhotnyuk/jsoniter-scala")),
  licenses := Seq(("MIT License", url("https://opensource.org/licenses/mit-license.html"))),
  startYear := Some(2017),
  developers := List(
    Developer(
      id = "plokhotnyuk",
      name = "Andriy Plokhotnyuk",
      email = "plokhotnyuk@gmail.com",
      url = url("https://twitter.com/aplokhotnyuk")
    )
  ),
  resolvers += "Sonatype OSS Staging" at "https://oss.sonatype.org/content/repositories/staging",
  scalaVersion := "2.13.0",
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-target:jvm-1.8",
    "-feature",
    "-unchecked",
    "-Ywarn-dead-code",
    "-Xlint",
    "-Xmacro-settings:" + sys.props.getOrElse("macro.settings", "none")
  ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, x)) if x >= 12 => Seq(
      "-opt:l:method"
    )
    case Some((2, x)) if x == 11 => Seq(
      "-Ybackend:GenBCode",
      "-Ydelambdafy:inline"
    )
    case _ => Seq()
  }),
  testOptions in Test += Tests.Argument("-oDF")
)

lazy val noPublishSettings = Seq(
  skip in publish := true,
  publishTo := sonatypePublishToBundle.value,
  mimaPreviousArtifacts := Set()  
)

lazy val publishSettings = Seq(
  publishTo := sonatypePublishToBundle.value,
  sonatypeProfileName := "com.github.plokhotnyuk",
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/plokhotnyuk/jsoniter-scala"),
      "scm:git@github.com:plokhotnyuk/jsoniter-scala.git"
    )
  ),
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  mimaCheckDirection := {
    def isPatch: Boolean = {
      val Array(newMajor, newMinor, _) = version.value.split('.')
      val Array(oldMajor, oldMinor, _) = oldVersion.split('.')
      newMajor == oldMajor && newMinor == oldMinor
    }

    if (isPatch) "both" else "backward"
  },
  mimaPreviousArtifacts := {
    def isCheckingRequired: Boolean = {
      val Array(newMajor, newMinor, _) = version.value.split('.')
      val Array(oldMajor, oldMinor, _) = oldVersion.split('.')
      newMajor == oldMajor && (newMajor != "0" || newMinor == oldMinor)
    }

    if (isCheckingRequired) Set(organization.value %% moduleName.value % oldVersion)
    else Set()
  }
)

lazy val `jsoniter-scala` = project.in(file("."))
  .settings(commonSettings)
  .settings(noPublishSettings)
  .aggregate(`jsoniter-scala-core`, `jsoniter-scala-macros`, `jsoniter-scala-benchmark`)

lazy val `jsoniter-scala-core` = project
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("2.13.0", "2.12.10", "2.11.12"),
    libraryDependencies ++= Seq(
      "com.github.plokhotnyuk.expression-evaluator" %% "expression-evaluator" % "0.1.1" % Provided,
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2" % Test,
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test
    )
  )

lazy val `jsoniter-scala-macros` = project
  .dependsOn(`jsoniter-scala-core`)
  .settings(commonSettings)
  .settings(publishSettings)
  .settings(
    crossScalaVersions := Seq("2.13.0", "2.12.10", "2.11.12"),
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scalacheck" %% "scalacheck" % "1.14.0" % Test,
      "org.scalatest" %% "scalatest" % "3.0.8" % Test
    )
  )

lazy val `jsoniter-scala-benchmark` = project
  .enablePlugins(JmhPlugin)
  .dependsOn(`jsoniter-scala-macros`)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    resolvers += Resolver.bintrayRepo("reug", "maven"),
    crossScalaVersions := Seq("2.13.0", "2.12.10"),
    libraryDependencies ++= Seq(
      "reug" %% "scalikejackson" % "0.5.6",
      "io.bullet" %% "borer-derivation" % "0.11.1",
      "pl.iterators" %% "kebs-spray-json" % "1.6.3",
      "io.spray" %%  "spray-json" % "1.3.5",
      "com.avsystem.commons" %% "commons-core" % "2.0.0-M2",
      "com.lihaoyi" %% "upickle" % "0.7.5",
      "com.dslplatform" %% "dsl-json-scala" % "1.9.3",
      "com.jsoniter" % "jsoniter" % "0.9.23",
      "org.javassist" % "javassist" % "3.25.0-GA", // required for Jsoniter Java
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.10.0.pr2",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % "2.10.0.pr2",
      "com.fasterxml.jackson.module" % "jackson-module-afterburner" % "2.10.0.pr2",
      "io.circe" %% "circe-generic" % "0.12.1",
      "io.circe" %% "circe-generic-extras" % "0.12.2",
      "io.circe" %% "circe-parser" % "0.12.1",
      "com.typesafe.play" %% "play-json" % "2.8.0-M6",
      "org.julienrf" %% "play-json-derived-codecs" % "6.0.0",
      "ai.x" %% "play-json-extensions" % "0.40.2",
      "pl.project13.scala" % "sbt-jmh-extras" % "0.3.7",
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.2",
      "org.scalatest" %% "scalatest" % "3.0.8" % Test
    )
  )
