package awsync.signing

import java.util.{TimeZone, Calendar, Date}

import akka.http.model.HttpEntity.Strict
import akka.http.model._
import akka.http.model.headers.{Host, RawHeader}
import akka.util.ByteString
import awsync.{Credentials, Service, Regions, AbstractSpec}

import scala.collection.immutable.Seq

class SigningSpec extends AbstractSpec {

  describe("The canonical request creator") {

    import awsync.signing.CanonicalRequest._
    it("uses / for empty path") {
      encodePath("") should be ("/")
    }

    it("uses the path as canonical path") {
      encodePath("/foo/bar") should be ("/foo/bar")
    }

    it("url encodes non-ascii path parts") {
      encodePath("/f o/bär") should be ("/f%20o/b%e4r")
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

    it("creates a complete canonical request") {
      val request = HttpRequest(
        HttpMethods.POST,
        Uri("http://iam.amazonaws.com"),
        Seq[HttpHeader](
          RawHeader("content-type", "application/x-www-form-urlencoded; charset=utf-8"),
          RawHeader("host", "iam.amazonaws.com"),
          RawHeader("x-amz-date", "20110909T233600Z")
        )
      ).withEntity(Strict(ContentTypes.`text/plain`, ByteString("Action=ListUsers&Version=2010-05-08")))
      val result = create(request)

      val expected = """
        |POST
        |/
        |
        |content-type:application/x-www-form-urlencoded; charset=utf-8
        |host:iam.amazonaws.com
        |x-amz-date:20110909T233600Z
        |
        |content-type;host;x-amz-date
        |b6359072c78d70ebee1e81adcbab4f01bf2c23245fa365ef83fe8f1f955085e2
      """.stripMargin.trim

      result.value should be (expected)
    }


    it("generates the expected hash string") {
      val result = createHash("Action=ListUsers&Version=2010-05-08")
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

    it("signs a request") {
      val request = HttpRequest(
        HttpMethods.POST,
        Uri("http://iam.amazonaws.com"),
        Seq[HttpHeader](
          RawHeader("content-type", "application/x-www-form-urlencoded; charset=utf-8"),
          RawHeader("host", "iam.amazonaws.com")
        )
      ).withEntity(Strict(ContentTypes.`text/plain`, ByteString("Action=ListUsers&Version=2010-05-08")))

      val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
      cal.set(2011, 8, 9, 23, 36, 0)
      val date = cal.getTime

      val result = Sign(request, Regions.USEast, Service("iam"), Credentials("key", "secret"), date)

      val maybeAuth = result.getHeader("Authorization")
      maybeAuth should be ('defined)
      val header: String = maybeAuth.get.value

      header should startWith ("AWS4-HMAC-SHA256 Credential=key/20110909/us-east-1/iam/aws4_request, SignedHeaders=content-type;host;x-amz-date, Signature=")

    }

  }

}