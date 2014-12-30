package awsync.s3

import java.security.MessageDigest

import akka.util.ByteString
import awsync.utils.Hex

private[s3] object MD5 {

  /** @return a base 64 encoded md5 hash of the UTF-8 bytes of the string */
  def hash(data: String): String =
    hash(data.getBytes("UTF-8"))

  /** @return a base 64 encoded md5 hash of the bytes */
  def hash(data: Array[Byte]): String =
    base64Encode(MessageDigest.getInstance("MD5").digest(data))

  /** @return a base 64 encoded md5 hash of the bytes */
  def hash(data: ByteString): String = {
    val digest = MessageDigest.getInstance("MD5")
    val it = data.getByteBuffers().iterator()
    while(it.hasNext) {
      val current = it.next()
      digest.update(current)
    }
    base64Encode(digest.digest())
  }

  private def base64Encode(data: Array[Byte]): String =
    new sun.misc.BASE64Encoder().encode(data)

}