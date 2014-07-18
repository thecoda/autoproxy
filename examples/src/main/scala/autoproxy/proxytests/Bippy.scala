package autoproxy.proxytests

import autoproxy.{delegating, proxy}

object inner {
  trait SomeTrait
}

trait Bippy {
  def bippy(i : Int) : String
}

object SimpleBippy extends Bippy {
  def bippy(i: Int) = i.toString
}

object DoublingBippy extends Bippy {
  def bippy(i: Int) = (i*2).toString
}

@delegating class BippyValParamWrapper(@proxy val dg : Bippy) extends autoproxy.proxytests.inner.SomeTrait {
  def one(s: String) = s
  def two(i: Int) = i
  def three[T](x: T) = x
  def dgtoo = dg
}

@delegating class BippyVarParamWrapper(@proxy var dg : Bippy)

@delegating class BippyValWrapper {
  @proxy val dg: Bippy = SimpleBippy
}

@delegating class BippyVarWrapper {
  @proxy var dg: Bippy = SimpleBippy
}

@delegating object SingletonBippyWithProxyVar {
  @proxy var dg: Bippy = SimpleBippy
}

@delegating object SmarterProps {
  @proxy private[this] object props {
    var x: Int = 0
    var y: String = ""
  }
  def y_=(txt: String): Unit = { props.y = txt + " banana"}
}

//@delegating class Clash {
//  @proxy val one: Bippy = SimpleBippy
//  @proxy val two: Bippy = DoublingBippy
//}

//class Jeopardy[T] {
//  private var it: T = _
//  def doit(): T = it
//}

//@delegating class DoubleJeopardy {
//  @proxy val one = new Jeopardy[Int]
//  @proxy val two = new Jeopardy[String]
//}
