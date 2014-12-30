package awsync.s3

import java.util.Date
import spray.httpx.marshalling.Marshaller

import scala.collection.immutable.Seq
import scala.xml.{Elem, XML}
import scala.concurrent.Future
import scala.util.Try
import akka.actor.ActorSystem
import akka.util.ByteString
import spray.http.HttpHeaders.RawHeader
import spray.http._
import awsync.{Regions, Region, Credentials}

private[s3] object ConcreteS3Client {

  def apply(credentials: Credentials, region: Region, https: Boolean = true)(implicit system: ActorSystem): S3Client =
    new ConcreteS3Client(credentials, region, https)(system)

  private def conditionToHeader(condition: GetObjectCondition): HttpHeader = condition match {
    case IfMatch(ETag(tag)) => HttpHeaders.`If-Match`(EntityTag(tag))
    case IfNotMatch(ETag(tag)) => HttpHeaders.`If-None-Match`(EntityTag(tag))
    case IfModifiedSince(date) => HttpHeaders.`If-Modified-Since`(DateTime(date.getTime))
    case IfNotModifiedSince(date) => HttpHeaders.`If-Unmodified-Since`(DateTime(date.getTime))
  }

  private def path(key: Key): Uri.Path = Uri.Path("/" + key.name)

  private def handleResponse[A](operation: String)(onOk: Elem => Try[A])(response: HttpResponse): A =
    if (response.status == StatusCodes.OK) onOk(XML.loadString(response.entity.asString)).get
    else throw exceptionFor(response, operation)

  private def exceptionFor(response: HttpResponse, operation: String): Nothing =
    throw new RuntimeException(s"Failed request $operation, status: ${response.status}, body: ${response.entity.asString}")




}

private[s3] final class ConcreteS3Client(
    credentials: Credentials,
    region: Region,
    val https: Boolean
  )(
    implicit val system: ActorSystem
  ) extends S3Connections with S3Client {

  import HttpMethods._
  import RequestUtils._
  import ConcreteS3Client._

  // api implementation

  override def listBuckets: Future[Seq[(BucketName, Date)]] =
    sendRequest(signedRequest(GET, baseUri, Regions.USEast, credentials))
      .map(handleResponse("listing buckets")(xml.ListBuckets.fromXml))

  override def listObjects(bucket: BucketName, config: ListObjectsConfig): Future[(ListObjectsInfo, Seq[KeyDetails])] = {
    val parameters = Seq(
      config.delimiter.map("delimiter" -> _),
      config.marker.map("marker" -> _),
      config.maxKeys.map("maxKeys" -> _.toString),
      config.prefix.map("prefix" -> _)
    )
    val uri = bucketBaseUri(bucket).withQuery(parameters.flatten: _*)
    sendBucketRequest(bucket, signedRequest(GET, uri, region, credentials))
      .map(handleResponse("listing objects")(xml.ListObjects.fromXml))
  }

  override def canAccess(bucket: BucketName): Future[Option[NoAccessReason]] =
    sendBucketRequest(bucket, signedRequest(HEAD, bucketBaseUri(bucket), region, credentials))
      .map { response =>
        response.status match {
          case StatusCodes.OK => None
          case StatusCodes.NotFound => Some(DoesNotExist)
          case StatusCodes.Forbidden => Some(PermissionDenied)
          case _ => exceptionFor(response, s"checking if we have access to bucket $bucket")
        }
      }

  override def getObjectMetadata(bucket: BucketName, key: Key): Future[Option[S3ObjectMetadata]] = {
    val request = signedRequest(HEAD, bucketBaseUri(bucket).withPath(path(key)), region, credentials)
    sendBucketRequest(bucket, request)
      .map { response =>
        response.status match {
          case StatusCodes.OK => Some(S3ObjectMetadata(response.headers.map(h => h.name -> h.value)))
          case StatusCodes.NotFound => None
          case _ => exceptionFor(response, s"fetching metadata for $key")
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
    sendBucketRequest(bucket, signedRequest(request, region, credentials)).map { response =>
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
        case _ => exceptionFor(response, "fetching object")
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

    sendBucketRequest(bucket, signedRequest(request, region, credentials)).map { response => () }
  }


  override def deleteObject(bucket: BucketName, key: Key): Future[Unit] = {
    val request = signedRequest(DELETE, bucketBaseUri(bucket).withPath(path(key)), region, credentials)
    sendBucketRequest(bucket, request)
      .map { response =>
      response.status match {
        case StatusCodes.NoContent => Unit
        case StatusCodes.NotFound => throw new RuntimeException(s"Cannot delete key $bucket $key because it does not exist")
        case _ => exceptionFor(response, "deleting object")
      }
    }

  }


  override def deleteObjects(bucket: BucketName, keys: Seq[Key]): Future[Unit] = {
    val body = spray.httpx.marshalling.marshalUnsafe(xml.DeleteKeys.toXml(keys))

    val request = signedRequest(POST, bucketBaseUri(bucket).withQuery("delete"), body, region, credentials)

    sendBucketRequest(bucket, request)
      .map { response =>
        response.status match {
          case StatusCodes.OK =>
            // TODO parse response and throw error with the failed deletes if there were any
            Unit
          case _ => exceptionFor(response, "deleting multiple object")
        }
      }
  }

}