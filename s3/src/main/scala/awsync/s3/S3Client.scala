package awsync.s3

import akka.actor.ActorSystem
import akka.stream.Materializer
import awsync.{Region, Credentials}

/**
 * API for interacting with amazon s3
 *
 * In general failures are returned as failed futures containing an exception, some operations
 * have return types for some of the failures that might occur.
 */
trait S3Client extends BucketOperations with ObjectOperations with HighLevelOperations


object S3Client {

  /**
   * @return A thread safe/shareable client to use for communication with amzon s3
   */
  def apply(credentials: Credentials, region: Region)(implicit system: ActorSystem, materializer: Materializer): S3Client =
    ConcreteS3Client(credentials, region, https = false)(system, materializer)

}