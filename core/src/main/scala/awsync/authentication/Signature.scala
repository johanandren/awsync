package awsync.authentication

import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import awsync.utils.DateUtils
import awsync.{Service, Region, AwsSecret}

private[authentication] object Signature {

  import Sha256.hmacSHA256

  def create(secret: AwsSecret, date: Date, region: Region, service: Service, stringToSign: String): String = {
    val secretKey = ("AWS4" + secret.secret).getBytes("UTF-8")
    val dateStamp = DateUtils.toIso8601DateFormat(date)
    val dateKey = hmacSHA256(dateStamp, secretKey)
    val regionKey = hmacSHA256(region.name, dateKey)
    val serviceKey = hmacSHA256(service.name, regionKey)
    val signingKey = hmacSHA256("aws4_request", serviceKey)
    Utils.hexEncode(hmacSHA256(stringToSign, signingKey))
  }


}
