package awsync.http

import java.util.Date
import akka.http.scaladsl.model.HttpHeader
import akka.http.scaladsl.model.headers.RawHeader
import awsync.utils.DateUtils

object AwsHeaders {

  def AmzContentSha256(hash: String): HttpHeader = RawHeader("x-amz-content-sha256", hash)
  def AmzDate(date: Date): HttpHeader = RawHeader("x-amz-date", DateUtils.toIso8601DateTimeFormat(date))

}
