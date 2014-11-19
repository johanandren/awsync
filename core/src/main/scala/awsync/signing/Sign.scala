package awsync.signing

import java.util.Date

import akka.http.model.headers.RawHeader
import akka.http.model.HttpRequest
import awsync.{Credentials, Service, Region}

object Sign {

  def apply(request: HttpRequest, region: Region, service: Service, credentials: Credentials): HttpRequest = {

    val date = new Date

    val requestWithDate = request.addHeader(RawHeader("x-amz-date", DateFormats.formatDateTime(date)))

    val canonical = CanonicalRequest.create(requestWithDate)
    val stringToSign = StringToSign.create(date, region, service, canonical)
    val signature = Signature.create(credentials.secret, date, region, service, stringToSign)

    val algorithm = "AWS4-HMAC-SHA256"
    val credential = "AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request"
    val signedHeaders = canonical.signedHeaders
    val authHeader = s"$algorithm Credential=$credential, SignedHeaders=$signedHeaders, Signature=$signature"

    requestWithDate.addHeader(RawHeader("Authorization", authHeader))
  }

}