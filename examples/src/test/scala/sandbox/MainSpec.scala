package sandbox

import org.scalacheck.Gen

class MainSpec extends StdSpec {
  describe("Main") {

    it("should print the standard welcome") {
      val out = captureOut {
        //Main.main(Array.empty)
        println("hello world")
      }
      val expected = "hello world" + System.lineSeparator()
      assert(out === expected)
    }
  }
}
