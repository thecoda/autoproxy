package sandbox

import org.scalatest.{Matchers, FunSpec}
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.reflect.runtime.universe._
import org.scalatest.matchers.{BePropertyMatchResult, BePropertyMatcher}

trait StdSpec extends FunSpec with GeneratorDrivenPropertyChecks with Matchers {
  def anInstanceOf[T: TypeTag] = {
    val m = runtimeMirror(getClass.getClassLoader)
    val clazz = m.runtimeClass(typeOf[T].typeSymbol.asClass)
    new BePropertyMatcher[AnyRef] { def apply(left: AnyRef) =
      BePropertyMatchResult(clazz.isAssignableFrom(left.getClass), "an instance of " + clazz.getName)
    }
  }


  def failedRequirement(msg: String) = {
    val fullMsg = "requirement failed: " + msg
    new BePropertyMatcher[Throwable] { def apply(thrown: Throwable) =
      BePropertyMatchResult(thrown.getMessage == fullMsg, "a failed requirement of '" + msg + "'")
    }
  }
  
  def captureOut[T](thunk: => T): String = {
    val mockOut = new java.io.ByteArrayOutputStream()
    Console.withOut(mockOut)(thunk)
    mockOut.toString()  
  }
}
