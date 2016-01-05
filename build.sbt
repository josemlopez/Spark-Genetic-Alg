name := "GeneticAlgoritm_Spark"

version := "1.0"

scalaVersion := "2.11.4"

scalacOptions := Seq("-feature", "-unchecked", "-deprecation", "-encoding", "utf8")

resolvers += "spray repo" at "http://repo.spray.io"

val sparkVersion = "1.5.2"

libraryDependencies ++= Seq(
  "org.apache.spark"  %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark"  %% "spark-graphx" % sparkVersion % "provided",
  "org.apache.spark"  %% "spark-mllib" % sparkVersion % "provided",
  "org.apache.spark"  %% "spark-sql" % sparkVersion % "provided"
)
