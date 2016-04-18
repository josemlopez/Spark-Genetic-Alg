package GeneticAlgorithm
import domain._

/**
  * Class Created by jmlopez on 08/04/16.
  * Modified by rferrer on 18/04/16.
  */

// Mutation

object MutationOperators {

  type MutationOperator = (Array[Double]) => Array[Double]

  /**
    *
    * @param mutationProb : Mutation probability
    * @param chrm : Chromosome to mutate
    * @return : A new chromosome with a unique position mutated
    */
  def OnePointMutation(mutationProb: Float)(chrm: Array[Double]): Array[Double] ={
    val chrSize = chrm.length
    val mutateRandom = scala.util.Random.nextFloat()
    if (mutateRandom >= mutationProb){
      val mutatePoint = scala.util.Random.nextInt(chrSize)
      chrm(mutatePoint) match {
        case 0 => chrm(mutatePoint) = 1.toDouble
        case 1 => chrm(mutatePoint) = 0.toDouble
      }
    }
    chrm
  }

  /**
    * This function doesn't apply mutation
    * @param chrm : Chromosome to mutate
    * @return : The same chromosome pass like argument
    */
  def NoMutation(chrm: Array[Double]): Array[Double] = chrm

}

/**
  *
  */
object SelectionOperators {

  type SelectionOperator[T] = (List[Individual[T]]) => List[Individual[T]]

  /**
    *  This function will select the first N Individuals. Where N is: population.length * percentage
    * @param population : Population from where to select the Individuals
    * @param percentage : Percentage of Individuals to be selected
    * @tparam T : Base type of the Individuals to be selected
    * @return : A population of length : population.length * percentage
    */
  def SelectionNaive[T](percentage: Double)(population: List[Individual[T]]): List[Individual[T]] = {
    population.take((population.length * percentage).toInt)
  }

  /**
    * This function will select N Individuals Randomly. Where N is: population.length * percentage
    * @param population : Population from where to select the Individuals
    * @param percentage : Percentage of Individuals to be selected
    * @tparam T : Base type of the Individuals to be selected
    * @return : A population of length : population.length * percentage
    */
  def SelectionRandom[T](percentage: Double)(population: List[Individual[T]]): List[Individual[T]] = {
    scala.util.Random.shuffle(population).take((population.length * percentage).toInt)
  }

  /**
    * A function that doesn't select Individuals but returns a population where all the Individuals are the
    * first Individual of the given population
    * @param population : Population from where to select the Individuals
    * @param percentage : Percentage of Individuals to be selected
    * @tparam T : Base type of the Individuals to be selected
    * @return : A population of length : population.length * percentage
    */
  def SelectionWrong[T](percentage: Double)(population: List[Individual[T]]): List[Individual[T]] = {
    List.fill((population.length * percentage).toInt)(population.head)
  }

}

// Selector
class Selector[T](toSelect: Seq[T]) extends java.io.Serializable{
  def apply(index: Int): T = toSelect(index % toSelect.length)
}

/*
trait GAOperators[T]{
  def crossover:(Individual[T], Individual[T]) => List[Individual[T]]
  def fitnessFunction:(Individual[T]) => Double
  def populationGenerator:(Long) => Iterator[Individual[T]]
}*/