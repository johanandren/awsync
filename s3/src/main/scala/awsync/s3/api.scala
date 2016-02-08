package awsync.s3

import java.util.Date

import akka.{Done, NotUsed}
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.headers.ByteRange
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

/**
 * Provides versions of the low level operations with as few parameters as possible, combining the low level operations
 * into common more high level use cases
 */
trait HighLevelOperations { this: BucketOperations with ObjectOperations =>

  /** execution context to run all composed operation on top of */
  implicit protected def executionContext: ExecutionContext
  /** materializer to use for stream operations */
  implicit protected def materializer: Materializer


  /**
   * Fetch key details for all the keys (or a subset using the config). If you have a bucket with very large numbers
   * of keys, be careful since it might fill your heap, in that case use [[BucketOperations.listObjectStream]] instead.
   */
  def listObjects(bucket: BucketName, config: ListObjectsConfig = ListObjectsConfig.default): Future[(ListObjectsInfo, Seq[KeyDetails])] = {
    listObjectStream(bucket, config).flatMap {
      case ((info, source)) =>
        source.runFold(Seq[KeyDetails]()) { (acc, details) =>
          acc ++ details
        }.map(allKeys => (info, allKeys))
    }
  }

  /**
   * Fetch the _entire_ contents of a key into memory. If the object cannot be read for one reason or another
   * the future will be failed. For big files, use getObjectSource instead as that will stream a chunk at a time
   * rather than fill up the memory.
   */
  def getObject(key: FqKey, range: Option[ByteRange] = None): Future[(S3ObjectMetadata, ByteString)] = {
    getObjectStream(key).flatMap {
      case ((metadata, source)) =>
        source.runFold(ByteString()) { (acc, blob) =>
          acc ++ blob
        }.map(allBytes => (metadata, allBytes))
    }
  }


}


trait ObjectOperations {

  /**
   * Create a new object by streaming data in chunks, performing multiple requests
   */
  def createObject(key: FqKey, contentType: ContentType, data: Source[ByteString, NotUsed], dataLength: Long, config: CreateObjectConfig): Future[Done]

  /**
   * Create a new object under the given key, with data that is available in memory, as a single request
   */
  def createObject(key: FqKey, contentType: ContentType, data: ByteString, config: CreateObjectConfig): Future[Done]

  /**
   * @return None if there is no such object, the metadata of the object at key if there is.
   */
  def getObjectMetadata(key: FqKey): Future[Option[S3ObjectMetadata]]

  /**
   * @return A future that completes with the metadata and then the stream of bytes for the object or fails with details
   *         about why (not such key, for example)
   */
  def getObjectStream(key: FqKey, range: Option[ByteRange] = None, conditions: Option[GetObjectCondition] = None): Future[(S3ObjectMetadata, Source[ByteString, NotUsed])]

  /** Delete the object at the given key */
  def deleteObject(fqKey: FqKey): Future[Done]

  /** Delete multiple objects with one request (max 1000 keys - TODO split more over many requests or return error?) */
  def deleteObjects(bucket: BucketName, keys: Seq[Key]): Future[Done]
}

trait BucketOperations {

  /** @return Each bucket owned by the account along with the date it was created */
  def listBuckets: Future[Seq[(BucketName, Date)]]

  /**
   * @return None if the bucket is accessible with the current credentials, the reason why if it is not
   */
  def canAccess(bucket: BucketName): Future[Option[NoAccessReason]]

  /**
   * List objects in the given bucket (possibly filtered/limited by the config given).
   * Default config is to list all objects in the bucket
   *
   * @param config Details about how to filter/search the listing
   */
  def listObjectStream(bucket: BucketName, config: ListObjectsConfig): Future[(ListObjectsInfo, Source[Seq[KeyDetails], NotUsed])]
  protected implicit def executionContext: ExecutionContext

//  /**
//   * List all objects in the given bucket. Note that all will be put in one seq in memory so with
//   * really big buckets this might be a problem.
//   */
//  def listAllObjects(bucket: BucketName): Future[Seq[KeyDetails]] = {
//    listObjects(bucket, config).map()
//
//
//    def loop(acc: Seq[KeyDetails]): Future[Seq[KeyDetails]] = {
//
//      val config =
//        if (acc.isEmpty) ListObjectsConfig.default
//        else ListObjectsConfig.default.copy(marker = Some(acc.last.key.name))
//
//
//      listObjects(bucket, config).flatMap { case (info, keys) =>
//        val keysSoFar: Seq[KeyDetails] = acc ++ keys
//        if (info.isTruncated) {
//          loop(keysSoFar)
//        } else {
//          Future.successful(keysSoFar)
//        }
//
//      }
//    }
//
//    loop(Seq())
//  }


}
