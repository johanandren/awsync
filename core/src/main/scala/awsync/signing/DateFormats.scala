package awsync.signing

import java.text.SimpleDateFormat
import java.util.{TimeZone, Date}

private[signing] object DateFormats {

  def formatDateTime(date: Date): String = {
    val timestampFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
    timestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
    timestampFormat.format(date)
  }

  def formatDate(date: Date): String = {
    val dateFormat = new SimpleDateFormat("yyyyMMdd")
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
    dateFormat.format(date)
  }
}
