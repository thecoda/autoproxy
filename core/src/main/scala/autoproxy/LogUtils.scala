package autoproxy

/**
 * Shamelessly liberated from the async macro
 */
object LogUtils {
//  println(sys.props.mkString("\n"))
  println("autoproxy.debug=" + verbose)
  println("autoproxy.trace=" + trace)

  private def enabled(level: String) = sys.props.getOrElse(s"autoproxy.$level", "false").equalsIgnoreCase("true")

  private def verbose = enabled("debug")
  private def trace   = enabled("trace")

  private[autoproxy] def vprintln(s: => Any): Unit = if (verbose) println(s"[autoproxy] $s")

  private[autoproxy] def trace(s: => Any): Unit = if (trace) println(s"[autoproxy] $s")
}
