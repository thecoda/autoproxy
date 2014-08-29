package autoproxy

import org.scalatest.FunSuite

class ParamDelegationSpec extends FunSuite {



  test("can delegate to a raw parameter") {
    @delegating class RawParamWrapper(@proxy pivot : Bippy)
    val wrapper = new RawParamWrapper(SimpleBippy)
    assert(wrapper.bippy(42) === "42")
  }

  test("can delegate to a val parameter") {
    @delegating class ValParamWrapper(@proxy val pivot : Bippy)
    val wrapper = new ValParamWrapper(SimpleBippy)
    assert(wrapper.bippy(42) === "42")
  }

  test("can delegate to a var parameter") {
    @delegating class VarParamWrapper(@proxy var pivot : Bippy)
    val wrapper = new VarParamWrapper(SimpleBippy)
    assert(wrapper.bippy(42) === "42")
    wrapper.pivot = DoublingBippy
    assert(wrapper.bippy(42) === "84")
  }

  test("can delegate to a val member") {
    @delegating class ValMemberWrapper { @proxy val pivot : Bippy = SimpleBippy }
    val wrapper = new ValMemberWrapper
    assert(wrapper.bippy(42) === "42")
  }

  test("can delegate to a var member") {
    @delegating class VarMemberWrapper { @proxy var pivot : Bippy = SimpleBippy }
    val wrapper = new VarMemberWrapper
    assert(wrapper.bippy(42) === "42")
    wrapper.pivot = DoublingBippy
    assert(wrapper.bippy(42) === "84")
  }

  test("can delegate to a def member") {
    @delegating class DefMemberWrapper { @proxy def pivot : Bippy = SimpleBippy }
    val wrapper = new DefMemberWrapper
    assert(wrapper.bippy(42) === "42")
  }

  test("can delegate to a val member in a singleton") {
    @delegating object ValMemberWrapperObj { @proxy val pivot : Bippy = SimpleBippy }
    assert(ValMemberWrapperObj.bippy(42) === "42")
  }

  test("can delegate to a var member in a singleton") {
    @delegating object VarMemberWrapperObj { @proxy var pivot : Bippy = SimpleBippy }
    assert(VarMemberWrapperObj.bippy(42) === "42")
    VarMemberWrapperObj.pivot = DoublingBippy
    assert(VarMemberWrapperObj.bippy(42) === "84")
  }

  test("can delegate to a def member in a singleton") {
    @delegating object DefMemberWrapperObj { @proxy def pivot : Bippy = SimpleBippy }
    assert(DefMemberWrapperObj.bippy(42) === "42")
  }

  test("can delegate to an embedded singleton with selective override") {
    @delegating object SmarterProps {
      @proxy private[this] object props {
        var x: Int = 0
        var y: String = ""
      }
      def y_=(txt: String): Unit = { props.y = txt + " bananas"}
    }
    assert(SmarterProps.x === 0)
    assert(SmarterProps.y === "")

    SmarterProps.x = 42
    SmarterProps.y = "forty-two"

    assert(SmarterProps.x === 42)
    assert(SmarterProps.y === "forty-two bananas")

  }

}
