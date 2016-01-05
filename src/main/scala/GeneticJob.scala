import domain.{Individual}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkContext, SparkConf}
import domain.generateIndividualBoolean._

import org.apache.spark.mllib.linalg.{Vectors, Vector, DenseVector}
import domain.fitness._


/**
 * Created by jmlopez on 01/01/16.
 */
class GeneticJob {

}

/**
 *
 */
object GeneticJob{
  def main(args: Array[String]) {
    val conf = new SparkConf ().setAppName ("Genetic Application")
    val sc = new SparkContext (conf)
    val sizePopulation = 1000
    val populationRDD = sc.parallelize(initialPopulationBoolean(5, sizePopulation), sizePopulation/5)

    val values: Broadcast[DenseVector] = sc.broadcast(Vectors.dense(20.5, 10.6, 10.2, 1 , 94,   2).toDense)
    val weights =  sc.broadcast(Vectors.dense(10.2, 76,  2,    18, 10.87, 5).toDense)

    val numGenerations = 10
    for (i <- 1 to numGenerations){
      val populationFitness = populationRDD.map(ind => (ind, fitnessMochilaProblem(ind, values, weights)))
      val populationSelectedAndCrossed =
        selectAndCrossAndMutatePopulation(
          populationFitness,
          0.5,
          sizePopulation,
          0.01f,
          fitnessMochilaProblem,
          values,
          weights)
    }


    /**
     * Select a percentage of the best Individuals in population
     * @param population
     * @return
     */
    def selectAndCrossAndMutatePopulation(population: RDD[(Individual[Boolean], Double)],
                                 percentage: Double,
                                 sizePopulation: Int,
                                 mutateProb: Float,
                                 fitness: (Individual[Boolean], Broadcast[DenseVector], Broadcast[DenseVector]) => Double,
                                 values: Broadcast[DenseVector],
                                 weights: Broadcast[DenseVector]):

    RDD[(Individual[Boolean], Double)]={
      // Why we use a Mega-function to do almost everything?: we can make everything in a simple pass in the worker side
      def mutateBoolean(chrm: Array[Double], mutateProb: Float): Unit ={
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

      def cross[T](parentA: Individual[Boolean],
                   parentB: Individual[Boolean]): (Individual[Boolean],Individual[Boolean])  = {
        val chrSize = parentA.chromosome.size
        val crossPoint = scala.util.Random.nextInt(chrSize)
        //crossover
        val chrmA: Array[Double] = parentA.chromosome.toDense.values.slice(0,crossPoint)++
          parentB.chromosome.toDense.values.slice(crossPoint+1,chrSize)
        val chrmB: Array[Double] = parentB.chromosome.toDense.values.slice(0,crossPoint)++
          parentA.chromosome.toDense.values.slice(crossPoint+1,chrSize)
        //mutation
        mutateBoolean(chrmA, mutateProb)
        mutateBoolean(chrmB, mutateProb)
        //
        (new Individual[Boolean](new DenseVector(chrmA), Some(0.toDouble)),
          new Individual[Boolean](new DenseVector(chrmB), Some(0.toDouble)))
      }

      def myfunc(iter: Iterator[(Individual[Boolean], Double)]): Iterator[(Individual[Boolean], Double)] = {
        var res = List[(Individual[Boolean], Double)]()
        val partitionSize = iter.size
        var timesToCross = partitionSize*percentage
        val pre = iter.next
        while (timesToCross > 0 && iter.hasNext){
          val cur = iter.next
          val descents = cross(pre._1, cur._1)
          // This is probably the best point to make the calculation of the fitness
          res = res:::List(pre,cur,(descents._1, fitnessMochilaProblem(descents._1, values, weights)),
            (descents._2,  fitnessMochilaProblem(descents._2, values, weights)))
          timesToCross -= 1
        }
        // In this point we have to give one concession: we can select only the best N from the partitionSize (a partition of the population)
        // and this way we make everything in the same pass, or we can wait to make the selection of the best M indiv
        // I'll follow that second path although this is not exactly a GA
        res.iterator
      }

      //We use a map for each partition, this way the execution is all in the worker side.
      //We have to give some concessions:
      population.mapPartitions(myfunc)
    }


  }
}