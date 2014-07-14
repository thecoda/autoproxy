package sandbox.proxytests

object RunEmAll extends App {

  println("autoproxy.debug=" + sys.props.getOrElse(s"autoproxy.debug", "false"))
  println("autoproxy.trace=" + sys.props.getOrElse(s"autoproxy.trace", "false"))

  val wrap1 = new BippyValParamWrapper(SimpleBippy)
  println(wrap1.bippy(42))

  assert(wrap1.isInstanceOf[inner.SomeTrait])
  
  val wrap2 = new BippyVarParamWrapper(SimpleBippy)
  println(wrap2.bippy(42))

  wrap2.dg = DoublingBippy
  println(wrap2.bippy(42))

  val wrap3 = new BippyValWrapper
  println(wrap3.bippy(42))

  val wrap4 = new BippyVarWrapper
  println(wrap4.bippy(42))
  wrap4.dg = DoublingBippy
  println(wrap4.bippy(42))

  println(SingletonBippyWithProxyVar.bippy(42))
  SingletonBippyWithProxyVar.dg = DoublingBippy
  println(SingletonBippyWithProxyVar.bippy(42))

  SmarterProps.x = 42
  SmarterProps.y = "forty-two"
  println(s"${SmarterProps.x} ${SmarterProps.y}")

}