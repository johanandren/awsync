package awsync.s3

import java.util.Date
import spray.http.HttpHeaders.ModeledCompanion

import scala.collection.immutable.Seq
import akka.util.ByteString
import awsync.utils.DateUtils
import spray.http.HttpHeaders

// marker traits for common types used for both buckets and objects but that have concrete types
// that is only applicable for one of them
trait ForBucket
trait ForObject

case class BucketName(name: String)
case class Key(name: String)
case class ETag(tag: String)
case class ByteSize(bytes: Long) {
  def kb: BigDecimal = BigDecimal(bytes) / 1000
  def mb: BigDecimal = kb / 1000
}



sealed abstract class StorageClass(private[s3] val name: String)
/** storage class allowed to be set on new object */
sealed trait ForCreate
object StorageClasses {
  case object Standard extends StorageClass("STANDARD") with ForCreate
  case object ReducedRedundancy extends StorageClass("REDUCED_REDUNDANCY") with ForCreate
  case object Glacier extends StorageClass("GLACIER")
}

case class OwnerId(id: String)
case class User(id: OwnerId, displayName: String)

case class KeyDetails(key: Key, lastModified: Date, etag: ETag, size: ByteSize, storageClass: StorageClass, owner: User)

case class ListObjectsInfo(bucket: BucketName, prefix: String, marker: String, maxKeys: Int, isTruncated: Boolean)


case class CustomMetadataKey(name: String)
object CustomMetadataKey {
  /** the http header prefix for custom metadata */
  private[s3] val headerPrefix = "x-amz-meta-"
}

case class S3Object(metadata: S3ObjectMetadata, data: ByteString)
case class S3ObjectMetadata(private val headers: Seq[(String, String)]) {
  private lazy val map = headers.groupBy(_._1).map { case (key, values) => key -> values.map(_._2)}.toMap
  def contentType: String = firstHeader(HttpHeaders.`Content-Type`).get
  def contentDisposition: Option[String] = firstHeader(HttpHeaders.`Content-Disposition`)
  def lastModified: Date = firstHeader(HttpHeaders.`Last-Modified`)
    .map { s =>
      DateUtils.fromHttpDateFormat(s)
        .getOrElse(throw new RuntimeException(s"Invalid format for last modified date in metadata: $s"))
    }
    .getOrElse(throw new RuntimeException("No last modified date in metadata(?!)"))

  def contentLength: Long = firstHeader(HttpHeaders.`Content-Length`).get.toLong

  /** the object version id */
  def version: String = oneValueFor("x-amz-version-id").get

  /**
   * Useful for headers not covered by the metadata access methods in this class
   * @param headerName The name of a http header in the response (case sensitive)
   * @return The value of the header, if it exists
   */
  def oneValueFor(headerName: String): Option[String] = map.get(headerName).map(_.head)
  def allValuesFor(headerName: String): Seq[String] = map.getOrElse(headerName, Seq())

  
  def oneValueFor(key: CustomMetadataKey): Option[String] = oneValueFor(CustomMetadataKey.headerPrefix + key.name)
  def allValuesFor(key: CustomMetadataKey): Seq[String] = allValuesFor(CustomMetadataKey.headerPrefix + key.name)

  private def firstHeader(header: ModeledCompanion): Option[String] = map.get(header.name).flatMap(_.headOption)
}

sealed trait NoObjectReason
case object NotModified extends NoObjectReason
case object ETagMismatch extends NoObjectReason

sealed trait NoAccessReason
case object DoesNotExist extends NoAccessReason with NoObjectReason
case object PermissionDenied extends NoAccessReason