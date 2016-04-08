import java.io.{BufferedWriter, FileWriter}

import GeneticAlgorithm.GA._
import au.com.bytecode.opencsv.CSVWriter
import domain.generateIndividualBoolean._
import domain.{OnePointMutation, FitnessKnapsackProblem, Individual}
import domain._
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{DenseVector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

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

    var argv = args
    if (args.length == 0) {
      argv = Array("default_config")
    }

    val sparkConf = new SparkConf().setAppName("Genetic-Spark").setMaster("local[*]")
    //val sparkConf = new SparkConf()
    val sc = new SparkContext(sparkConf)

    val config = AppConfig(argv(0))

    val values: Broadcast[DenseVector] = sc.broadcast(Vectors.dense(Array.fill(config.chromSize)(math.random*100)).toDense)
    val weights =  sc.broadcast(Vectors.dense(Array.fill(config.chromSize)(math.random*10)).toDense)
    val maxW = config.maxW
    val selectionPer = config.selectionPercentage
    val mutationProb = config.mutProb
    if (values.value.size != weights.value.size) {
      sys.exit(-1)
    }
    val crhmSize = values.value.size-1

    val sizePopulation = config.worldSize
    val fitnessKSP = new FitnessKnapsackProblem(values, weights, maxW)

    val mutationF = new OnePointMutation()
    val populationRDD: RDD[Individual[Boolean]] = sc.parallelize(initialPopulationBoolean(crhmSize, sizePopulation), config.numPopulations).
      map(ind => ind(fitnessKSP.fitnessFunction))

    println("----------Running---------")

    //val out = new BufferedWriter(new FileWriter("stat.csv"))
    //val writer = new CSVWriter(out)
    val numGenerations = config.numGenerations

    val selections: Selector[SelectionFunction] = new Selector(Seq(new SelectionNaive, new SelectionRandom, new SelectionWrong))
    val mutations: Selector[MutationFunction] = new Selector(Seq(new OnePointMutation, new OnePointMutation, new NoMutation))

    val result = selectAndCrossAndMutatePopulation(
      populationRDD,
      selectionPer,
      sizePopulation,
      mutationProb,
      fitnessKSP,
      values,
      weights,
      maxW,
      numGenerations,
      mutationF,
      selections,
      mutations)

    //out.close()

    //val totalFitness: Option[Double] = result._1.map(indv => indv.fitnessScore).reduce((acc, curr) => if (curr.get > 0) { Some(acc.get + curr.get)} else acc)

    println("Resultado final: "+result._2.map(ind => (ind.population, ind.fitnessScore.get)).collect().mkString(";"))

  }
}


