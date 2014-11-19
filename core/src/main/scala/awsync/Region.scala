package awsync

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
