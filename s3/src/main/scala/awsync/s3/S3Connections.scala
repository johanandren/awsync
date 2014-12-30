package awsync.s3

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.io.IO
import akka.util.Timeout
import spray.can.Http
import spray.http.{HttpResponse, HttpRequest, Uri}

import scala.concurrent.Future

private[s3] trait S3Connections {

  def https: Boolean

  implicit def system: ActorSystem
  implicit val executionContext = system.dispatcher
  implicit val timeout: Timeout = 30.seconds

  private val (protocol, port) = if (https) ("https", 443) else ("http", 80)

  protected val baseUri = Uri(s"$protocol://s3.amazonaws.com")
  protected def bucketBaseUri(bucket: BucketName) = Uri(s"$protocol://${bucket.name}.s3.amazonaws.com")

  protected def sendRequest(request: HttpRequest): Future[HttpResponse] =
    baseConnectorInfo.flatMap(_.hostConnector ? request).mapTo[HttpResponse]

  protected def sendBucketRequest(bucket: BucketName, request: HttpRequest): Future[HttpResponse] =
    bucketConnectorInfo(bucket).flatMap(_.hostConnector ? request).mapTo[HttpResponse]

  // host connectors - spray will manage a pool of instances for each host for us
  // TODO maybe tweak the size of those pools somehow?
  private def baseConnectorInfo: Future[Http.HostConnectorInfo]  =
    (IO(Http) ? Http.HostConnectorSetup("s3.amazonaws.com", port)).mapTo[Http.HostConnectorInfo]

  private def bucketConnectorInfo(bucket: BucketName): Future[Http.HostConnectorInfo] =
    (IO(Http) ? Http.HostConnectorSetup(s"${bucket.name}.s3.amazonaws.com", port))
      .mapTo[Http.HostConnectorInfo]
}