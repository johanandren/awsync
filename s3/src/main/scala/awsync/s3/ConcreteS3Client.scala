package awsync.s3

import java.util.Date

import scala.concurrent.duration._
import scala.collection.immutable.Seq
import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.util.Timeout
import awsync.{Service, Region, Credentials}
import spray.can.Http
import spray.http._

import scala.concurrent.Future
import scala.util.Try
import scala.xml.{Elem, XML}

private[s3] object ConcreteS3Client {

  def apply(credentials: Credentials, region: Region, https: Boolean = true)(implicit system: ActorSystem): S3Client =
    new ConcreteS3Client(credentials, region, https)(system)


  private val service = Service("s3")

}

private[s3] final class ConcreteS3Client(
    credentials: Credentials,
    region: Region,
    https: Boolean
  )(
    implicit system: ActorSystem
  ) extends S3Client {

  import ConcreteS3Client._
  import awsync.authentication.Authentication
  import HttpMethods._
  import system.dispatcher

  implicit val timeout: Timeout = 30.seconds
  private val (protocol, port) = if (https) ("https", 443) else ("http", 80)

  private val baseUri = Uri(s"$protocol://s3.amazonaws.com")
  private def bucketBaseUri(bucket: BucketName) = Uri(s"$protocol://${bucket.name}.s3.amazonaws.com")

  // api implementation

  override def listBuckets: Future[Seq[(BucketName, Date)]] =
    sendRequest(signedRequest(GET, baseUri, ""))
      .map(handleResponse(parsers.ListBuckets.parse))

  override def listObjects(bucket: BucketName, config: ListObjectsConfig): Future[(ListObjectsInfo, Seq[KeyDetails])] = {
    val parameters = Seq(
      config.delimiter.map("delimiter" -> _),
      config.marker.map("marker" -> _),
      config.maxKeys.map("maxKeys" -> _.toString),
      config.prefix.map("prefix" -> _)
    )
    val uri = bucketBaseUri(bucket).withQuery(parameters.flatten: _*)
    sendBucketRequest(bucket, signedRequest(GET, uri, ""))
      .map(handleResponse(parsers.ListObjects.parse))
  }

  override def canAccess(bucket: BucketName): Future[Option[NoAccessReason]] =
    sendBucketRequest(bucket, signedRequest(HEAD, bucketBaseUri(bucket), ""))
      .map { response =>
        response.status match {
          case StatusCodes.OK => None
          case StatusCodes.NotFound => Some(DoesNotExist)
          case StatusCodes.Forbidden => Some(PermissionDenied)
          case _ => exceptionFor(response)
        }
      }




  // helpers

  private def handleResponse[A](onOk: Elem => Try[A])(response: HttpResponse): A =
    if (response.status == StatusCodes.OK) onOk(XML.loadString(response.entity.asString)).get
    else throw exceptionFor(response)

  private def exceptionFor(response: HttpResponse): Nothing =
    throw new RuntimeException(s"Failed request, status: ${response.status}, body: ${response.entity.asString}")


  private def signedRequest(method: HttpMethod, uri: Uri, body: String): HttpRequest =
    Authentication.signWithHeader(
      HttpRequest(method, uri, List(HttpHeaders.Host(uri.authority.host.address))),
      region,
      service,
      credentials
    )

  private def sendRequest(request: HttpRequest): Future[HttpResponse] =
    baseConnectorInfo.flatMap(_.hostConnector ? request).mapTo[HttpResponse]

  private def sendBucketRequest(bucket: BucketName, request: HttpRequest): Future[HttpResponse] =
    bucketConnectorInfo(bucket).flatMap(_.hostConnector ? request).mapTo[HttpResponse]


  // host connector for bucket-less requests
  private def baseConnectorInfo: Future[Http.HostConnectorInfo]  =
    (IO(Http) ? Http.HostConnectorSetup("s3.amazonaws.com", port)).mapTo[Http.HostConnectorInfo]

  private def bucketConnectorInfo(bucket: BucketName): Future[Http.HostConnectorInfo] =
    (IO(Http) ? Http.HostConnectorSetup(s"${bucket.name}.s3.amazonaws.com", port))
      .mapTo[Http.HostConnectorInfo]

}
