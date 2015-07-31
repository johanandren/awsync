package awsync.authentication

import akka.http.scaladsl.model.{HttpHeader, Uri, HttpRequest}
import awsync.utils.Hex
import collection.immutable.Seq

private[authentication] object CanonicalRequest {

  type CanonicalRequest = String
  type SignedHeaders = String

  def canonicalRequest(request: HttpRequest, bodyHash: String): (String, String) = {
    val method = request.method.name
    val canonicalUri = encodePath(request.uri.path)
    val canonicalQuery = encodeParameters(request.uri.query)
    val canonicalHeaders = encodeHeaders(request.headers)
    val signedHeaders = encodeSignedHeaders(request.headers)

    (s"$method\n$canonicalUri\n$canonicalQuery\n$canonicalHeaders\n$signedHeaders\n$bodyHash", signedHeaders)
  }


  def encodePath(path: Uri.Path): String =
    if (path.length == 0) "/"
    else path.toString

  def encodeParameters(query: Uri.Query): String =
    query.map(t => Hex.uriEncode(t._1) + "=" + Hex.uriEncode(t._2))
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
  
}