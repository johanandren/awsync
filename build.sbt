scalaVersion in ThisBuild := "2.11.7"
scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-unchecked")

lazy val versions = new {
  val akkaV = "2.4.2-RC2"
  val scalaTest = "2.2.1"
}

lazy val akkaDeps = Seq(
  "com.typesafe.akka" %% "akka-actor" % versions.akkaV,
  "com.typesafe.akka" %% "akka-stream" % versions.akkaV,
  "com.typesafe.akka" %% "akka-http-core" % versions.akkaV,
  "com.typesafe.akka" %% "akka-http-experimental" % versions.akkaV
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
