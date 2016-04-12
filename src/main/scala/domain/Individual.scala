package domain
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.linalg.DenseVector
import scala.collection.mutable.ListBuffer
import scala.util.Random

/**
  * Created by jmlopez on 01/01/16.
  */

/**
  *
  * @param chromosome : A Vector modeling the solution of the problem
  * @param fitnessScore : Fitness of the Individual
  * @param bestInd : True if it's the best Individual of the population
  * @param indexPop : Id of the population
  * @tparam T : Base type of the Individual
  */
case class Individual[T](chromosome: Vector, fitnessScore: Option[Double], bestInd: Boolean = false, indexPop: Int = -1) {
  override def toString: String = chromosome.toArray.mkString(";")
  def apply(function: Individual[T] => Double): Individual[T] = copy(fitnessScore = Some(function(this)))
  def setBest(value: Boolean = true): Individual[T] = copy(bestInd = value)
  def setPop(value: Int): Individual[T] = copy(indexPop = value)
}

object initialPopulation {
  /**
   * Given a function to generate an Individual, this function returns an Iterator for a generation of the individuals
   * using the function given: generateIndividual
   *
   * @param generateIndividual : Function that given a seed creates an Individual
   * @tparam T : Base type for Individual
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
  /**
    *  Given the size of the Chromosome and the size of the population , this function returns a new population
    *  with size: sizePop containing Individuals with Chromosome size og sizeChrm
    * @param sizeChrm : Size of the Chromosome
    * @param sizePop : Size of the Population returned
    * @return : A new population
    */
  def initialPopulationBoolean(sizeChrm: Int, sizePop: Long): Seq[Individual[Boolean]] = {
    /**
     * Taking the seed, this function creates an individual with a sizeChrm given
     * @param seed : Seed for the Random
     * @return : A new Individual
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