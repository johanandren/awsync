package awsync.s3

import awsync.{Credentials, Service, Region}
import awsync.authentication.Authentication
import spray.http.HttpHeaders.RawHeader
import spray.http._

private[s3] object RequestUtils {

  private val service = Service("s3")
  
  def signedRequest(method: HttpMethod, uri: Uri, body: HttpEntity, region: Region, credentials: Credentials): HttpRequest =
    signedRequest(HttpRequest(method, uri, List(HttpHeaders.Host(uri.authority.host.address)), body), region, credentials)


  def signedRequest(method: HttpMethod, uri: Uri, region: Region, credentials: Credentials): HttpRequest =
    signedRequest(HttpRequest(method, uri, List(HttpHeaders.Host(uri.authority.host.address))), region, credentials)

  def signedRequest(request: HttpRequest, region: Region, credentials: Credentials) = {
    val withMd5 = addBodyMd5(request)
    Authentication.signWithHeader(withMd5, region, service, credentials)
  }


  def addBodyMd5(request: HttpRequest): HttpRequest = {
    val md5 =
      if (request.entity.data.isEmpty) Some(MD5.hash(""))
      else if (!request.entity.data.hasFileBytes) Some(MD5.hash(request.entity.data.toByteString))
      else None

    md5.fold(request)(hash => request.withHeaders(request.headers :+ RawHeader("Content-MD5", hash)))
  }
}
