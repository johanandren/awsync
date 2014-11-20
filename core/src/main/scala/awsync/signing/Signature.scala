package awsync.signing

import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import awsync.{Service, Region, AwsSecret}

private[signing] object Signature {

  def create(secret: AwsSecret, date: Date, region: Region, service: Service, stringToSign: String): String = {
    val secretKey = ("AWS4" + secret.secret).getBytes("UTF-8")
    val dateStamp = DateFormats.formatDate(date)
    val dateKey = hmacSHA256(dateStamp, secretKey)
    val regionKey = hmacSHA256(region.name, dateKey)
    val serviceKey = hmacSHA256(service.name, regionKey)
    val signingKey = hmacSHA256("aws4_request", serviceKey)
    Utils.hexEncode(hmacSHA256(stringToSign, signingKey))
  }

  private val algorithm = "HmacSHA256"
  private def hmacSHA256(data: String, key: Array[Byte]): Array[Byte] = {
    val mac = Mac.getInstance(algorithm)
    mac.init(new SecretKeySpec(key, algorithm))
    mac.doFinal(data.getBytes("UTF8"))
  }
}
