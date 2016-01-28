import GeneticAlgorithm.GA._
import domain.Individual
import domain.fitness._
import domain.generateIndividualBoolean._
import org.apache.spark.SparkContext
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{DenseVector, Vectors}
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._


import java.io.StringWriter
import au.com.bytecode.opencsv.CSVWriter
import scala.collection.JavaConversions._
import java.io.FileWriter
import java.io.BufferedWriter

/**
 * Created by jmlopez on 01/01/16.
 */
object GeneticJob{

  case class GAStat (generation: Int, averageFit: Double, totalFit: Double)

  def merge(srcPath: String, dstPath: String): Unit =  {
    val hadoopConfig = new Configuration()
    val hdfs = FileSystem.get(hadoopConfig)
    FileUtil.copyMerge(hdfs, new Path(srcPath), hdfs, new Path(dstPath), false, hadoopConfig, null)
  }

  def main(args: Array[String]) {
    // val conf = new SparkConf ().setAppName ("Genetic Application")
    val sc = new SparkContext ()

    /* var rdd = sc.parallelize(List(1,2,3,4,5,6,7,8),2)
    for (i <-1 to 10){
      rdd = rdd.map(x => x+1)
    }
    rdd.foreach(println)
    */

    // This will go to a .ini file or will be passed like argument
    // Quick & Dirty testing purposes
    // Broadcasting the values and weights to the workers. This vector can be huge "in normal use"
    val values: Broadcast[DenseVector] = sc.broadcast(Vectors.dense(20.5, 10.6, 10.2, 1 , 94, 2, 1, 3, 7, 91, 64, 4).toDense)
    val weights =  sc.broadcast(Vectors.dense(10.2, 76,  2, 18, 10.87, 5, 2, 1, 3, 1, 76, 198).toDense)
    val maxW = 67
    if (values.value.size != weights.value.size) {
      sys.exit(-1)
    }
    val crhmSize = values.value.size-1

    val sizePopulation = 10
    var populationRDD = sc.parallelize(initialPopulationBoolean(crhmSize, sizePopulation), 3).
      map(ind => (fitnessKnapsackProblem(ind, values, weights, maxW), ind))


    println("----------initial population and Fitness-------")
    populationRDD.foreach(println)


    var statisticsList: List[Array[String]] = List()
    val out = new BufferedWriter(new FileWriter("stat.csv"))
    val writer = new CSVWriter(out)


    val numGenerations = 300
    for (i<-0 to numGenerations) {
      val populationSize = populationRDD.count()
      val partitions = populationRDD.partitions.size
      println(s"----------Generation $i with populationSize $populationSize and numPartitions $partitions -------")

      populationRDD = selectAndCrossAndMutatePopulation (
        populationRDD,
        0.5,
        sizePopulation,
        0.01f,
        fitnessKnapsackProblem,
        values,
        weights,
        maxW)
      //populationFitness.foreach(println)
      val totalFitness: Double = populationRDD.map (indv => indv._1).reduce ((acc, curr) => if (curr > 0) {acc + curr} else acc)
      val bestIndv = populationRDD.takeOrdered(1)(Ordering[Double].reverse.on(x => x._1))(0)

      statisticsList = Array(i.toString,
        (totalFitness/populationSize).toString,
        totalFitness.toString,
        bestIndv._2.toString(),
        bestIndv._1.toString
      )::statisticsList
    }

    writer.writeAll(statisticsList)
    out.close()
    populationRDD.takeOrdered(1)(Ordering[Double].reverse.on(x => x._1)).
      foreach(x => println("This is the solution: " + x.toString()))
  }
}


