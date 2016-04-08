package domain

import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.DenseVector

/**
 * Created by jmlopez on 01/01/16.
 */
case class Individual[T](chromosome: Vector, fitnessScore: Option[Double], var bestInd: Boolean = false, population: Int = -1) {
  override def toString(): String = {
     chromosome.toArray.mkString(";")
  }
  def apply(function: Individual[T] => Double) = Individual[T](chromosome=this.chromosome, fitnessScore = Some(function(this)))
  def setBest(value: Boolean = true): Individual[T] = Individual[T](this.chromosome, this.fitnessScore, value, population)
  def setPop(value: Int) = Individual[T](this.chromosome, this.fitnessScore, bestInd, value)
}

trait MutationFunction extends java.io.Serializable{
  def mutation(chrm:Array[Double], mutateProb: Float) : Array[Double]
}

class OnePointMutation extends MutationFunction {
  // Here the point of cross is selected and a mutation is applied to the chromosome with probability: mutateProb
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

class NoMutation extends MutationFunction {
  override  def mutation(chrm: Array[Double], mutateProb: Float): Array[Double] = chrm
}

class Selector[T](toSelect: Seq[T]) extends java.io.Serializable{
  def apply(index: Int): T = toSelect(index % toSelect.length)
}

trait SelectionFunction extends java.io.Serializable {
  def selection[T](population: List[Individual[T]], percentage: Double): List[Individual[T]]
}

class SelectionNaive extends SelectionFunction {
  override def selection[T](population: List[Individual[T]], percentage: Double): List[Individual[T]] = {
    population.take((population.length * percentage).toInt)
  }
}

class SelectionRandom extends SelectionFunction {
  override def selection[T](population: List[Individual[T]], percentage: Double): List[Individual[T]] = {
    scala.util.Random.shuffle(population).take((population.length * percentage).toInt)
  }
}

class SelectionWrong extends SelectionFunction {
  override def selection[T](population: List[Individual[T]], percentage: Double): List[Individual[T]] = {
    List.fill((population.length * percentage).toInt)(population.head)
  }
}


trait GAOperators[T]{
  def crossover:(Individual[T], Individual[T]) => List[Individual[T]]
  def fitnessFunction:(Individual[T]) => Double
  def populationGenerator:(Long) => Iterator[Individual[T]]
}

import scala.collection.mutable.ListBuffer
import scala.util.Random

object initialPopulation {

  /**
   * Given a function to generate an Individual, this function returns an Iterator for a generation of the individuals
   * using the function given: generateIndividual
    *
    * @param generateIndividual
   * @tparam T
   * @return
   */
  def randomInitialPopulation[T](generateIndividual:Long => Individual[T]): Long => Iterator[Individual[T]] = {
    def result(seed: Long = System.currentTimeMillis()): Iterator[Individual[T]] = {
      val random = new Random(seed)
      new Iterator[Individual[T]]{
        var chseed = random.nextLong()
        def hasNext = true
        def next(): Individual[T] = {
          chseed = random.nextLong()
          generateIndividual(chseed)
        }
      }
    }
    result
  }

}

object generateIndividualBoolean {
  import initialPopulation._

  def initialPopulationBoolean(sizeChrm: Int, sizePop: Long): Seq[Individual[Boolean]] = {

    /**
     * Taking the seed, this function creates an individual with a sizeChrm given
      *
      * @param seed
     * @return
     */
    def generateIndividualBoolean(seed: Long) : Individual[Boolean] = {
      val r = new Random(seed)
      val chromosome = new DenseVector((0 to sizeChrm).map( _ => r.nextBoolean() match {
        case false => 0.toDouble
        case true  => 1.toDouble }).toArray)
      new Individual[Boolean](chromosome, Option.empty[Double])
    }
    val population = new ListBuffer[Individual[Boolean]]
    val populationIterator = randomInitialPopulation[Boolean](generateIndividualBoolean)(Random.nextLong())
    for (i <- 1 to sizePop.toInt) {
      population += populationIterator.next()
    }
    population
  }
}