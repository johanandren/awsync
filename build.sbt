scalaVersion := "2.11.4"

val akkaVersion = "2.3.7"

lazy val core = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-http-core-experimental" % "0.11",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    )
  )

lazy val s3 = project
