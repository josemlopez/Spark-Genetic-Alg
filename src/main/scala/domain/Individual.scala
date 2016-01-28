package domain

import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.DenseVector

/**
 * Created by jmlopez on 01/01/16.
 */
case class Individual[T](chromosome: Vector, fitness: Option[Double]) {
  override def toString(): String = {
     chromosome.toArray.mkString(";")
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