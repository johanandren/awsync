package awsync.http

import java.util.Date

import awsync.DateUtils
import spray.http.HttpHeader
import spray.http.HttpHeaders.RawHeader

object AwsHeaders {

  def AmzContentSha256(hash: String): HttpHeader = RawHeader("x-amz-content-sha256", hash)
  def AmzDate(date: Date): HttpHeader = RawHeader("x-amz-date", DateUtils.toIso8601DateTimeFormat(date))

}
