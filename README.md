# Awsync 
[![Build Status](https://travis-ci.org/johanandren/awsync.svg?branch=master)](https://travis-ci.org/johanandren/awsync)

An async client for amazon web services

Most "async" clients for aws for Scala simply wraps the blocking amazon Java API:s on a separate threadpool, or not even that.
This project provides a core package which makes it easy to talk to amazon 
services asynchronously using Akka HTTP.

## Using
Not yet published to maven central. Download and `publishLocal` with sbt for now.

## Sample usage

```scala
implicit val system = ActorSystem("test")

val creds = Credentials("aws-key", "aws-secret")

val client = S3Client(creds, Regions.USEast)
client.listBuckets.foreach { buckets: Seq[Bucket] =>
  println("My buckets: " + buckets)
}
```
