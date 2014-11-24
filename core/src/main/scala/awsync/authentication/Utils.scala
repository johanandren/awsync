package awsync.authentication

private[authentication] object Utils {

  def hexEncode(bytes: Array[Byte]): String =
    bytes.foldLeft(StringBuilder.newBuilder) { (acc, byte) =>
      acc += Character.forDigit((byte & 0xf0) >> 4, 16)
      acc += Character.forDigit(byte & 0x0f, 16)
      acc
    }.result()


  private val charIsOk: Set[Char] = {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    chars.toSet ++ chars.toLowerCase.toSet ++ "_-~.0123456789".toSet
  }

  def uriEncode(str: String, encodeSlash: Boolean = true): String =
    str.foldLeft(StringBuilder.newBuilder) { (acc, char) =>
      if (charIsOk(char) || (char == '/' && !encodeSlash)) acc += char
      else acc ++= toHexUtf8(char)
    }.toString()

  def toHexUtf8(ch: Char): String = "%" + Integer.toHexString(ch.toInt)

}