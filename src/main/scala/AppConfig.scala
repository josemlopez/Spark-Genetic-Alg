import scala.io.Source

/**
  * GraphInsight: Class Created by jmlopez on 08/04/16.
  */

case class AppConfig(properties: Map[String, String]) extends java.io.Serializable {
  val numGenerations = properties.getOrElse("numGenerations", "1000").toInt
  val mutProb: Float = properties.getOrElse("mutProb", "0.01").toFloat
  val selectionPercentage = properties.getOrElse("selectionPercentage", "0.5").toFloat
  val numPopulations = properties.getOrElse("numPopulations", "8").toInt // Use a value >= numCores in your cluster
  val sizePop = properties.getOrElse("sizePop", "1000").toInt
  val worldSize = numPopulations * sizePop
  val chromSize = properties.getOrElse("chromSize", "300").toInt
  val maxW = properties.getOrElse("maxWeight_problem", "157").toInt
}

/**
  *  This object will help to read a properties file
  */
object AppConfig {

  def apply(file: String): AppConfig = {
    new AppConfig(AppConfig.readProperties(file))
  }

  /**
    * Returns all the properties contained in a given file
    * @param file
    * @return
    */
  def readProperties(file: String): Map[String, String] = {
    var map: Map[String, String] = Map()
    val lines = Source.fromFile(file).getLines()
    for (l <- lines if l.length > 0 && !l.startsWith("#") && l.contains("="))
      map += (l.split("=")(0) -> l.split("=")(1))
    map
  }
}
