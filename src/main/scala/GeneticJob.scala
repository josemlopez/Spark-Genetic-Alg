import GeneticAlgorithm.GA._
import GeneticAlgorithm._
import domain.generateIndividualBoolean._
import domain.{FitnessKnapsackProblem, Individual}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{DenseVector, Vectors}
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

/**
*  Class Created by jmlopez on 01/01/16.
*/
object GeneticJob{

  // case class GAStat (generation: Int, averageFit: Double, totalFit: Double)

  def main(args: Array[String]) {
    var argv = args
    if (args.length == 0) {
      argv = Array("default_config")
    }
    val setup = AppConfig(argv(0))
    // Spark Config
    val sparkConf = new SparkConf().setAppName("Genetic-Spark").setMaster("local[*]")
    //val sparkConf = new SparkConf()
    val sc = new SparkContext(sparkConf)

    // Problem definition
    val values: Broadcast[DenseVector] = sc.broadcast(Vectors.dense(Array.fill(setup.chromSize)(math.random*100)).toDense)
    val weights =  sc.broadcast(Vectors.dense(Array.fill(setup.chromSize)(math.random*10)).toDense)
    val maxW = setup.maxW
    val fitnessKSP = new FitnessKnapsackProblem(values, weights, maxW)
    val crhmSize = values.value.size-1
    // GA definition
    val sizePopulation = setup.worldSize
    val selectionPer = setup.selectionPercentage
    val mutationProb = setup.mutProb
    val numGenerations = setup.numGenerations
    val selections: Selector[SelectionFunction] = new Selector(Seq(new SelectionNaive, new SelectionRandom, new SelectionWrong))
    val mutations: Selector[MutationFunction] = new Selector(Seq(new OnePointMutation, new OnePointMutation, new NoMutation))

    // Creation Random Population
    val populationRDD: RDD[Individual[Boolean]] = sc.parallelize(initialPopulationBoolean(crhmSize, sizePopulation), setup.numPopulations).
      map(ind => ind(fitnessKSP.fitnessFunction))
    println("----------Running---------")

    val result = selectAndCrossAndMutatePopulation(
      populationRDD,
      selectionPer,
      mutationProb,
      fitnessKSP,
      values,
      weights,
      maxW,
      numGenerations,
      selections,
      mutations)

    //val totalFitness: Option[Double] = result._1.map(indv => indv.fitnessScore).reduce((acc, curr) => if (curr.get > 0) { Some(acc.get + curr.get)} else acc)

    println("Final results: "+result._2.map(ind => (ind.indexPop, ind.fitnessScore.get)).collect().mkString(";"))
    sc.stop()
  }
}


