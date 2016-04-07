import GeneticAlgorithm.GA._
import domain.FitnessKnapsackProblem
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

    val sc = new SparkContext ()

    // This will go to a .ini file or will be passed like argument
    // Quick & Dirty testing purposes
    // Broadcasting the values and weights to the workers. This vector can be huge "in normal use"
    val values: Broadcast[DenseVector] = sc.broadcast(Vectors.dense(Array.fill(300)(math.random*100)).toDense)
    val weights =  sc.broadcast(Vectors.dense(Array.fill(300)(math.random*10)).toDense)
    val maxW = 157
    val selectionPer = 0.5
    val mutationProb = 0.01f
    if (values.value.size != weights.value.size) {
      sys.exit(-1)
    }
    val crhmSize = values.value.size-1

    val sizePopulation = 10000
    val fitnessKSP = new FitnessKnapsackProblem(values, weights, maxW)
    var populationRDD = sc.parallelize(initialPopulationBoolean(crhmSize, sizePopulation), 3).
      map(ind => ind(fitnessKSP.fitnessFunction))


    println("----------initial population and Fitness-------")
    populationRDD.foreach(println)


    var statisticsList: List[Array[String]] = List()
    val out = new BufferedWriter(new FileWriter("stat.csv"))
    val writer = new CSVWriter(out)

    val outT = new BufferedWriter(new FileWriter("totalStat.csv"))
    val writerT = new CSVWriter(outT)


    val numGenerations = 30
    for (i<-0 to numGenerations) {
      val populationSize = populationRDD.count()
      val partitions = populationRDD.partitions.size
      println(s"----------Generation $i with populationSize $populationSize and numPartitions $partitions -------")

      populationRDD = selectAndCrossAndMutatePopulation (
        populationRDD,
        selectionPer,
        sizePopulation,
        mutationProb,
        fitnessKSP,
        values,
        weights,
        maxW)
      //populationFitness.foreach(println)
      val totalFitness: Option[Double] = populationRDD.map(indv => indv.fitnessScore).reduce((acc, curr) => if (curr.get > 0) { Some(acc.get + curr.get)} else acc)
      val bestIndv = populationRDD.takeOrdered(1)(Ordering[Double].reverse.on(x => x.fitnessScore.getOrElse(Double.MaxValue)))(0)

      val actualStat = Array(i.toString,
        (totalFitness.get/populationSize).toString,
        totalFitness.toString,
        bestIndv.chromosome.toString,
        bestIndv.fitnessScore.toString
      )
      println("This is the partial solution: " + actualStat.mkString(";"))
      statisticsList = statisticsList ++ List(actualStat)

      writer.writeAll(List(actualStat))
    }

    writerT.writeAll(statisticsList)
    out.close()
    out.flush()
    outT.flush()
    outT.close()
    populationRDD.takeOrdered(1)(Ordering[Double].reverse.on(x => x.fitnessScore.get)).
      foreach(x => println("This is the solution: " + x.toString()))
  }
}


