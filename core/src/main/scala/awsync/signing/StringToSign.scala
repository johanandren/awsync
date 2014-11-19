package awsync.signing

import java.util.Date

import awsync.{Service, Region}

object StringToSign {

  private val algorithm = "AWS4-HMAC-SHA256"

  // TODO timezone?
  def create(requestDate: Date, region: Region, service: Service, canonicalRequest: CanonicalRequest): String = {
    val timeStampString = DateFormats.formatDateTime(requestDate)
    val dateString = DateFormats.formatDate(requestDate)
    val credentialScopeValue = s"$dateString/${region.name}/${service.name}/aws4_request"
    s"$algorithm\n$timeStampString\n$credentialScopeValue\n${canonicalRequest.hash}"
  }
}
