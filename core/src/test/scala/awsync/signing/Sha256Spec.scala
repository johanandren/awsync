package awsync.signing

class Sha256Spec extends AwsyncSpec {

  describe("The sha256 hash") {

    it("generates the expected hash string") {
      val result = Sha256.createHash("Action=ListUsers&Version=2010-05-08")
      result should be("b6359072c78d70ebee1e81adcbab4f01bf2c23245fa365ef83fe8f1f955085e2")
    }

  }

}
