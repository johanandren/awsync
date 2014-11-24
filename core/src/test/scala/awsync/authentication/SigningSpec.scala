package awsync.authentication

import java.util.{TimeZone, Calendar}

import akka.util.ByteString
import awsync.{Credentials, Service, Regions, AbstractSpec}
import spray.http.HttpHeaders.{Host, RawHeader}
import spray.http._

import scala.collection.immutable.Seq

class SigningSpec extends AbstractSpec {

  describe("The canonical request creator") {

    import awsync.authentication.CanonicalRequest._
    it("uses / for empty path") {
      encodePath(Uri.Path("")) should be ("/")
    }

    it("uses the path as canonical path") {
      encodePath(Uri.Path("/foo/bar")) should be ("/foo/bar")
    }

    it("url encodes non-ascii path parts") {
      encodePath(Uri.Path("/f o/bär")) should be ("/f%20o/b%C3%A4r")
    }

    it("creates an empty string for no query parameters") {
      encodeParameters(Uri.Query()) should be ("")
    }

    it("encodes query parameters correctly") {
      val result = encodeParameters(Uri.Query(
        "UserName" -> "NewUser",
        "Action" -> "CreateUser",
        "Version" -> "2010-05-08"))

      result should be ("Action=CreateUser&UserName=NewUser&Version=2010-05-08")
    }

    it("url encodes query parameter names and values") {
      encodeParameters(Uri.Query("Bö" -> "Bä")) should be ("B%f6=B%e4")
    }

    it("sorts query parameter on asci name") {
      encodeParameters(Uri.Query("A" -> "1", "a" -> "2")) should be ("A=1&a=2")
    }

    it("sorts and encodes query headers") {
      val result = encodeHeaders(Seq(
        Host("www.example.com"),
        RawHeader("ablab", "agrajag")
      ))
      result should be ("ablab:agrajag\nhost:www.example.com\n")
    }

    it("trims double space from header values") {
      val result = encodeHeaders(Seq(
        RawHeader("ablab", "  agrajag  ")
      ))
      result should be ("ablab: agrajag \n")
    }

    it("creates a complete and correct canonical request") {
      val request = HttpRequest(
        HttpMethods.POST,
        Uri("http://iam.amazonaws.com"),
        List[HttpHeader](
          RawHeader("content-type", "application/x-www-form-urlencoded; charset=utf-8"),
          RawHeader("host", "iam.amazonaws.com"),
          RawHeader("x-amz-date", "20110909T233600Z")
        )
      ).withEntity(HttpEntity(ContentTypes.`text/plain`, HttpData(ByteString("Action=ListUsers&Version=2010-05-08"))))
      val (result, signedHeaders) = canonicalRequest(request, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")

      val expected = """
        |POST
        |/
        |
        |content-type:application/x-www-form-urlencoded; charset=utf-8
        |host:iam.amazonaws.com
        |x-amz-date:20110909T233600Z
        |
        |content-type;host;x-amz-date
        |e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
      """.stripMargin.trim

      result should be (expected)
      signedHeaders should be ("content-type;host;x-amz-date")
    }


    it("generates the expected hash string") {
      val result = Sha256.createHash("Action=ListUsers&Version=2010-05-08")
      result should be("b6359072c78d70ebee1e81adcbab4f01bf2c23245fa365ef83fe8f1f955085e2")
    }

  }

  describe("String to sign creator") {

    it("generates the expected string to sign") {

      val result = StringToSign.create(
        "20110909T233600Z",
        Regions.USEast,
        Service("iam"),
        "3511de7e95d28ecd39e9513b642aee07e54f4941150d8df8bf94b328ef7e55e2")

      result should be ("AWS4-HMAC-SHA256\n20110909T233600Z\n20110909/us-east-1/iam/aws4_request\n3511de7e95d28ecd39e9513b642aee07e54f4941150d8df8bf94b328ef7e55e2")

    }
  }

  describe("Request signing") {

    it("signs a request correctly") {
      val request = HttpRequest(
        HttpMethods.POST,
        Uri("http://iam.amazonaws.com"),
        List[HttpHeader](
          RawHeader("content-type", "application/x-www-form-urlencoded; charset=utf-8"),
          RawHeader("host", "iam.amazonaws.com")
        )
      ).withEntity(HttpEntity(ContentTypes.`text/plain`, HttpData(ByteString("Action=ListUsers&Version=2010-05-08"))))

      val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
      cal.set(2011, 8, 9, 23, 36, 0)
      val date = cal.getTime

      val result = Authentication.signWithHeader(request, Regions.USEast, Service("iam"), Credentials("key", "secret"), date)

      val maybeAuth = result.headers.find(_.is("authorization"))
      maybeAuth should be ('defined)
      val header: String = maybeAuth.get.value

      header should startWith ("AWS4-HMAC-SHA256 Credential=key/20110909/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-content-sha256;x-amz-date, Signature=")

    }

  }

}