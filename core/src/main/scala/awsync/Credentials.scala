package awsync

/** account key/identifier */
case class AwsKey(key: String)

/** account secret/password */
final class AwsSecret(val secret: String) {
  // make sure the secret does not go in logs/printlns etc. by accident
  override def toString = "AwsSecret(****)"
}
object AwsSecret {
  def apply(secret: String): AwsSecret = new AwsSecret(secret)
}

/** the pair of key and secret used to access services */
case class Credentials(key: AwsKey, secret: AwsSecret)

object Credentials {
  def apply(key: String, secret: String): Credentials = new Credentials(AwsKey(key), AwsSecret(secret))
}