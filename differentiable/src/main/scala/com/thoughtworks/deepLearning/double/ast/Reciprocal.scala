package com.thoughtworks.deepLearning
package double.ast

import cats._
import cats.implicits._
import com.thoughtworks.deepLearning.NeuralNetwork._
import com.thoughtworks.deepLearning.double.utilities.DoubleMonoidBatch

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
final case class Reciprocal[Input0 <: Batch](
    operand: NeuralNetwork.Aux[Input0, Batch.Aux[Eval[scala.Double], Eval[scala.Double]]])
    extends Cached {

  protected final class SharedBatch private[deepLearning](override val input: Input0,
                                    upstream: Batch.Aux[Eval[scala.Double], Eval[scala.Double]])
      extends MonoidBatch
      with DoubleMonoidBatch {
    type Input >: Input0
    val value = upstream.value.map(1.0 / _)

    override protected def closeUpstreams(): Unit = {
      upstream.close()
    }

    override protected def rawBackward(delta: Eval[scala.Double]): Unit = {
      val a = upstream.value
      upstream.backward(delta.map2(a) { (outputDeltaValue: scala.Double, aValue: scala.Double) =>
        -outputDeltaValue / (aValue * aValue)
      })
    }
  }

  type Input = Input0

  override protected def rawForward(input: Input): SharedBatch = {
    val upstream = operand.forward(input)
    new SharedBatch(input, upstream)
  }
}
