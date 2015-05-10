scalaVersion in Global := "2.11.5"
scalacOptions in Global ++= Seq("-deprecation", "-feature", "-unchecked")

val versions = new {
  val akka = "2.3.7"
  val akkaStreams = "1.0-RC2"
  val akkaHttp = akkaStreams
  val scalaTest = "2.2.1"
}

lazy val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % versions.akka,
  "com.typesafe.akka" %% "akka-http-core-experimental" % versions.akkaHttp,
  "com.typesafe.akka" %% "akka-http-scala-experimental" % versions.akkaHttp,
  "com.typesafe.akka" %% "akka-stream-experimental" % versions.akkaStreams
)

lazy val core = project
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % versions.scalaTest % "test"
    ) ++ akkaDeps
  )

lazy val s3 = project
  .dependsOn(core)
  .configs(IntegrationTest)
  .settings(Defaults.itSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.4",
      "org.scalatest" %% "scalatest" % versions.scalaTest % "it,test",
      "com.typesafe" % "config" % "1.2.1" % "test"
    ) ++ akkaDeps ,
    parallelExecution in IntegrationTest := false
  )
