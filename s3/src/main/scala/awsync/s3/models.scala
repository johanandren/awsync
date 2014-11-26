package awsync.s3

import akka.util.ByteString
import awsync.utils.DateUtils
import spray.http.{HttpHeader, HttpHeaders}

import scala.collection.immutable.Seq
import java.util.Date

case class BucketName(name: String)
case class Key(name: String)
case class ETag(tag: String)
case class ByteSize(bytes: Long) {
  def kb: BigDecimal = BigDecimal(bytes) / 1000
  def mb: BigDecimal = kb / 1000
}

sealed trait StorageClass
object StorageClasses {
  case object Standard extends StorageClass
  case object Glacier extends StorageClass
  case object ReducedRedundancy extends StorageClass
}
case class OwnerId(id: String)
case class Owner(id: OwnerId, displayName: String)

case class KeyDetails(key: Key, lastModified: Date, etag: ETag, size: ByteSize, storageClass: StorageClass, owner: Owner)

case class ListObjectsInfo(bucket: BucketName, prefix: String, marker: String, maxKeys: Int, isTruncated: Boolean)


case class S3Object(metadata: S3ObjectMetadata, data: ByteString)
case class S3ObjectMetadata(private val headers: Map[String, String]) {
  def contentType: String = headers(HttpHeaders.`Content-Type`.name)
  def contentDisposition: Option[String] = headers.get(HttpHeaders.`Content-Disposition`.name)
  def lastModified: Date = DateUtils.fromHttpDateFormat(HttpHeaders.`Last-Modified`.name).get
  def contentLength: Long = headers(HttpHeaders.`Content-Length`.name).toLong

  /**
   * Useful for headers not covered by the metadata access methods in this class
   * @param headerName The name of a http header in the response (case sensitive)
   * @return The value of the header, if it exists
   */
  def valueFor(headerName: String): Option[String] = headers.get(headerName)
}



sealed trait NoAccessReason
case object DoesNotExist extends NoAccessReason
case object PermissionDenied extends NoAccessReason
