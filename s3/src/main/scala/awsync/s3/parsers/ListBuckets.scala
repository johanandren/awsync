package awsync.s3.parsers

import java.util.Date

import awsync.s3.Bucket
import scala.collection.immutable.Seq
import scala.util.{Success, Try}
import scala.xml.Elem
import awsync.xml.DateUtils.parseTimestamp
import awsync.utils.Functional.sequence

object ListBuckets {

  def parse(xml: Elem): Try[Seq[(Bucket, Date)]] =
    sequence((xml \\ "Bucket").map { bucket =>
      for {
        name <- Success((bucket \ "Name").text)
        timestamp <- parseTimestamp((bucket \ "CreationDate").text)
      } yield (Bucket(name), timestamp)
    })

}
