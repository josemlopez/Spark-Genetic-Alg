package GeneticAlgorithm

import domain.Individual
import domain.fitness._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.rdd.RDD

/**
 * Created by jmlopez on 27/01/16.
 */
object GA{
  /**
   * Select a percentage of the best Individuals in population
   * @param population
   * @return
   */
  def selectAndCrossAndMutatePopulation(population: RDD[(Double, Individual[Boolean])],
                                        selectionPercentage: Double, // we use a percentage of the final population and not a fixed population selection
                                        sizePopulation: Int,
                                        mutateProb: Float,
                                        fitness: (Individual[Boolean], Broadcast[DenseVector], Broadcast[DenseVector], Double) => Double,
                                        values: Broadcast[DenseVector], //In the "Knapsack problem" we have "Values of Objects and Weights of Objects"
                                        weights: Broadcast[DenseVector],
                                        maxWeight: Double): RDD[(Double, Individual[Boolean])]={
    // Why we use a Mega-function to do almost everything (Select-Mutation-Cross and Fitness calc)?:
    //   because we can make everything in a simple pass in the worker side

    val numPartitions = population.partitions.size
    // This method will take one random point for each individual (A and B) and create two new individuals with the
    // genome of each parent
    def cross[T](parentA: Individual[Boolean],
                 parentB: Individual[Boolean]): (Individual[Boolean],Individual[Boolean])  = {
      // Here the point of cross is selected and a mutation is applied to the chromosome with probability: mutateProb
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
      // We'll need the chromosome of each parent to create the new individuals
      val chrmA: Array[Double] = parentA.chromosome.toDense.values.slice(0,crossPoint)++
        parentB.chromosome.toDense.values.slice(crossPoint+1,chrSize)
      val chrmB: Array[Double] = parentB.chromosome.toDense.values.slice(0,crossPoint)++
        parentA.chromosome.toDense.values.slice(crossPoint+1,chrSize)
      // One point mutation.
      if (chrmA.size == 0 || chrmB.size == 0){
        println("Estos son: " + chrmA.mkString(";") + " y este " + chrmB.mkString(";"))
        println("Y estos los padres: " + parentA.toString() + " y este : " + parentB.toString())
      }
      onePointMutationBoolean(chrmA, mutateProb)
      onePointMutationBoolean(chrmB, mutateProb)
      // Execute of the crossover and creation of the two new individuals
      val res = (new Individual[Boolean](new DenseVector(chrmA), Some(0.toDouble)),
        new Individual[Boolean](new DenseVector(chrmB), Some(0.toDouble)))
      //println(" CrossOver: " + res.toString())
      res
    }

    def selection(iter: Iterator[(Double, Individual[Boolean])]): Iterator[(Double, Individual[Boolean])] = {
      var currentSelectionOrdered = iter.toList.sortBy(x => x._1).reverse
      val initialPopSize = currentSelectionOrdered.size
      var selectionSize = (initialPopSize*selectionPercentage).ceil
      var res: List[(Double, Individual[Boolean])] = List()
      while (selectionSize>=2){
        val selectionSplit = currentSelectionOrdered.splitAt(2)
        currentSelectionOrdered = selectionSplit._2
        val parent_A = selectionSplit._1.head
        val parent_B = selectionSplit._1.last
        val descents = cross(parent_A._2, parent_B._2)
        // This is probably the best (in terms of optimization) point to make the calculation of the fitness of
        // each individual
        val family: List[(Double, Individual[Boolean])] = List(parent_A, parent_B,
          (fitnessKnapsackProblem(descents._1, values, weights, maxWeight), descents._1),
          (fitnessKnapsackProblem(descents._2, values, weights, maxWeight), descents._2)).sortBy(x => x._1).reverse
        // Once we have sorted the list of individuals in the family, we take the best 2 individuals.
        // This is pure Elitism and it affect directly to the behaviour of the GA because it affect to the diversity of
        // the population.
        //println("family: " +family.mkString(";"))
        res = res:::family
        //println("Res: " +res.mkString(";"))
        selectionSize -= 2
      }
      // In this point we have to give one concession:
      //   As we know, we have a partition of our population in each partition of the RDD
      // An GA with elitism selects the best N individuals of the population to be crossed to generate new individuals.
      // These new individuals will be "fitness" and will replace the worst N individuals of the population.
      // I will not follow that path: The current implementation will calculate and replace the best M < N (where M aprox N * numpartitions)
      // individuals of the current partition, this way the implementation will be faster than the other
      // and our population can be huge.

      //This implementation have another side effect: it can be a good idea in terms of diversity in our population.
      // A problem with the elitism is that we can fall in local solution quickly if we lose diversity in our population
      // fast. This problem is present when you select always the same parents (the best) and the algorithm doesn't
      // inspect other possible paths that can appear worst in the beginning but can be far better finally.
      val selectedIndv = res.sortBy(x=>x._1).reverse.take(initialPopSize)
      //println("number of elements in Selection: " + selectedIndv.size)
      selectedIndv.iterator
    }

    //We use a map for each partition, this way the execution is all in the worker side.
    //We have to give some concessions:
    population.mapPartitions(selection, preservesPartitioning = true)
  }
}
