package awsync.s3

import akka.actor.ActorSystem
import awsync.{Region, Credentials}

/**
 * API for interacting with amazon s3
 *
 * In general failures are returned as failed futures containing an exception, some operations
 * have return types for some of the failures that might occur.
 */
trait S3Client extends LowLevelBucketOperations with LowLevelObjectOperations with SimpleLowLevelOperations


object S3Client {

  /**
   * @return A thread safe/shareable client to use for communication with amzon s3
   */
  def apply(credentials: Credentials, region: Region)(implicit system: ActorSystem): S3Client =
    ConcreteS3Client(credentials, region, https = false)(system)

}