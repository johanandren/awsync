package awsync.s3

import java.security.MessageDigest

import awsync.utils.Hex

private[s3] object MD5 {

  def hash(data: String): String = hash(data.getBytes)

  def hash(data: Array[Byte]): String =
    Hex.hexEncode(MessageDigest.getInstance("MD5").digest(data))

}