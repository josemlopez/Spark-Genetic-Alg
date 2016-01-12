import domain.{Individual}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}
import domain.generateIndividualBoolean._

import org.apache.spark.mllib.linalg.{Vectors, Vector, DenseVector}
import domain.fitness._


/**
 * Created by jmlopez on 01/01/16.
 */
class GeneticJob {

}

/**
 *
 */
object GeneticJob{
  def main(args: Array[String]) {
    val conf = new SparkConf ().setAppName ("Genetic Application")
    val sc = new SparkContext (conf)

    //This will go to a .ini file or will be passed like argument
    val sizePopulation = 1000
    val populationRDD = sc.parallelize(initialPopulationBoolean(5, sizePopulation), sizePopulation/5)

    //Quick & Dirty testing purposes
    val values: Broadcast[DenseVector] = sc.broadcast(Vectors.dense(20.5, 10.6, 10.2, 1 , 94,   2).toDense)
    val weights =  sc.broadcast(Vectors.dense(10.2, 76,  2,    18, 10.87, 5).toDense)
    val populationFitness = populationRDD.map(ind => (ind, fitnessKnapsackProblem(ind, values, weights)))

    val numGenerations = 10
    for (i <- 1 to numGenerations){
      val populationSelectedAndCrossed =
        selectAndCrossAndMutatePopulation(
          populationFitness,
          0.5,
          sizePopulation,
          0.01f,
          fitnessKnapsackProblem,
          values,
          weights)
    }


    /**
     * Select a percentage of the best Individuals in population
     * @param population
     * @return
     */
    def selectAndCrossAndMutatePopulation(population: RDD[(Individual[Boolean], Double)],
                                 selectionPercentage: Double, // we use a percentage of the final population and not a
                                 sizePopulation: Int,
                                 mutateProb: Float,
                                 fitness: (Individual[Boolean], Broadcast[DenseVector], Broadcast[DenseVector]) => Double,
                                 values: Broadcast[DenseVector], //In the "Knapsack problem" we have "Values of Objects and Weights of Objects"
                                 weights: Broadcast[DenseVector]):

    RDD[(Individual[Boolean], Double)]={
      // Why we use a Mega-function to do almost everything (Select-Mutation-Cross and Fitness calc)?:
      //   because we can make everything in a simple pass in the worker side


      def cross[T](parentA: Individual[Boolean],
                   parentB: Individual[Boolean]): (Individual[Boolean],Individual[Boolean])  = {

        def onePointMutationBoolean(chrm: Array[Double], mutateProb: Float): Unit ={
          val chrSize = chrm.size
          val mutateRandom = scala.util.Random.nextFloat()
          if (mutateRandom >= mutateProb){
            val mutatePoint = scala.util.Random.nextInt(chrSize)
            chrm(mutatePoint) match {
              case 0 => chrm(mutatePoint) = 1.toDouble
              case 1 => chrm(mutatePoint) = 0.toDouble
            }
          }
        }

        val chrSize = parentA.chromosome.size
        val crossPoint = scala.util.Random.nextInt(chrSize)
        //crossover
        val chrmA: Array[Double] = parentA.chromosome.toDense.values.slice(0,crossPoint)++
          parentB.chromosome.toDense.values.slice(crossPoint+1,chrSize)
        val chrmB: Array[Double] = parentB.chromosome.toDense.values.slice(0,crossPoint)++
          parentA.chromosome.toDense.values.slice(crossPoint+1,chrSize)
        //One point mutation
        onePointMutationBoolean(chrmA, mutateProb)
        onePointMutationBoolean(chrmB, mutateProb)
        //
        (new Individual[Boolean](new DenseVector(chrmA), Some(0.toDouble)),
          new Individual[Boolean](new DenseVector(chrmB), Some(0.toDouble)))
      }

      def selection(iter: Iterator[(Individual[Boolean], Double)]): Iterator[(Individual[Boolean], Double)] = {
        var res = List[(Individual[Boolean], Double)]()
        val partitionSize = iter.size
        var selectionSize = partitionSize*selectionPercentage
        val pre = iter.next
        while (selectionSize > 0 && iter.hasNext){
          val cur = iter.next
          val descents = cross(pre._1, cur._1)
          // This is probably the best (in terms of optimization) point to make the calculation of the fitness
          res = res:::List(pre,cur,(descents._1, fitnessKnapsackProblem(descents._1, values, weights)),
            (descents._2,  fitnessKnapsackProblem(descents._2, values, weights)))
          selectionSize -= 1
        }
        // In this point we have to give one concession:
        //   As we know we have a partition of our population in each partition of the RDD
        // A GA with elitism select the best N individuals of the population to be crossed to generate new individuals.
        // These new individual will be "fitness" and will replace the worst N individuals of the population.
        // But we will not follow that path: this implementation will calculate and replace the best M individuals of the
        // current partition, this way the implementation will be faster than the other and our population can be huge.

        //This implementation, indeed, can be a good idea in terms of diversity in our population: a problem with the
        // elitism is that we can fall in local solution quickly if we lose diversity in our population fast. This problem
        // is present when you select always the same parents (the best) and the algorithm doesn't inspect other posible
        // paths that can appear worst in the beginning but can be far better finally.
        res.iterator
      }

      //We use a map for each partition, this way the execution is all in the worker side.
      //We have to give some concessions:
      population.mapPartitions(selection)
    }


  }
}