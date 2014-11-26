package awsync.utils

import java.text.SimpleDateFormat
import java.util.{Date, Locale, TimeZone}

import scala.util.Try

private[awsync] object DateUtils {

  private def httpDateFormat = {
    val dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"))
    dateFormat
  }

  /** @return A date string suitable for the http Date header */
  def toHttpDateFormat(date: Date): String = httpDateFormat.format(date)

  def fromHttpDateFormat(text: String): Try[Date] = Try(httpDateFormat.parse(text))

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
