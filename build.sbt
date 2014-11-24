scalaVersion in Global := "2.11.4"

val akkaVersion = "2.3.7"

val sprayVersion = "1.3.1"

lazy val core = project
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "io.spray" %% "spray-http" % sprayVersion,
      "io.spray" %% "spray-client" % sprayVersion,
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    )
  )

lazy val s3 = project
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "io.spray" %% "spray-client" % sprayVersion,
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    )
  )
