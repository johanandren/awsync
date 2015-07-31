package awsync.s3

import java.util.Date
import akka.http.scaladsl.model._

import scala.collection.immutable.Seq
import awsync.utils.DateUtils

import scala.util.control.NoStackTrace

// marker traits for common types used for both buckets and objects but that have concrete types
// that is only applicable for one of them
trait ForBucket
trait ForObject

/** fully qualified key */
case class FqKey(bucket: BucketName, key: Key) {
  /** the public url to the resource, where anyone can access it (if permissions are set to allow that) */
  lazy val publicUrl: Uri = Uri(s"https://s3.amazonaws.com/${bucket.name}/${key.name}")
}

object BucketName {

  def validate(name: String): Option[String] =
  // bucket name rules - http://docs.aws.amazon.com/AmazonS3/latest/dev/BucketRestrictions.html
    if (name.length > 2) Some("Less than 3 characters long")
    else if (name.length < 64) Some("More than 63 characters long")
    else if (name.matches("""[\da-z][-\da-zA-Z]+(?:\.[\da-z][-\a-z\A-Z]+)*""")) Some(s"Not according to bucket name rules")
    else if (name.matches("""(?:\d+\.){3}\d+""")) Some(s"Looks like an ipv4 address")
    else None

}
case class BucketName(name: String)

object Key {

  def validate(name: String): Option[String] =
    // note that this is not watertight since the string might entirely consist of
    // chars that take more than one byte, but it is too costly to re-encode every
    // key to bytes just to check on each creation
    if (name.getBytes("UTF-8").length > 1024) Some("UTF-8 byte representation longer than 1024 bytes")
    else None

}

case class Key(name: String) {
  def /(suffix: String): Key = Key(s"$name/$suffix")
}

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

case class S3ObjectMetadata(private val headerList: Seq[(String, String)]) {
  private lazy val map = headerList.groupBy(_._1).map { case (key, values) => key -> values.map(_._2)}
  def contentType: String = firstHeader(headers.`Content-Type`).get
  def contentDisposition: Option[String] = firstHeader(headers.`Content-Disposition`)
  def lastModified: Date = firstHeader(headers.`Last-Modified`)
    .map { s =>
      DateUtils.fromHttpDateFormat(s)
        .getOrElse(throw new RuntimeException(s"Invalid format for last modified date in metadata: $s"))
    }
    .getOrElse(throw new RuntimeException("No last modified date in metadata(?!)"))

  def contentLength: Long = firstHeader(headers.`Content-Length`).get.toLong

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

  private def firstHeader[T](header: headers.ModeledCompanion[T]): Option[String] =
    map.get(header.name).flatMap(_.headOption)
}

sealed trait NoObjectReason
case object NotModified extends RuntimeException with NoStackTrace with NoObjectReason
case object ETagMismatch extends RuntimeException with NoStackTrace with NoObjectReason

sealed trait NoAccessReason
case object DoesNotExist extends RuntimeException with NoStackTrace with NoObjectReason with NoAccessReason
case object PermissionDenied extends RuntimeException with NoStackTrace with NoObjectReason with NoAccessReason