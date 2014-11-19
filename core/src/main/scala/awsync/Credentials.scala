package awsync

case class AwsKey(key: String)
case class AwsSecret(secret: String)

case class Credentials(key: AwsKey, secret: AwsSecret)

object Credentials {
  def apply(key: String, secret: String): Credentials = new Credentials(AwsKey(key), AwsSecret(secret))
}



