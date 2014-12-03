package awsync.authentication

import java.util.Date

import awsync.http.AwsHeaders
import awsync.utils.DateUtils
import awsync.{Credentials, Service, Region}
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpRequest

/**
 * AWS signature v4
 */
object Authentication {

  /**
   * Signs a request. Also, always adds the x-amz-date header with current time and a hash
   * of the request body to the request.
   *
   * @return A request with the Authorization header set
   */
  def signWithHeader(request: HttpRequest, region: Region, service: Service, credentials: Credentials): HttpRequest =
    signWithHeader(request, region, service, credentials, new Date)

  def signWithHeader(request: HttpRequest, region: Region, service: Service, credentials: Credentials, date: Date): HttpRequest = {

    // TODO how do we know that it is chunked?
    val bodyHash = Sha256.bodyHash(request.entity, chunked = false)

    val requestWithDate = request.withHeaders(
      request.headers ++ Seq(AwsHeaders.AmzDate(date), RawHeader("x-amz-content-sha256", bodyHash))
    )


    val (canonical, signedHeaders)= CanonicalRequest.canonicalRequest(requestWithDate, bodyHash)
    val canonicalHash = Sha256.createHash(canonical)
    val dateTimeString = DateUtils.toIso8601DateTimeFormat(date)
    val stringToSign = StringToSign.create(dateTimeString, region, service, canonicalHash)
    val signature = Signature.create(credentials.secret, date, region, service, stringToSign)

    val algorithm = "AWS4-HMAC-SHA256"
    val dateString = DateUtils.toIso8601DateFormat(date)
    val credential = s"${credentials.key.key}/$dateString/${region.name}/${service.name}/aws4_request"
    val authHeader = s"$algorithm Credential=$credential, SignedHeaders=$signedHeaders, Signature=$signature"

    requestWithDate.withHeaders(requestWithDate.headers :+ RawHeader("Authorization", authHeader))
  }

}