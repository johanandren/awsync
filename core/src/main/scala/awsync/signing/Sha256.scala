package awsync.signing

import akka.util.ByteString

object Sha256 {
  import java.security.MessageDigest

  // not thread safe so must be created for each calculation
  private def sha = MessageDigest.getInstance("SHA-256")

  def createHash(s: String): String = createHash(s.getBytes)

  def createHash(bytes: ByteString): String = {
    // this sucks, but, no idea of how to create hash from byte string
    val array = new Array[Byte](bytes.length)
    bytes.copyToArray(array, 0, bytes.length)
    createHash(array)
  }


  def createHash(bytes: Array[Byte]): String =
    sha.digest(bytes).foldLeft("")((acc: String, byte: Byte) =>
      acc +
        Character.forDigit((byte & 0xf0) >> 4, 16) +
        Character.forDigit(byte & 0x0f, 16))

}