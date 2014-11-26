package awsync.s3

import java.util.Date

/**
 * @param delimiter A delimiter is a character you use to group keys.
 * @param marker specifies the key to start with when listing objects in a bucket. Amazon S3 returns object
 *               keys in alphabetical order, starting with key after the marker in order.
 * @param maxKeys Sets the maximum number of keys returned in the response body. You can add this to your request if
 *                you want to retrieve fewer than the default 1000 keys.
 * @param prefix Limits the response to keys that begin with the specified prefix. You can use prefixes to separate
 *               a bucket into different groupings of keys. (You can think of using prefix to make groups in the
 *               same way you'd use a folder in a file system.)
 */
case class ListObjectsConfig(delimiter: Option[String], marker: Option[String] , maxKeys: Option[Int], prefix: Option[String])


sealed trait ETagMatch
case class IfMatch(tag: ETag) extends ETagMatch
case class IfNoneMatch(tag: ETag) extends ETagMatch
case class GetObjectConfig(ifModifiedSince: Option[Date], ifUnmodifiedSince: Option[Date], etagMatch: Option[ETagMatch])