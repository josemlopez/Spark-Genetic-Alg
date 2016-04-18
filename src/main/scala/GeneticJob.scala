import GeneticAlgorithm.GA._
import GeneticAlgorithm._
import domain.FitnessKnapsackProblem
import domain.generateIndividualBoolean._
import org.apache.spark.mllib.linalg.Vectors
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
    val values = sc.broadcast(Vectors.dense(Array.fill(setup.chromSize)(math.random*100)).toDense)
    val weights =  sc.broadcast(Vectors.dense(Array.fill(setup.chromSize)(math.random*10)).toDense)
    val maxW = setup.maxW
    val fitnessKSP = new FitnessKnapsackProblem(values, weights, maxW)
    val crhmSize = values.value.size-1
    // GA definition
    val sizePopulation = setup.worldSize
    val selectionPer = setup.selectionPercentage
    val mutationProb = setup.mutProb
    val numGenerations = setup.numGenerations
    val selections = new Selector(Seq(
      SelectionOperators.SelectionNaive[Boolean](selectionPer) _,
      SelectionOperators.SelectionNaive[Boolean](selectionPer) _,
      SelectionOperators.SelectionNaive[Boolean](selectionPer) _
    ))
    val mutations = new Selector(Seq(
      MutationOperators.OnePointMutation(mutationProb) _,
      MutationOperators.NoMutation _,
      MutationOperators.OnePointMutation(mutationProb*20000) _
    ))

    // Creation Random Population
    val populationRDD = sc.parallelize(initialPopulationBoolean(crhmSize, sizePopulation), setup.numPopulations).
      map(ind => ind(fitnessKSP.fitnessFunction))
    println("----------Running---------")

    val result = selectAndCrossAndMutatePopulation(
      populationRDD,
      fitnessKSP,
      maxW,
      numGenerations,
      selections,
      mutations)

    //val totalFitness: Option[Double] = result._1.map(indv => indv.fitnessScore).reduce((acc, curr) => if (curr.get > 0) { Some(acc.get + curr.get)} else acc)

    println("Final results: "+result._2.map(ind => (ind.indexPop, ind.fitnessScore.get)).collect().mkString(";"))
    sc.stop()
  }
}

