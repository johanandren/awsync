package awsync.signing

import akka.http.model.HttpEntity.Strict
import akka.http.model.{HttpEntity, HttpHeader, HttpRequest, Uri}

object CanonicalRequest {

  def create(request: HttpRequest, body: HttpEntity): CanonicalRequest = {
    val method = request.method.name
    val canonicalUri = encodePath(request.getUri().path())
    val canonicalQuery = encodeParameters(request.uri.query)
    val canonicalHeaders = encodeHeaders(request.headers)
    val signedHeaders = encodeSignedHeaders(request.headers)
    val hash = bodyHash(body)
    new CanonicalRequest(s"$method\n$canonicalUri\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$hash", hash)
  }

  def bodyHash(body: HttpEntity): String = body match {
    case Strict(_, bytes) => Sha256.createHash(bytes)
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
}

class CanonicalRequest(val value: String, val hash: String)