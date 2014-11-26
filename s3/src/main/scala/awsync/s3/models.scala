package awsync.s3

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

sealed trait NoAccessReason
case object DoesNotExist extends NoAccessReason
case object PermissionDenied extends NoAccessReason