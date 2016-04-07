package domain

import com.github.fommil.netlib.{F2jBLAS, BLAS => NetlibBLAS}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.DenseVector

/**
 * Created by jmlopez on 02/01/16.
 */


trait Fitness extends java.io.Serializable {

  @transient private var _f2jBLAS: NetlibBLAS = _
  // @transient private var _nativeBLAS: NetlibBLAS = _

  // For level-1 routines, we use Java implementation.
  protected def f2jBLAS: NetlibBLAS = {
    if (_f2jBLAS == null) {
      _f2jBLAS = new F2jBLAS
    }
    _f2jBLAS
  }

  def fitnessFunction[T](individual: Individual[T]): Double

}

class FitnessKnapsackProblem (prices: Broadcast[DenseVector], weight: Broadcast[DenseVector], maxW: Double)
  extends Fitness with java.io.Serializable{

  override def fitnessFunction[Boolean](indv : Individual[Boolean]): Double = {
    require(prices.value.size == weight.value.size)

    val weightInSack = f2jBLAS.ddot(indv.chromosome.size, weight.value.values, 1, indv.chromosome.toDense.values, 1)
    if (weightInSack > maxW) {
      return -1000.toDouble * weightInSack
    }

    // Element-wise Product ---> [a11 a12 a13] [b11 b12 b13] = [a11*b11 a12*b12 a13*b13]
    /*val ewPWeight = new ElementwiseProduct(weight.value)
    val valuesByWeight = ewPWeight.transform(prices.value).toDense
    val ewPPrice = new ElementwiseProduct(prices.value)
    val pricesByWeight = ewPWeight.transform(prices.value).toDense
    f2jBLAS.ddot(prices.value.size, valuesByWeight.values, 1, indv.chromosome.toDense.values, 1)
    */
    f2jBLAS.ddot(indv.chromosome.size, prices.value.values, 1, indv.chromosome.toDense.values, 1)
  }
}