package GeneticAlgorithm

import domain.Individual
import domain.{Fitness, FitnessKnapsackProblem}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.rdd.RDD
import scala.util.Try

/**
 * Created by jmlopez on 27/01/16.
 */
object GA{

  def selectAndCrossAndMutatePopulation(population: RDD[Individual[Boolean]],
                                        selectionPercentage: Double, // we use a percentage of the final population and not a fixed population selection
                                        sizePopulation: Int,
                                        mutateProb: Float,
                                        fitness: Fitness,
                                        values: Broadcast[DenseVector], //In the "Knapsack problem" we have "Values of Objects and Weights of Objects"
                                        weights: Broadcast[DenseVector],
                                        maxWeight: Double) ={
    // Why we use a Mega-function to do almost everything (Select-Mutation-Cross and Fitness calc)?:
    //   because we can make everything in a simple pass in the worker side

    val numPartitions = population.partitions.size

    /**
      * This cross function will create two children from parentA and parentB
      *
      * Note : A near future improvement will be the mutation fuction passed like an argument like the fitness function
      * @param parentA
      * @param parentB
      * @tparam T
      * @return
      * @todo A near future improvement will be the mutation function passed like an argument
      */
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
        parentB.chromosome.toDense.values.slice(crossPoint,chrSize)
      val chrmB: Array[Double] = parentB.chromosome.toDense.values.slice(0,crossPoint)++
        parentA.chromosome.toDense.values.slice(crossPoint,chrSize)
      // One point mutation.
      onePointMutationBoolean (chrmA, mutateProb)
      onePointMutationBoolean (chrmB, mutateProb)
      // Execute of the crossover and creation of the two new individuals
      val res = (new Individual[Boolean](new DenseVector(chrmA), Some(0.toDouble)),
        new Individual[Boolean](new DenseVector(chrmB), Some(0.toDouble)))
      //println(" CrossOver: " + res.toString())
      res
    }

    /**
      * The selection function will select the parents to be crossover to build a new generation.
      *
      * The amount of parents that will be selected is calculated like a percentage of the total size in our population
      *
      * @param iter
      * @return
      */
    def selection(iter: Iterator[Individual[Boolean]]) = {
      // Ordering the population by fitnessScore    (1)
      val currentSelectionOrdered: List[Individual[Boolean]] = iter.toList.sortBy(x => x.fitnessScore).reverse
      // Calculate the popSize and then the number of Individuals to be selected and to be Crossed
      val initialPopSize = currentSelectionOrdered.size
      val selectionSize = (initialPopSize*selectionPercentage).ceil.toInt

      // This is probably the best (in terms of optimization) point to make the calculation of the fitness of
      // each new individual
      val springs = currentSelectionOrdered. // we have our population in a List, ordered by the bests first
        take(selectionSize).   // (1) Selecting the N parents that will create the next childhood
        sliding(2,2).          // (2) Sliding is the trick here: List(0,1,2,3).sliding(2,2).ToList = List(List(0, 1), List(2, 3))
        map {                  // (3) Now that we have the parents separated in Lists, we can crossover
        case List(parent_A, parent_B) => {
          val spring = cross(parent_A, parent_B)    // (4) Lets remember that mutation is executed inside the Cross (to be changed)
          List(spring._1(fitness.fitnessFunction), spring._2(fitness.fitnessFunction))  // (5) Remember to fitness the children!!!
        }
      }.toList.
        flatMap(x => x)   // (6) we are interested in a plain List, not in a List of Lists => flatmap(x => x)

      val genOldAndGenNew  = springs ++ currentSelectionOrdered
      val selectedIndviduals = genOldAndGenNew.sortBy(x=>x.fitnessScore).reverse.take(initialPopSize)
      //println("number of elements in Selection: " + selectedIndv.size)
      selectedIndviduals.iterator
    }

    // Why mapPartitions?

    //   We have the entire population partitioned in some partitions of the RDD
    // An GA with elitism selects the best N individuals of the entire population from generation i-1 to generation i.
    // To select the best "k" individuals and toss the worst "k" individuals to be substituted in some point we will need
    // to do a "collect". This substitution of the worst with the bests is called Elitism.
    // The Selection is probably the principal tool in a GA. The problem is that Elitism reduce drastically the diversity of the population and
    // will drive (with a high probability) the GA to a local optimum.
    // To maintain the diversity in the population is a problem driven with Mutation (for example) but sometimes this operator it's not enough.

    // This solution will not follow that path: The current implementation will calculate and replace the best K individuals
    // in each partition, not in the entire population, this way we have a set of GAs running in parallel in our RDD.

    population.mapPartitions(selection, preservesPartitioning = true)
  }
}
