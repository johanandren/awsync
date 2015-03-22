# Awsync 
[![Build Status](https://travis-ci.org/johanandren/awsync.svg?branch=master)](https://travis-ci.org/johanandren/awsync)

An async client for amazon web services


**Current status**: a bit on hold waiting for akka-http so that the library can provide streaming of data to and from s3

Most "async" clients for aws for Scala simply wraps the blocking amazon Java API:s on a separate threadpool, or not even that.
This project is meant to solve that by providing a core package which makes it easy to talk to amazon 
services using Spray (and in the future Akka HTTP) with no blocking whatsoever.

## Requirements
Preliminary
* Built with Scala 2.11 and Akka 2.3.x

## Sample usage

```scala
implicit val system = ActorSystem("test")

val creds = Credentials("aws-key", "aws-secret")

val client = S3Client(creds, Regions.USEast)
client.listBuckets.foreach { buckets: Seq[Bucket] =>
  println("My buckets: " + buckets)
}
```
