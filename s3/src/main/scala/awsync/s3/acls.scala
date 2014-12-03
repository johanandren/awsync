package awsync.s3

/** A canned ACL is a predefined set of grants, making setting permissions easier than having to construct */
sealed abstract class CannedAcl(private[s3] val name: String)

object CannedAcls {
  /** Owner gets FULL_CONTROL. No one else has access rights (default). */
  case object Private extends CannedAcl("private") with ForBucket with ForObject
  /** Owner gets FULL_CONTROL. The AllUsers group gets READ access */
  case object PublicRead extends CannedAcl("public-read") with ForBucket with ForObject
  /** Owner gets FULL_CONTROL. The AllUsers group gets READ and WRITE access.
    * Granting this on a bucket is generally not recommended */
  case object PublicReadWrite extends CannedAcl("public-read-write") with ForBucket with ForObject
  /** Owner gets FULL_CONTROL. The AuthenticatedUsers group gets READ access */
  case object AuthenticatedRead extends CannedAcl("authenticated-read") with ForBucket with ForObject
  /** Object owner gets FULL_CONTROL. Bucket owner gets READ access. */
  case object BucketOwnerRead extends CannedAcl("bucket-owner-read") with ForObject
  /** Both the object owner and the bucket owner get FULL_CONTROL over the object. */
  case object BucketOwnerFullControl extends CannedAcl("bucket-owner-full-control") with ForObject
  /** The LogDelivery group gets WRITE and READ_ACP permissions on the bucket. */
  case object LogDeliveryWrite extends CannedAcl("log-delivery-write") with ForBucket
}

case class Grant(grantee: User, permission: Permission)

sealed trait Permission
object Permissions {
  /** Allows grantee to list the objects in the bucket or the contents and metadata of the object */
  case object Read extends Permission with ForBucket with ForObject
  /** Allows grantee to create, overwrite, and delete any object in the bucket */
  case object Write extends Permission with ForBucket
  /** Allows grantee to read the bucket or object ACL */
  case object ReadAcl extends Permission with ForBucket with ForObject
  /** Allows grantee to write the ACL for the applicable bucket or object */
  case object WriteAcl extends Permission with ForBucket with ForObject
  /** Allows grantee the all the other permissions on the bucket or object */
  case object FullControl extends Permission with ForBucket with ForObject
}