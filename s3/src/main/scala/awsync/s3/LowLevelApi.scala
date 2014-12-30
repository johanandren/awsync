package awsync.s3

import java.util.Date

import akka.util.ByteString
import spray.http.ByteRange

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

/**
 * Low level s3 api operations, basically mapping 1:1 with the s3 rest api
 */


/**
 * Provides versions of the low level operations with as few parameters as possible
 */
trait SimpleLowLevelOperations { this: LowLevelBucketOperations with LowLevelObjectOperations =>

  implicit def executionContext: ExecutionContext

  /**
   * List objects in the given bucket,
   * if the bucket contains more objects than fits in one request many requests will be performed
   */
  def listAllObjects(bucket: BucketName): Future[Seq[KeyDetails]] = {
    def loop(acc: Seq[KeyDetails]): Future[Seq[KeyDetails]] = {

      val config =
        if (acc.isEmpty) ListObjectsConfig.default
        else ListObjectsConfig.default.copy(marker = Some(acc.last.key.name))

      listObjects(bucket, config).flatMap { case (info, keys) =>
        val keysSoFar: Seq[KeyDetails] = acc ++ keys
        if (info.isTruncated) {
          loop(keysSoFar)
        } else {
          Future.successful(keysSoFar)
        }

      }
    }

    loop(Seq())
  }

  /**
   * Fetch the entire contents of a key, if it could not be found or
   */
  def getObject(bucket: BucketName, key: Key): Future[Option[S3Object]] = getObject(bucket, key, None, None).map {
    case Right(s3object) => Some(s3object)
    case Left(DoesNotExist) => None
    case Left(PermissionDenied) => throw new RuntimeException(s"You do not have permission to access $bucket $key")
    // these cannot happen since we do not send in any conditions
    // but are needed to keep the compiler happy
    case Left(ETagMismatch) => None
    case Left(NotModified) => None
  }

}

trait LowLevelObjectOperations {

  /**
   * Create a new object under the given key
   */
  def createObject(bucket: BucketName, key: Key, data: ByteString, config: CreateObjectConfig): Future[Unit]

  /**
   * @return None if there is no such object, the metadata of the object at key if there is.
   */
  def getObjectMetadata(bucket: BucketName, key: Key): Future[Option[S3ObjectMetadata]]

  /**
   * Fetch the contents of a key, or parts of it, possibly with hash/modification date check
   * Important: will load the entire object into memory, so be careful not to fetch objects that will not fit in your heap.
   *
   * @param range Fetch just these bytes
   * @param conditions Limit fetching based on last modified or etag
   * @return Either the reason why it was not fetched or the object data
   */
  def getObject(bucket: BucketName, key: Key, range: Option[ByteRange], conditions: Option[GetObjectCondition]): Future[Either[NoObjectReason, S3Object]]

  /** Delete the object at the given key */
  def deleteObject(bucket: BucketName, key: Key): Future[Unit]

  /** Delete multiple objects with one request (max 1000) */
  def deleteObjects(bucket: BucketName, keys: Seq[Key]): Future[Unit]
}

trait LowLevelBucketOperations {

  /** @return Each bucket owned by the account along with the date it was created */
  def listBuckets: Future[Seq[(BucketName, Date)]]

  /**
   * @return None if the bucket is accessible with the current credentials, the reason why if it is not
   */
  def canAccess(bucket: BucketName): Future[Option[NoAccessReason]]

  /**
   * List objects in the given bucket
   *
   * Note that if the list of objects is long it will get truncated and you will have to look at ListObjectsInfo
   * to figure out if you need to fetch multiple times.
   *
   * @param config Details about how to filter/search the listing
   */
  def listObjects(bucket: BucketName, config: ListObjectsConfig): Future[(ListObjectsInfo, Seq[KeyDetails])]

}
