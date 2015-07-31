package awsync.s3

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpResponse, HttpRequest, Uri}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.ActorMaterializer

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.util.Timeout

import scala.concurrent.Future

private[s3] trait S3Connections {

  def https: Boolean

  implicit def system: ActorSystem

  private implicit val fm = ActorMaterializer()

  private def http = Http()

  implicit val executionContext = system.dispatcher

  private val (protocol, port) = if (https) ("https", 443) else ("http", 80)

  protected val baseUri = Uri(s"$protocol://s3.amazonaws.com")

  protected def bucketBaseUri(bucket: BucketName) = Uri(s"$protocol://${bucket.name}.s3.amazonaws.com")

  protected def sendRequest(request: HttpRequest): Future[HttpResponse] =
    Source.single(request)
      .via(http.outgoingConnection("s3.amazonaws.com", port))
      .runWith(Sink.head)

  protected def sendBucketRequest(bucket: BucketName, request: HttpRequest): Future[HttpResponse] =
    Source.single(request)
      .via(http.outgoingConnection(s"${bucket.name}.s3.amazonaws.com", port))
      .runWith(Sink.head)



}