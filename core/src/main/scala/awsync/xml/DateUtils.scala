package awsync.xml

import java.text.SimpleDateFormat
import java.util.Date

import scala.util.Try

object DateUtils {

  def parseTimestamp(text: String): Try[Date] = {
    val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    Try(format.parse(text))
  }
}
