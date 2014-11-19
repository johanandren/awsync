package awsync.signing

import collection.immutable.Seq
import akka.http.model.HttpEntity.Strict
import akka.http.model.{HttpEntity, HttpHeader, HttpRequest, Uri}
import akka.util.ByteString

object CanonicalRequest {

  def create(request: HttpRequest): CanonicalRequest = {
    val method = request.method.name
    val canonicalUri = encodePath(request.getUri().path())
    val canonicalQuery = encodeParameters(request.uri.query)
    val canonicalHeaders = encodeHeaders(request.headers)
    val signedHeaders = encodeSignedHeaders(request.headers)
    val hash = bodyHash(request.entity)
    new CanonicalRequest(
      s"$method\n$canonicalUri\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$hash",
      hash,
      signedHeaders
    )
  }

  def bodyHash(body: HttpEntity): String = body match {
    case Strict(_, bytes) => createHash(bytes)
    case x => throw new RuntimeException(s"Unsupported body type $x")
  }

  def encodePath(path: String): String =
    if (path.length == 0) "/"
    else path.split('/').map(uriEncode).mkString("/")

  def encodeParameters(query: Uri.Query): String =
    query.map(t => uriEncode(t._1) + "=" + uriEncode(t._2))
      .sorted
      .mkString("&")

  def encodeHeaders(headers: Seq[HttpHeader]): String =
    headers.map { header =>
      val name = header.lowercaseName
      val value = header.value.replaceFirst("""^\s+""", " ").replaceFirst("""\s+$""", " ")
      s"$name:$value\n"
    }.sorted
      .mkString("")

  def encodeSignedHeaders(headers: Seq[HttpHeader]): String =
    headers.map(_.lowercaseName).sorted.mkString(";")


  private val okChars: Set[Char] = "ABCDEFGIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.~".toSet
  private def uriEncode(str: String): String = str.flatMap { c =>
    if (okChars(c)) c.toString
    else percentEncode(c)
  }

  private def percentEncode(char: Char): String = {
    "%" + Integer.toHexString(char.toInt)
  }

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

  def createHash(bytes: Array[Byte]): String = Utils.hexEncode(sha.digest(bytes))

}

class CanonicalRequest(val value: String, val hash: String, val signedHeaders: String)