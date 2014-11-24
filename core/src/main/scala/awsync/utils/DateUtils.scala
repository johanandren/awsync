package awsync.utils

import java.text.SimpleDateFormat
import java.util.{Date, Locale, TimeZone}

private[awsync] object DateUtils {

  /** @return A date string suitable for the http Date header */
  def toHttpDateFormat(date: Date): String = {
    val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
    dateFormat.format(date)
  }

  def toIso8601DateTimeFormat(date: Date): String = {
    val timestampFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'")
    timestampFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
    timestampFormat.format(date)
  }

  def toIso8601DateFormat(date: Date): String = {
    val dateFormat = new SimpleDateFormat("yyyyMMdd")
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
    dateFormat.format(date)
  }
}
