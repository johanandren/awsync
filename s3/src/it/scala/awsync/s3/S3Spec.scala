package awsync.s3

import akka.NotUsed
import akka.http.scaladsl.model.ContentTypes
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source

import scala.collection.immutable.Seq
import akka.actor.ActorSystem
import akka.util.ByteString
import awsync.{Regions, Credentials}
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, FunSpec}
import scala.concurrent.duration._
import scala.concurrent.Await

class S3Spec extends FunSpec with Matchers with ScalaFutures with BeforeAndAfterAll {


  val config = ConfigFactory.load("it")

  val s3Key = config.getString("s3.key")
  val s3Secret = config.getString("s3.secret")
  val s3Region = Regions.fromName(config.getString("s3.region")).get
  val bucket = BucketName(config.getString("s3.bucket"))

  implicit val system: ActorSystem = ActorSystem("it", ConfigFactory.parseString(
    """
      |akka.log-dead-letters-during-shutdown: false
      |akka.log-dead-letters: 1
    """.stripMargin))

  implicit val ec = system.dispatcher
  implicit val fm = ActorMaterializer()
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
      val loc = FqKey(bucket, key)
      val data = ByteString("some data".getBytes("UTF-8"))

      val result =
        for {
          _ <- client.createObject(loc, ContentTypes.`application/octet-stream` ,data, CreateObjectConfig.default)
          (metadata, bytes) <- client.getObject(loc)
          _ <- client.deleteObject(loc)
        } yield bytes

      result.futureValue shouldEqual data
    }

    it("creates, fetches and deletes an object with streaming data") {
      val key = Key("it-test-object-stream")
      val loc = FqKey(bucket, key)
      val data: ByteString = ByteString(Array.tabulate(100)(n => n.toByte))
      val source: Source[ByteString, NotUsed] = Source.fromIterator(() => data.grouped(10))

      val result =
        for {
          _ <- client.createObject(loc, ContentTypes.`application/octet-stream`, source, data.length, CreateObjectConfig.default)
          (metadata, objectStream) <- client.getObjectStream(loc)
          readData <- objectStream.runFold(ByteString())((acc, chunk) => acc ++ chunk)
          _ <- client.deleteObject(loc)
        } yield readData

      result.futureValue shouldEqual data
    }

    it("returns DoesNotExist when trying to fetch a non-existant key") {
      val key = Key("does-not-exist")
      val loc = FqKey(bucket, key)

      val result = client.getObject(loc)

      result.failed.futureValue should be(DoesNotExist)
    }

    it("deletes multiple keys") {
      val key1 = Key("it-test-delete-object-1")
      val key2 = Key("it-test-delete-object-2")
      val data = ByteString("some data".getBytes("UTF-8"))

      val result =
        for {
          _ <- client.createObject(FqKey(bucket, key1), ContentTypes.`application/octet-stream`, data, CreateObjectConfig.default)
          _ <- client.createObject(FqKey(bucket, key2), ContentTypes.`application/octet-stream`, data, CreateObjectConfig.default)
          _ <- client.deleteObjects(bucket, Seq(key1, key2))
          items <- client.listObjects(bucket)
        } yield items

      result.futureValue._2 should be (empty)
    }

  }

  override protected def afterAll(): Unit = {
    Await.result(system.terminate(), 20.seconds)
  }
}
