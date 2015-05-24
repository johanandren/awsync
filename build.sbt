scalaVersion in ThisBuild := "2.11.6"
scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-unchecked")

val versions = new {
  val akka = "2.3.7"
  val akkaStreams = "1.0-RC3"
  val akkaHttp = akkaStreams
  val scalaTest = "2.2.1"
}

lazy val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % versions.akka,
  "com.typesafe.akka" %% "akka-http-core-experimental" % versions.akkaHttp,
  "com.typesafe.akka" %% "akka-http-experimental" % versions.akkaHttp,
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
