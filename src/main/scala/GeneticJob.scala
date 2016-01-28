import domain.{Individual}
import GeneticAlgorithm.GA._
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}
import domain.generateIndividualBoolean._

import org.apache.spark.mllib.linalg.{Vectors, Vector, DenseVector}
import domain.fitness._


/**
 * Created by jmlopez on 01/01/16.
 */
object GeneticJob{
  def main(args: Array[String]) {
    // val conf = new SparkConf ().setAppName ("Genetic Application")
    val sc = new SparkContext ()

    /* var rdd = sc.parallelize(List(1,2,3,4,5,6,7,8),2)
    for (i <-1 to 10){
      rdd = rdd.map(x => x+1)
    }
    rdd.foreach(println)
    */

    // This will go to a .ini file or will be passed like argument
    // Quick & Dirty testing purposes
    // Broadcasting the values and weights to the workers. This vector can be huge "in normal use"
    val values: Broadcast[DenseVector] = sc.broadcast(Vectors.dense(20.5, 10.6, 10.2, 1 , 94, 2, 1, 3, 7, 91, 64, 4).toDense)
    val weights =  sc.broadcast(Vectors.dense(10.2, 76,  2, 18, 10.87, 5, 2, 1, 3, 1, 76, 198).toDense)
    val maxW = 67

    val sizePopulation = 10
    var populationRDD = sc.parallelize(initialPopulationBoolean(5, sizePopulation), 3).
      map(ind => (fitnessKnapsackProblem(ind, values, weights, maxW), ind))


    println("----------initial population and Fitness-------")
    populationRDD.foreach(println)

    val numGenerations = 10
    for (i<-0 to numGenerations) {
      val populationSize = populationRDD.count()
      val partitions = populationRDD.partitions.size
      println(s"----------Generation $i with populationSize $populationSize and numPartitions $partitions -------")

      populationRDD = selectAndCrossAndMutatePopulation (
        populationRDD,
        0.5,
        sizePopulation,
        0.01f,
        fitnessKnapsackProblem,
        values,
        weights,
        maxW)
      //populationFitness.foreach(println)
      println ("Total fitness: " + populationRDD.map (indv => indv._1).reduce ((acc, curr) => if (curr > 0) {acc + curr} else acc))
    }
    populationRDD.takeOrdered(1)(Ordering[Double].reverse.on(x => x._1)).
      foreach(x => println("This is the solution: " + x.toString()))
  }
}


