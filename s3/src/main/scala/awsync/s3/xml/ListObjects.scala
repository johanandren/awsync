package awsync.s3.xml

import awsync.s3._
import awsync.utils.Functional
import awsync.xml.DateUtils
import scala.collection.immutable.Seq
import scala.util.{Failure, Success, Try}
import scala.xml.Elem

private[s3] object ListObjects {

  def fromXml(xml: Elem): Try[(ListObjectsInfo, Seq[KeyDetails])] = {
    def parseInfo =
      for {
        maxKeys <- Try((xml \ "MaxKeys").text.toInt)
        truncated <- Try((xml \ "IsTruncated").text.toBoolean)
      } yield ListObjectsInfo(
        BucketName((xml \ "Name").text),
        (xml \ "Prefix").text,
        (xml \ "Marker").text,
        maxKeys,
        truncated
      )

    def parseItems = Functional.sequence(
      (xml \ "Contents").map { content =>
        for {
          modified <- DateUtils.parseTimestamp((content \ "LastModified").text)
          storageClass <- parseStorageClass((content \ "StorageClass").text)
          size <- Try((content \ "Size").text.toLong)
        } yield
          KeyDetails(
            Key((content \ "Key").text),
            modified,
            ETag((content \ "ETag").text),
            ByteSize(size),
            storageClass,
            User(
              OwnerId((content \ "ID").text),
              (content \ "DisplayName").text
            )
          )
      }
    )

    for {
      info <- parseInfo
      items <- parseItems
    } yield (info, items)
  }

  private val mapping: PartialFunction[String, StorageClass] = {
    case "STANDARD" => StorageClasses.Standard
    case "REDUCED_REDUNDANCY" => StorageClasses.ReducedRedundancy
    case "GLACIER" => StorageClasses.Glacier
  }
  private def parseStorageClass(name: String): Try[StorageClass] =
    mapping.lift(name)
      .map(Success(_))
      .getOrElse(Failure(new RuntimeException(s"Unknown storage class '$name'")))


}
