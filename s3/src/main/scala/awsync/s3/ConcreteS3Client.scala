package awsync.s3

import java.util.Date

import scala.concurrent.duration._
import scala.collection.immutable.Seq
import akka.pattern.ask
import akka.actor.ActorSystem
import akka.io.IO
import akka.util.Timeout
import awsync.{Service, Region, Credentials}
import spray.can.Http
import spray.http._

import scala.concurrent.Future
import scala.xml.{Elem, XML}

private[s3] object ConcreteS3Client {

  def apply(credentials: Credentials, region: Region)(implicit system: ActorSystem): Future[S3Client] = {

    import system.dispatcher
    implicit val timeout: Timeout = 2.seconds

    (IO(Http) ? Http.HostConnectorSetup("s3.amazonaws.com", port = 80))
      .mapTo[Http.HostConnectorInfo]
      .map(info => new ConcreteS3Client(credentials, region, info, system))

  }

  private val baseUri = Uri("http://s3.amazonaws.com")
  private val service = Service("s3")

}

private[s3] final class ConcreteS3Client(credentials: Credentials, region: Region, info: Http.HostConnectorInfo, system: ActorSystem) extends S3Client {

  import ConcreteS3Client._
  import awsync.authentication.Authentication
  import HttpMethods._
  import system.dispatcher

  implicit val timeout: Timeout = 30.seconds

  override def listBuckets: Future[Seq[(Bucket, Date)]] =
    (info.hostConnector ? signedRequest(GET, baseUri, ""))
      .mapTo[HttpResponse]
      .map { handleResponse(xml => parsers.ListBuckets.parse(xml).get) }

  private def handleResponse[A](onOk: Elem => A)(response: HttpResponse): A =
    if (response.status == StatusCodes.OK) onOk(XML.loadString(response.entity.asString))
    else throw exceptionFor(response)

  private def exceptionFor(response: HttpResponse) =
    throw new RuntimeException(s"Failed request, status: ${response.status}, body: ${response.entity.asString}")

  private def signedRequest(method: HttpMethod, uri: Uri, body: String) =
    Authentication.signWithHeader(
      HttpRequest(method, uri, List(HttpHeaders.Host(uri.authority.host.address))),
      region,
      service,
      credentials
    )


}
