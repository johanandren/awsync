package awsync.s3

import akka.util.ByteString

class MD5Spec extends AbstractSpec {

  val text = "The quick brown fox jumps over the lazy dog"
  val bytes = text.getBytes("UTF-8")
  // base 64 encoded!
  val md5sum = "nhB9nTcrtoJr2B01QqQZ1g=="

  describe("The md5 operations") {

    it("generates a correct md5 checksum for a byte array") {
      MD5.hash(bytes) should be (md5sum)
    }

    it("generates a correct md5 checksum for a string") {
      MD5.hash(text) should be (md5sum)
    }

    it("generates a correct md5 checksum for a byte string") {
      val byteStringWithChunks = ByteString(bytes.take(2)) ++ bytes.drop(2)
      MD5.hash(byteStringWithChunks) should be (md5sum)
    }
  }

}
