package awsync.signing

import akka.http.model.HttpEntity.Strict
import akka.http.model._
import akka.http.model.headers.{Host, RawHeader}
import akka.util.ByteString

import scala.collection.immutable.Seq

class CanonicalRequestSpec extends AwsyncSpec {

  describe("The canonical request") {

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
}
