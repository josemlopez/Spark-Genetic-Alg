package domain

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{Vector, Vectors, DenseVector}
import org.apache.spark.mllib.feature.ElementwiseProduct
import com.github.fommil.netlib.{BLAS => NetlibBLAS, F2jBLAS}

/**
 * Created by jmlopez on 02/01/16.
 */
class fitness {

}

object fitness {

  @transient private var _f2jBLAS: NetlibBLAS = _
  // @transient private var _nativeBLAS: NetlibBLAS = _

  // For level-1 routines, we use Java implementation.
  private def f2jBLAS: NetlibBLAS = {
    if (_f2jBLAS == null) {
      _f2jBLAS = new F2jBLAS
    }
    _f2jBLAS
  }

  def fitnessKnapsackProblem(indv : Individual[Boolean], prices: Broadcast[DenseVector], weight: Broadcast[DenseVector], maxW: Double): Double = {
    require(prices.value.size == weight.value.size)

    val weightInSack = f2jBLAS.ddot(indv.chromosome.size, weight.value.values, 1, indv.chromosome.toDense.values, 1)
    if (weightInSack > maxW) {
      return -1000.toDouble
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