package GeneticAlgorithm
import domain._

/**
  * Class Created by jmlopez on 08/04/16.
  */

// Mutation
/**
  * Trait for the Mutation functions
  */
trait MutationFunction extends java.io.Serializable{
  def mutation(chrm:Array[Double], mutateProb: Float) : Array[Double]
}

/**
  * This mutation function changes a unique position of the given chromosome with probability mutateProb
  */
class OnePointMutation extends MutationFunction {

  /**
    * This mutation function changes a unique position of the given chromosome with probability mutateProb
    * @param chrm : Chromosome to mutate
    * @param mutateProb : Mutation probability
    * @return : A new chromosome with a unique position mutated
    */
  override  def mutation(chrm: Array[Double], mutateProb: Float): Array[Double] ={
    val chrSize = chrm.length
    val mutateRandom = scala.util.Random.nextFloat()
    if (mutateRandom >= mutateProb){
      val mutatePoint = scala.util.Random.nextInt(chrSize)
      chrm(mutatePoint) match {
        case 0 => chrm(mutatePoint) = 1.toDouble
        case 1 => chrm(mutatePoint) = 0.toDouble
      }
    }
    chrm
  }
}

/**
  * Mutation
  */
class NoMutation extends MutationFunction {
  /**
    * This function doesn't apply mutation
    * @param chrm : Chromosome to mutate
    * @param mutateProb : Mutation probability
    * @return : The same chromosome pass like argument
    */
  override  def mutation(chrm: Array[Double], mutateProb: Float): Array[Double] = chrm
}



/**
  *
  */
object SelectionOperators {
  /**
    *  This function will select the first N Individuals. Where N is: population.length * percentage
    * @param population : Population from where to select the Individuals
    * @param percentage : Percentage of Individuals to be selected
    * @tparam T : Base type of the Individuals to be selected
    * @return : A population of length : population.length * percentage
    */
   def SelectionNaive[T](population: List[Individual[T]], percentage: Double): List[Individual[T]] = {
    population.take((population.length * percentage).toInt)
  }

  /**
    * This function will select N Individuals Randomly. Where N is: population.length * percentage
    * @param population : Population from where to select the Individuals
    * @param percentage : Percentage of Individuals to be selected
    * @tparam T : Base type of the Individuals to be selected
    * @return : A population of length : population.length * percentage
    */
  def SelectionRandom[T](population: List[Individual[T]], percentage: Double): List[Individual[T]] = {
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
  def SelectionWrong[T](population: List[Individual[T]], percentage: Double): List[Individual[T]] = {
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