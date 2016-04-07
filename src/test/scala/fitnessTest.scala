/**
 * Created by jmlopez on 27/01/16.
 */

/**
import GeneticAlgorithm.GA
import domain.Individual
import domain.FitnessKnapsackProblem
import domain.generateIndividualBoolean._
import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{Vectors, DenseVector}

import collection.mutable.Stack
import org.scalatest._

class fitnessTest extends FlatSpec with Matchers {

  val conf = new SparkConf ().setAppName ("Genetic Application")
  val sc = new SparkContext (conf)
  val maxW = 45

  // This will go to a .ini file or will be passed like argument
  val sizePopulation = 10
  val populationRDD = sc.parallelize(initialPopulationBoolean(5, sizePopulation), sizePopulation/5)

  // Quick & Dirty testing purposes
  // Broadcasting the values and weights to the workers. This vector can be huge "in normal use"
  val values: Broadcast[DenseVector] = sc.broadcast(Vectors.dense(20.5, 10.6, 10.2, 1 , 94,   2).toDense)
  val weights =  sc.broadcast(Vectors.dense(10.2, 76,  2,    18, 10.87, 5).toDense)


  val populationFitness = FitnessKnapsackProblem(Individual[Boolean](Vectors.dense(1,1,1,1,1,1), Option.empty[Double]), values, weights, maxW)

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    stack.pop() should be (2)
    stack.pop() should be (1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new Stack[Int]
    a [NoSuchElementException] should be thrownBy {
      emptyStack.pop()
    }
  }

  //GA.sel

}

  **/