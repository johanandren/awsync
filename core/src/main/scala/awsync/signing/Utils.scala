package awsync.signing

object Utils {

  def hexEncode(bytes: Array[Byte]): String =
    bytes.foldLeft(StringBuilder.newBuilder) { (acc, byte) =>
      acc += Character.forDigit((byte & 0xf0) >> 4, 16)
      acc += Character.forDigit(byte & 0x0f, 16)
      acc
    }.toString

}
