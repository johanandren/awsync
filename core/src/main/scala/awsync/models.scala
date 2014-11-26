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

/**
 * A geographic amazon location (where the services are hosted)
 * @param location Human readable representation
 * @param name Unique name of the region
 */
case class Region(location: String, name: String)

object Regions {
  val USEast = Region("US East (N. Virginia)", "us-east-1")
  val USWest1 = Region("US West (N. California)", "us-west-1")
  val USWest2 = Region("US West (Oregon)", "us-west-2")
  val EUWest = Region("EU (Ireland)", "eu-west-1")
  val EUCentral = Region("EU (Frankfurt)", "eu-central-1")
  val AsiaPacificSouthEast1 = Region("Asia Pacific (Singapore)", "ap-southeast-1")
  val AsiaPacificSouthEast2 = Region("Asia Pacific (Sydney)", "ap-southeast-2")
  val AsiaPacificNorthEast = Region("Asia Pacific (Tokyo)", "ap-northeast-1")
  val SouthAmerica = Region("South America (Sao Paulo)", "sa-east-1")
}

/**
 * @param name Unique service name, for example "iam", "s3"
 */
case class Service(name: String)