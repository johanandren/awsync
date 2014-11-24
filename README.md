# Awsync
An async client for amazon web services

Most "async" clients for aws for Scala simply wraps the blocking amazon Java API:s on a separate threadpool, or not even that.
This project is meant to solve that by providing a core package which makes it easy to talk to amazon 
services using Spray (and in the future Akka HTTP) with no blocking whatsoever.

## Requirements
Preliminary
* Built with Scala 2.11 and Akka 2.3.x

## Sample usage

```scala
implicit val system = ActorSystem("test")
import system.dispatcher

val creds = Credentials("aws-key", "aws-secret")

val client = Await.result(S3Client(creds, Regions.USEast), 2.seconds)
client.listBuckets.foreach { buckets: Seq[Bucket] =>
  println("My buckets: " + buckets)
}
```