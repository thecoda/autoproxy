import sbt._
import Keys._
//import com.github.siasia.WebPlugin


object BuildSettings {

  def mkValStr(name: String, value: String) =
    "  val " + name + " = \"" + value + "\""

  def optEnv(key: String): Option[String] =
    Option(System.getenv get key)

  def env(key: String, default: String = ""): String =
    optEnv(key) getOrElse default

  def prop(key: String, default: String = ""): String =
    System.getProperties.getProperty(key, default)

  val sbtPromptSettings = Seq (
    shellPrompt  <<= (thisProjectRef, version) { (projref, ver) =>
      _ => projref.project +":"+ Git.branch +":"+ ver + ">"
    }
  )
}

object Git {
  object devnull extends ProcessLogger {
    def info (s: => String) {}
    def error (s: => String) {}
    def buffer[T] (f: => T): T = f
  }

  def silentExec(cmd: String) = cmd lines_! devnull

  def branch =
    silentExec("git status -sb").headOption getOrElse "-" stripPrefix "## "

  def hash =
    silentExec("git log --pretty=format:%H -n1").headOption getOrElse "-"
}
