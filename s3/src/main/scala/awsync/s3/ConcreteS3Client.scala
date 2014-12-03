package awsync.s3

import java.util.Date
import scala.concurrent.duration._
import scala.collection.immutable.Seq
import scala.xml.{Elem, XML}
import scala.concurrent.Future
import scala.util.Try
import akka.pattern.ask
import akka.actor.{ActorRef, ActorSystem}
import akka.io.IO
import akka.util.{ByteString, Timeout}
import spray.http.HttpHeaders.RawHeader
import spray.can.Http
import spray.http._
import awsync.{Service, Region, Credentials}

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
    sendRequest(signedRequest(GET, baseUri))
      .map(handleResponse(parsers.ListBuckets.parse))

  override def listObjects(bucket: BucketName, config: ListObjectsConfig): Future[(ListObjectsInfo, Seq[KeyDetails])] = {
    val parameters = Seq(
      config.delimiter.map("delimiter" -> _),
      config.marker.map("marker" -> _),
      config.maxKeys.map("maxKeys" -> _.toString),
      config.prefix.map("prefix" -> _)
    )
    val uri = bucketBaseUri(bucket).withQuery(parameters.flatten: _*)
    sendBucketRequest(bucket, signedRequest(GET, uri))
      .map(handleResponse(parsers.ListObjects.parse))
  }

  override def canAccess(bucket: BucketName): Future[Option[NoAccessReason]] =
    sendBucketRequest(bucket, signedRequest(HEAD, bucketBaseUri(bucket)))
      .map { response =>
        response.status match {
          case StatusCodes.OK => None
          case StatusCodes.NotFound => Some(DoesNotExist)
          case StatusCodes.Forbidden => Some(PermissionDenied)
          case _ => exceptionFor(response)
        }
      }

  override def getObjectMetadata(bucket: BucketName, key: Key): Future[Option[S3ObjectMetadata]] = {
    val request = signedRequest(HEAD, bucketBaseUri(bucket).withPath(path(key)))
    sendBucketRequest(bucket, request)
      .map { response =>
        response.status match {
          case StatusCodes.OK => Some(S3ObjectMetadata(response.headers.map(h => h.name -> h.value)))
          case StatusCodes.NotFound => None
          case _ => exceptionFor(response)
        }
    }
  }

  override def getObject(bucket: BucketName, key: Key, range: Option[ByteRange], conditions: Option[GetObjectCondition]): Future[Either[NoObjectReason, S3Object]] = {
    val uri = bucketBaseUri(bucket).withPath(path(key))
    val request = HttpRequest(GET, uri,
      List(
        Some[HttpHeader](HttpHeaders.Host(uri.authority.host.address)),
        range.map(HttpHeaders.Range(_)),
        conditions.map(conditionToHeader)
      ).flatten: List[HttpHeader]
    )
    sendBucketRequest(bucket, signedRequest(request)).map { response =>
      response.status match {
        case StatusCodes.OK =>
          Right(S3Object(
            S3ObjectMetadata(response.headers.map(h => h.name -> h.value)),
            // TODO think this through carefully - can we leverage FileBytes for example?
            response.entity.data.toByteString
          ))

        case StatusCodes.NotFound => Left(DoesNotExist)
        case StatusCodes.NotModified => Left(NotModified)
        case StatusCodes.PreconditionFailed if conditions.exists(i => i.isInstanceOf[IfMatch] || i.isInstanceOf[IfNotModifiedSince]) => Left(NotModified)
        case _ => exceptionFor(response)
      }
    }
  }


  override def createObject(bucket: BucketName, key: Key, data: ByteString, config: CreateObjectConfig): Future[Unit] = {
    val uri = bucketBaseUri(bucket).withPath(path(key))

    val headers: List[HttpHeader] =
      List(
        Some[HttpHeader](HttpHeaders.Host(uri.authority.host.address)),
        config.cacheControl.map(HttpHeaders.`Cache-Control`(_)),
        /* config.cannedAcl.fold(
           canned -> None //Some(RawHeader("x-amz-acl" -> canned.name)),
           permissions -> None
         */
        config.contentDisposition.map(HttpHeaders.`Content-Disposition`(_)),
        config.contentType.map(t => HttpHeaders.`Content-Type`(ContentType(MediaType.custom(t)))),
        Some(RawHeader("x-amz-storage-class", config.storageClass.name))
      ).flatten ++ config.customMetadata.map(t => RawHeader(t._1.name, t._2))

    val request = HttpRequest(PUT, uri,
      headers,
      HttpData(data)
    )

    // TOOD body md5 somehow
    // MD5.hash(data)

    sendBucketRequest(bucket, signedRequest(request)).map { response => () }
  }



  // helpers


  private def conditionToHeader(condition: GetObjectCondition): HttpHeader = condition match {
    case IfMatch(ETag(tag)) => HttpHeaders.`If-Match`(EntityTag(tag))
    case IfNotMatch(ETag(tag)) => HttpHeaders.`If-None-Match`(EntityTag(tag))
    case IfModifiedSince(date) => HttpHeaders.`If-Modified-Since`(DateTime(date.getTime))
    case IfNotModifiedSince(date) => HttpHeaders.`If-Unmodified-Since`(DateTime(date.getTime))
  }

  private def path(key: Key): Uri.Path = Uri.Path("/" + key.name)

  private def handleResponse[A](onOk: Elem => Try[A])(response: HttpResponse): A =
    if (response.status == StatusCodes.OK) onOk(XML.loadString(response.entity.asString)).get
    else throw exceptionFor(response)

  private def exceptionFor(response: HttpResponse): Nothing =
    throw new RuntimeException(s"Failed request, status: ${response.status}, body: ${response.entity.asString}")


  private def signedRequest(method: HttpMethod, uri: Uri): HttpRequest =
    signedRequest(HttpRequest(method, uri, List(HttpHeaders.Host(uri.authority.host.address))))


  private def signedRequest(request: HttpRequest) = Authentication.signWithHeader(request, region, service, credentials)

  private def sendRequest(request: HttpRequest): Future[HttpResponse] =
    baseConnectorInfo.flatMap(_.hostConnector ? request).mapTo[HttpResponse]

  private def sendBucketRequest(bucket: BucketName, request: HttpRequest): Future[HttpResponse] =
    bucketConnectorInfo(bucket).flatMap(_.hostConnector ? request).mapTo[HttpResponse]


  // host connectors - spray will manage a pool of instances for each host for us
  // TODO maybe tweak the size of those pools somehow?
  private def baseConnectorInfo: Future[Http.HostConnectorInfo]  =
    (IO(Http) ? Http.HostConnectorSetup("s3.amazonaws.com", port)).mapTo[Http.HostConnectorInfo]

  private def bucketConnectorInfo(bucket: BucketName): Future[Http.HostConnectorInfo] =
    (IO(Http) ? Http.HostConnectorSetup(s"${bucket.name}.s3.amazonaws.com", port))
      .mapTo[Http.HostConnectorInfo]

}
