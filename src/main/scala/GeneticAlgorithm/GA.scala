package GeneticAlgorithm

import GeneticAlgorithm.MutationOperators.MutationOperator
import GeneticAlgorithm.SelectionOperators.SelectionOperator
import domain.{Fitness, Individual}
import org.apache.spark.mllib.linalg.DenseVector
import org.apache.spark.rdd.RDD

object GA{

  /**
    *
    * @param population : Original populations to be evolved
    * @param fitness : Fitness Function to measure each Individual
    * @param maxWeight : Max weight to carry in the Knapsack problem
    * @param numGen : Max num of generations
    * @param selectionSelector : Selections to be applied to each population in each generation
    * @param mutationSelector : Mutations to be applied to each population in each generation
    * @return The final population once the stop condition have been satisfied
    */
  def selectAndCrossAndMutatePopulation[T](population: RDD[Individual[T]],
                                           fitness: Fitness,
                                           maxWeight: Double,
                                           numGen: Int,
                                           selectionSelector: Selector[SelectionOperator[T]],
                                           mutationSelector: Selector[MutationOperator])={
    /**
      * This cross function will create two children from parentA and parentB
      *
      * Note : A near future improvement will be the mutation function passed like an argument like the fitness function
      *
      * @param parentA : Individual
      * @param parentB : Individual
      * @return A pair of Individuals product of the crossed between parentA and parentB
      * @todo A near future improvement: mutation function passed like an argument
      */
    def cross(parentA: Individual[T],
              parentB: Individual[T],
              index: Int):(Individual[T], Individual[T])  = {
      val chrSize = parentA.chromosome.size
      val crossPoint = scala.util.Random.nextInt(chrSize)
      // We'll need the chromosome of each parent to create the new individuals
      val chrmA: Array[Double] = parentA.chromosome.toDense.values.slice(0,crossPoint)++parentB.chromosome.toDense.values.slice(crossPoint,chrSize)
      val chrmB: Array[Double] = parentB.chromosome.toDense.values.slice(0,crossPoint)++parentA.chromosome.toDense.values.slice(crossPoint,chrSize)
      // Mutation.
      val chrmAMutated = mutationSelector(index)(chrmA)
      val chrmBMutated = mutationSelector(index)(chrmB)
      // Execute of the crossover and creation of the two new individuals
      ( new Individual[T](new DenseVector(chrmAMutated), Some(0.toDouble)),
        new Individual[T](new DenseVector(chrmBMutated), Some(0.toDouble)))
    }

    /**
      * The selection function will select the parents to be crossover to build a new generation.
      *
      * The amount of parents that will be selected is calculated like a percentage of the total size in our population
      *
      * @param index : Population Id
      * @param iter : Population
      * @return
      */
    def selection(index: Int, iter: Iterator[Individual[T]])= {
      var iter2 = iter
      // Selection and replacement must be done "numGen" times
      for (i <- 0 to numGen) {
        // Ordering the population by fitnessScore and storing in each Individual the population index (for statistical purposes)
        val currentSelectionOrdered: List[Individual[T]] = iter2.toList.map(
          ind => {Individual[T](ind.chromosome, ind.fitnessScore, bestInd = false, index)}
        ).sortBy(x => x.fitnessScore).reverse
        // Calculate the popSize and then the number of Individuals to be selected and to be Crossed
        val initialPopSize = currentSelectionOrdered.size
        val selectionF = selectionSelector(index)
        // Calculate the next Generation
        val springs = selectionF(currentSelectionOrdered).           //  (1) Selecting the N parents that will create the next childhood
          sliding(2, 2).                                                                  //  (2) Sliding is the trick here: List(0,1,2,3).sliding(2,2).ToList = List(List(0, 1), List(2, 3))
          map(
          l => l match {
            case List(parent_A, parent_B) =>
              val spring = cross(parent_A, parent_B, index)                               //  (3) Now that we have the parents separated in Lists, we can crossover
              List(spring._1(fitness.fitnessFunction).setPop(index), spring._2(fitness.fitnessFunction).setPop(index)) // (4) Remember to fitness the children!!!
            case List(p) =>
              List(p)
          }
        ).
          toList.flatMap(x => x)                                                          // (5) we are interested in a plain List, not in a List of Lists => flatmap(x => x)
        // I've chosen a type of replacement that select the best individuals from the append of: oldPopulation + newPopulation
        val selectedIndividuals = (springs ++ currentSelectionOrdered).sortBy(x => x.fitnessScore).reverse.take(initialPopSize)
        iter2 = (List(selectedIndividuals.head.setBest(true)) ++ selectedIndividuals.tail).iterator
      }
      iter2
    }
    // mapPartitionsWithIndex allows us to treat each partition like an entire population
    val populationRDD = population.mapPartitionsWithIndex(selection, preservesPartitioning = true)
    val bestIndvs = populationRDD.filter(ind => ind.bestInd)
    (bestIndvs,bestIndvs)
  }
}
