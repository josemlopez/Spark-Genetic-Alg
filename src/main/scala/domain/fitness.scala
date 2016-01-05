package domain

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.linalg.{Vector, Vectors, DenseVector}
import org.apache.spark.mllib.feature.ElementwiseProduct
import com.github.fommil.netlib.{BLAS => NetlibBLAS, BLAS, F2jBLAS}

/**
 * Created by jmlopez on 02/01/16.
 */
class fitness {

}

object fitness {

  @transient private var _f2jBLAS: NetlibBLAS = _
  @transient private var _nativeBLAS: NetlibBLAS = _

  // For level-1 routines, we use Java implementation.
  private def f2jBLAS: NetlibBLAS = {
    if (_f2jBLAS == null) {
      _f2jBLAS = new F2jBLAS
    }
    _f2jBLAS
  }

  def fitnessMochilaProblem(indv : Individual[Boolean], prices: Broadcast[DenseVector], weight: Broadcast[DenseVector]): Double = {
    require(prices.value.size == weight.value.size)
    val ewPWeight = new ElementwiseProduct(weight.value)
    val valuesByWeight = ewPWeight.transform(prices.value).toDense
    f2jBLAS.ddot(prices.value.size, valuesByWeight.values, 1, indv.chromosome.toDense.values, 1)
  }
}