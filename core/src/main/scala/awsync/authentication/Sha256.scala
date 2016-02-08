package awsync.authentication

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.http.scaladsl.model.HttpEntity
import akka.util.ByteString
import awsync.utils.Hex

private[awsync] object Sha256 {

  // not thread safe so must be created for each calculation
  private def sha256 = MessageDigest.getInstance("SHA-256")

  def createHash(bytes: Array[Byte]): String = Hex.hexEncode(sha256.digest(bytes))

  def createHash(s: String): String = createHash(s.getBytes)

  def createHash(bytes: ByteString): String = {
    // this sucks, but, no idea of how to create hash from byte string
    val array = new Array[Byte](bytes.length)
    bytes.copyToArray(array, 0, bytes.length)
    createHash(array)
  }

  def bodyHash(body: HttpEntity): String = body match {
    case x if x.isChunked => "STREAMING-AWS4-HMAC-SHA256-PAYLOAD"
    case HttpEntity.Empty => createHash("")
    case HttpEntity.Strict(_, bytes) => createHash(bytes)
    case x => throw new RuntimeException(s"Unsupported body type $x")
  }


  private val algorithm = "HmacSHA256"

  def hmacSHA256(data: String, key: Array[Byte]): Array[Byte] = {
    val mac = Mac.getInstance(algorithm)
    mac.init(new SecretKeySpec(key, algorithm))
    mac.doFinal(data.getBytes("UTF8"))
  }

}
