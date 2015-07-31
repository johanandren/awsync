package awsync.s3

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import awsync.{Credentials, Service, Region}
import awsync.authentication.Authentication

private[s3] object RequestUtils {

  private val service = Service("s3")
  
  def signedRequest(method: HttpMethod, uri: Uri, body: RequestEntity, region: Region, credentials: Credentials): HttpRequest =
    signedRequest(HttpRequest(method, uri, List(headers.Host(uri.authority.host.address)), body), region, credentials)


  def signedRequest(method: HttpMethod, uri: Uri, region: Region, credentials: Credentials): HttpRequest =
    signedRequest(HttpRequest(method, uri, List(headers.Host(uri.authority.host.address))), region, credentials)

  def signedRequest(request: HttpRequest, region: Region, credentials: Credentials) = {
    val withMd5 = addBodyMd5(request)
    Authentication.signWithHeader(withMd5, region, service, credentials)
  }


  def addBodyMd5(request: HttpRequest): HttpRequest = {
    val md5 = request.entity match {
      case HttpEntity.Empty => Some(MD5.hash(""))
      case HttpEntity.Strict(_, bytes) => Some(MD5.hash(bytes))
      case _ => None
    }

    md5.fold(request)(hash => request.withHeaders(request.headers :+ RawHeader("Content-MD5", hash)))
  }
}
