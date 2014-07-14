import sbt._
import scala._

object Resolvers {
  //lazy val localm2 = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
  lazy val mvncentral = "Maven Central" at "http://repo1.maven.org/maven2/"
  lazy val typesafe = Classpaths.typesafeReleases
  lazy val ossreleases = "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/"
  lazy val osssnapshots = "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

  lazy val all = Seq(mvncentral, typesafe, ossreleases, osssnapshots)
}

object Dependencies {
  val core = Core.all
  val test = Test.all

  

  object Core {
    //lazy val scalaz     = "org.scalaz"                 %% "scalaz-core"                   % "7.1.0-M4"
    //lazy val shapeless   = "com.chuusai"                %% "shapeless"                     % "2.0.0-SNAPSHOT"
    lazy val paradise      = "org.scalamacros"            %  "paradise_2.11.1"               % "2.1.0-M1"
    lazy val slf4j         = "org.slf4j"                  %  "slf4j-api"                     % "1.7.5"
    lazy val all = Seq(paradise, slf4j) //shapeless, scalaz
  }

  object Test {
    lazy val logback    = "ch.qos.logback"             %  "logback-classic"               % "1.1.0"   % "test"
    lazy val groovy     = "org.codehaus.groovy"        %  "groovy-all"                    % "1.7.6"   % "test"
    lazy val janino     = "org.codehaus.janino"        %  "janino"                        % "2.6.1"  % "test"
//    lazy val specs2     = "org.specs2"                 %% "specs2"                        % "2.0-RC1" % "test"
    lazy val scalatest  = "org.scalatest"              %% "scalatest"                     % "2.2.0"   % "test"
    lazy val scalacheck = "org.scalacheck"             %% "scalacheck"                    % "1.11.4"  % "test"
    lazy val mockito    = "org.mockito"                %  "mockito-core"                  % "1.9.0"   % "test"
    lazy val hamcrest   = "org.hamcrest"               %  "hamcrest-core"                 % "1.3"     % "test"
    lazy val junit      = "junit"                      %  "junit"                         % "4.7"     % "test" //for xml output

    lazy val all = Seq(logback, groovy, janino, scalatest, scalacheck, mockito, hamcrest)

  }

}
