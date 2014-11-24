package awsync.utils

import scala.collection.immutable.Seq
import awsync.AbstractSpec

import scala.util.{Failure, Success, Try}

class FunctionalSpec extends AbstractSpec {

  describe("sequencing a Seq of Try") {

    it("turns successes into a successful seq") {
      val result = Functional.sequence(Seq(Try(1), Try(2)))
      result should be (Success(Seq(1,2)))
    }

    it("turns a seq with fail into the first fail") {
      val result = Functional.sequence(Seq(Try(1), Failure(new RuntimeException("1")), Failure(new RuntimeException("2"))))
      result.isFailure should be (true)
      result.failed.get.getMessage should be ("1")
    }

  }
}
