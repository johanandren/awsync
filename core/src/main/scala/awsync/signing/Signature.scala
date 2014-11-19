package awsync.signing

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import awsync.{Service, Region, AwsSecret}

object Signature {

  // TODO how to deal with date/timezone?
  def create(secret: AwsSecret, dateStamp: String, region: Region, service: Service) = {
    val secretKey = ("AWS4" + secret.secret).getBytes("UTF-8")
    val dateKey = hmacSHA256(dateStamp, secretKey)
    val regionKey = hmacSHA256(region.name, dateKey)
    val serviceKey = hmacSHA256(service.name, regionKey)
    val hash = hmacSHA256("aws4_request", serviceKey)

    hash
  }

  private val algorithm = "HmacSHA256"
  private def hmacSHA256(data: String, key: Array[Byte]): Array[Byte] = {
    val mac = Mac.getInstance(algorithm)
    mac.init(new SecretKeySpec(key, algorithm))
    mac.doFinal(data.getBytes("UTF8"))
  }
}
