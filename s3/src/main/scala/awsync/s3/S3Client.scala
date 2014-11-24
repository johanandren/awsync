package awsync.s3

import java.util.Date

import scala.collection.immutable.Seq
import scala.concurrent.Future
import akka.actor.ActorSystem
import awsync.{Region, Credentials}

trait S3Client {

  /** @return Each bucket owned by the account along with the date it was created */
  def listBuckets: Future[Seq[(Bucket, Date)]]

}

object S3Client {

  /**
   * @return A thread safe/shareable client to use for communication with amzon s3
   */
  def apply(credentials: Credentials, region: Region)(implicit system: ActorSystem): Future[S3Client] =
    ConcreteS3Client(credentials, region)(system)

}