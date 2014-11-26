package awsync.s3

import java.util.Date
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.immutable.Seq
import scala.concurrent.Future
import akka.actor.ActorSystem
import spray.http.ByteRange
import awsync.{Region, Credentials}

trait S3Client {

  /** @return Each bucket owned by the account along with the date it was created */
  def listBuckets: Future[Seq[(BucketName, Date)]]

  /**
   * @return None if the bucket is accessible with the current credentials, the reason why if it is not
   */
  def canAccess(bucket: BucketName): Future[Option[NoAccessReason]]

  /**
    * List objects in the given bucket (using defaults for the parameters of the many-parameter-version of the method)
   */
  def listObjects(bucket: BucketName): Future[(ListObjectsInfo, Seq[KeyDetails])] =
    listObjects(bucket, ListObjectsConfig(None, None, None, None))

  /**
    * List objects in the given bucket
   * @param config Details about how to filter/search the listing
   */
  def listObjects(bucket: BucketName, config: ListObjectsConfig): Future[(ListObjectsInfo, Seq[KeyDetails])]


  /**
   * @return None if there is no such object, the metadata of the object at key if there is.
   */
  def getObjectMetadata(bucket: BucketName, key: Key): Future[Option[S3ObjectMetadata]]

  /**
   * Fetch the entire contents of a key
   */
  def getObject(bucket: BucketName, key: Key): Future[Option[S3Object]] = getObject(bucket, key, None, None).map(_.right.toOption)

  /**
   * Fetch the contents of a key, or parts of it, possibly with hash/modification date check
   *
   * @param range Fetch just these bytes
   * @param conditions Limit fetching based on last modified or etag
   * @return Either the reason why it was not fetched or the object data
   */
  def getObject(bucket: BucketName, key: Key, range: Option[ByteRange], conditions: Option[GetObjectCondition]): Future[Either[NoObjectReason, S3Object]]

}

object S3Client {

  /**
   * @return A thread safe/shareable client to use for communication with amzon s3
   */
  def apply(credentials: Credentials, region: Region)(implicit system: ActorSystem): S3Client =
    ConcreteS3Client(credentials, region, https = false)(system)

}