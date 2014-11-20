package awsync.signing

import awsync.{Service, Region}

private[signing] object StringToSign {

  private val algorithm = "AWS4-HMAC-SHA256"

  /**
   * @param timestampString A date in the form "yyyyMMddTHHmmssZ"
   */
  def create(timestampString: String, region: Region, service: Service, canonicalRequestHash: String): String = {
    val dateString = timestampString.take(8)
    val credentialScopeValue = s"$dateString/${region.name}/${service.name}/aws4_request"
    s"$algorithm\n$timestampString\n$credentialScopeValue\n${canonicalRequestHash}"
  }
}
