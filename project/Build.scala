import sbt._
//import com.github.siasia._
import Keys._
import sbt.Package._
import sbtrelease._
import sbtrelease.ReleasePlugin._
import net.virtualvoid.sbt.graph.Plugin.graphSettings
import sbtrelease.ReleasePlugin.ReleaseKeys._
import sbtrelease.ReleaseStateTransformations._
import aether.Aether._


object TopLevelBuild extends Build {
  import BuildSettings._

  lazy val buildSettings = Seq(
    organization := "net.thecoda.autoproxy",
    scalaVersion := "2.11.1",
    scalacOptions := Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-Xlint",
      "-encoding", "utf8",
//      "-Yrangepos",
      "-Yno-adapted-args",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",   
      "-Ywarn-value-discard",
      "-Dautoproxy.debug=true",
      "-Dautoproxy.trace=true")
//    scalacOptions in (console) += "-Yrangepos"
    //scalacOptions in (Compile, compile) += "-P:wartremover:traverser:org.brianmckenna.wartremover.warts.Unsafe"
  )

  lazy val root = Project(id = "root", base = file("."))
    .aggregate(core, examples)
    .settings(commonSettings : _*)

  lazy val core = Project(id = "core", base = file("core"))
    .configs(IntegrationTest)
    .settings(commonSettings : _*)

  lazy val examples = Project(id = "examples", base = file("examples"))
    .dependsOn(core)
    .configs(IntegrationTest)
    .settings(commonSettings : _*)

  lazy val commonSettings =
    sbtPromptSettings ++
    buildSettings ++
    graphSettings ++
    releaseSettings ++
    Defaults.itSettings ++
    miscSettings
    //addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0-M1")
    //addCompilerPlugin("org.brianmckenna" %% "wartremover" % "0.7")
    //publishSettings ++
    //releaseSettings ++
    //packagingSettings ++
    //aetherSettings ++
    //aetherPublishSettings ++

  lazy val miscSettings = Seq(
    resolvers ++= Resolvers.all,
    ivyXML := ivyDeps,
    autoCompilerPlugins := true,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M1" cross CrossVersion.full),
    libraryDependencies ++= Dependencies.core,
    libraryDependencies ++= Dependencies.test,
    libraryDependencies <++= (scalaVersion)(sv =>
      Seq(
        "org.scala-lang" % "scala-reflect" % sv,
        "org.scala-lang" % "scala-compiler" % sv
      )
    )
  )

  val ivyDeps = {
    <dependencies>
      <!-- commons logging is evil. It does bad, bad things to the classpath and must die. We use slf4j instead -->
      <exclude module="commons-logging"/>
    </dependencies>
  }
}
