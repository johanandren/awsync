package awsync.signing

import java.util.Date

import akka.http.model.headers.RawHeader
import akka.http.model.HttpRequest
import awsync.{Credentials, Service, Region}

object Sign {

  def apply(request: HttpRequest, region: Region, service: Service, credentials: Credentials): HttpRequest =
    apply(request, region, service, credentials, new Date)

  def apply(request: HttpRequest, region: Region, service: Service, credentials: Credentials, date: Date): HttpRequest = {
    val dateTimeString = DateFormats.formatDateTime(date)
    val dateString = dateTimeString.substring(0, 8)

    val requestWithDate = request.addHeader(RawHeader("x-amz-date", dateTimeString))

    val canonical = CanonicalRequest.create(requestWithDate)
    val stringToSign = StringToSign.create(dateTimeString, region, service, canonical.hash)
    val signature = Signature.create(credentials.secret, date, region, service, stringToSign)

    val algorithm = "AWS4-HMAC-SHA256"
    val credential = s"${credentials.key.key}/$dateString/${region.name}/${service.name}/aws4_request"
    val signedHeaders = canonical.signedHeaders
    val authHeader = s"$algorithm Credential=$credential, SignedHeaders=$signedHeaders, Signature=$signature"

    requestWithDate.addHeader(RawHeader("Authorization", authHeader))
  }

}