package GeneticAlgorithm

import domain.{Fitness, Individual}
import domain._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.rdd.RDD

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
                                        maxWeight: Double,
                                        numGen: Int,
                                        mutationF: MutationFunction,
                                        selectionSelector: Selector[SelectionFunction],
                                        mutationSelector: Selector[MutationFunction]) ={
    // Why we use a Mega-function to do almost everything (Select-Mutation-Cross and Fitness calc)?:
    //   because we can make everything in a simple pass in the worker side

    /**
      * This cross function will create two children from parentA and parentB
      *
      * Note : A near future improvement will be the mutation fuction passed like an argument like the fitness function
      *
      * @param parentA
      * @param parentB
      * @tparam T
      * @return
      * @todo A near future improvement: mutation function passed like an argument
      */
    def cross[T](parentA: Individual[T],
                 parentB: Individual[T]):(Individual[Boolean], Individual[Boolean])  = {

      val chrSize = parentA.chromosome.size
      val crossPoint = scala.util.Random.nextInt(chrSize)
      // We'll need the chromosome of each parent to create the new individuals
      val chrmA: Array[Double] = parentA.chromosome.toDense.values.slice(0,crossPoint)++
        parentB.chromosome.toDense.values.slice(crossPoint,chrSize)
      val chrmB: Array[Double] = parentB.chromosome.toDense.values.slice(0,crossPoint)++
        parentA.chromosome.toDense.values.slice(crossPoint,chrSize)
      // Mutation.
      val chrmAMutated = mutationF.mutation(chrmA, mutateProb)
      val chrmBMutated = mutationF.mutation(chrmB, mutateProb)
      // Execute of the crossover and creation of the two new individuals
      val res = (new Individual[Boolean](new DenseVector(chrmAMutated), Some(0.toDouble)),
        new Individual[Boolean](new DenseVector(chrmBMutated), Some(0.toDouble)))
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
    def selection(index: Int, iter: Iterator[Individual[Boolean]]): Iterator[Individual[Boolean]] = {
      var iter2 = iter
      // Selection and replacement must be done "numGen" times
      for (i <- 0 to numGen) {
        println(s"--- Running Gen: $i population: $index ---- ")
        // Ordering the population by fitnessScore and storing in each Individual it's population (for statistical purposes)
        val currentSelectionOrdered: List[Individual[Boolean]] = iter2.toList.map(ind => {
          Individual[Boolean](ind.chromosome, ind.fitnessScore, bestInd = false, index)
        }).sortBy(x => x.fitnessScore).reverse

        // Calculate the popSize and then the number of Individuals to be selected and to be Crossed
        val initialPopSize = currentSelectionOrdered.size
        //val selectionSize = (initialPopSize * selectionPercentage).ceil.toInt

        val selectionF: SelectionFunction = selectionSelector(index)

        // This is probably the best (in terms of optimization) point to make the calculation of the fitness of
        // each new individual
        val springs = selectionF.selection(currentSelectionOrdered, selectionPercentage). //  (1) Selecting the N parents that will create the next childhood
          sliding(2, 2).                                                                  //  (2) Sliding is the trick here: List(0,1,2,3).sliding(2,2).ToList = List(List(0, 1), List(2, 3))
          map { l => l match {
          case List(parent_A, parent_B) =>
            val spring = cross(parent_A, parent_B)                                        // (3) Now that we have the parents separated in Lists, we can crossover
            List(spring._1(fitness.fitnessFunction).setPop(index), spring._2(fitness.fitnessFunction).setPop(index)) // (4) Remember to fitness the children!!!
          case List(p) => List(p)
        }
        }.toList.
          flatMap(x => x) // (6) we are interested in a plain List, not in a List of Lists => flatmap(x => x)

        // I've chosen a type of replacement that select the best individuals from the sum of: oldPopulation + newPopulation
        // in a very near future new replacement strategies will be implemented and pass like function
        val genOldAndGenNew = springs ++ currentSelectionOrdered
        val selectedIndviduals = genOldAndGenNew.sortBy(x => x.fitnessScore).reverse.take(initialPopSize)
        // println("number of elements in Selection: " + selectedIndv.size)
        // println("gen:"+i+" pob: "+index+"****"+selectedIndviduals.head.fitnessScore)
        iter2 = (List(selectedIndviduals.head.setBest(true)) ++ selectedIndviduals.tail).iterator
        //selectedIndviduals.iterator
      }
      iter2
    }

    // Why mapPartitions?

    //   I've the entire population spread in some partitions of the RDD
    // This GA selects the best N individuals of the entire population from generation i-1 to generation i.
    // To select the best "k" individuals and toss the worst "k" individuals to be substituted in some point we will need
    // to do a "collect".
    // The Selection is, probably, the principal tool in a GA. One of the problems in a GA is when your population lost the diversity and
    // will drive (with a high probability) the GA to a local optimum.
    // To maintain the diversity in the population is a problem driven with Mutation (for example) but sometimes this operator it's not enough.
    // The replacement (the second phase in the selection) is another important tool to keep the diversity in out population.

    // This solution will not follow the usual path: The current implementation will calculate and replace the best K individuals
    // in each partition, not in the entire population, this way we have a set of GAs running in parallel in our RDD.

    val populationRDD = population.mapPartitionsWithIndex(selection, preservesPartitioning = true)

    //val totalFitness: Option[Double] = populationRDD.map(indv => indv.fitnessScore).reduce((acc, curr) => if (curr.get > 0) { Some(acc.get + curr.get)} else acc)
    val bestIndvs = populationRDD.filter(ind => ind.bestInd)

    // populationRDD
    (populationRDD,bestIndvs)
  }
}
