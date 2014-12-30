package awsync.s3.xml

import java.util.Date

import awsync.s3.BucketName
import scala.collection.immutable.Seq
import scala.util.{Success, Try}
import scala.xml.Elem
import awsync.xml.DateUtils.parseTimestamp
import awsync.utils.Functional.sequence

object ListBuckets {

  def fromXml(xml: Elem): Try[Seq[(BucketName, Date)]] =
    sequence((xml \\ "Bucket").map { bucket =>
      for {
        name <- Success((bucket \ "Name").text)
        timestamp <- parseTimestamp((bucket \ "CreationDate").text)
      } yield (BucketName(name), timestamp)
    })

}
