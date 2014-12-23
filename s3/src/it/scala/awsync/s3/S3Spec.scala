package awsync.s3

import akka.actor.ActorSystem
import akka.util.ByteString
import awsync.{Region, Regions, Credentials}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}

class S3Spec extends FunSpec with Matchers with ScalaFutures with BeforeAndAfterAll {

  val config = ConfigFactory.load("it")

  val s3Key = config.getString("s3.key")
  val s3Secret = config.getString("s3.secret")
  val s3Region = Regions.fromName(config.getString("s3.region")).get
  val bucket = BucketName(config.getString("s3.bucket"))

  implicit val system = ActorSystem("it", ConfigFactory.parseString(
    """
      |akka.log-dead-letters-during-shutdown: false
      |akka.log-dead-letters: 1
    """.stripMargin))

  implicit val ec = system.dispatcher
  val client = S3Client(Credentials(s3Key, s3Secret), s3Region)

  implicit override val patienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(100, Millis))


  describe("The low level s3 api") {

    it("lists buckets") {
      val result = client.listBuckets

      whenReady(result) { list =>
        list.size should be >= 1
        list.map(_._1) should contain (bucket)
      }
    }

    it("lists contents of a bucket") {
      val result = client.listObjects(bucket, ListObjectsConfig.default)

      whenReady(result) { case (info, keys) =>
        info.bucket should be (bucket)
        keys should be (empty)
      }
    }

    it("creates, fetches and deletes an object") {
      val key = Key("it-test-object")
      val data = ByteString("some data".getBytes("UTF-8"))

      val result =
        for {
          _ <- client.createObject(bucket, key, data, CreateObjectConfig.default)
          maybeObj <- client.getObject(bucket, key)
          _ <- client.deleteObject(bucket, key)
        } yield maybeObj.map(_.data)

      result.futureValue should be (Some(data))
    }

    it("returns DoesNotExist when trying to fetch a non-existant key") {
      val key = Key("does-not-exist")

      val result = client.getObject(bucket, key, None, None)

      result.futureValue should be (Left(DoesNotExist))
    }

  }

  override protected def afterAll(): Unit = {
    system.shutdown()
  }
}
