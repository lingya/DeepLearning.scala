package com.thoughtworks.deeplearning

import com.thoughtworks.deeplearning.Symbolic._
import com.thoughtworks.deeplearning.DifferentiableINDArray.Layers._
import com.thoughtworks.deeplearning.Symbolic.Layers.Literal
import org.nd4j.linalg.convolution.Convolution
import org.scalatest._
import com.thoughtworks.deeplearning
import com.thoughtworks.deeplearning.DifferentiableDouble._
import com.thoughtworks.deeplearning.DifferentiableINDArray._
import com.thoughtworks.deeplearning.DifferentiableAny._
import com.thoughtworks.deeplearning.DifferentiableInt._
import com.thoughtworks.deeplearning.DifferentiableSeq._
import com.thoughtworks.deeplearning.DifferentiableINDArray.Optimizers._
import com.thoughtworks.deeplearning.Layer.Batch.Aux
import com.thoughtworks.deeplearning._
import com.thoughtworks.deeplearning.Layer.{Aux, Batch}
import com.thoughtworks.deeplearning.Symbolic.Layers.Identity
import com.thoughtworks.deeplearning.Symbolic._
import com.thoughtworks.deeplearning.Poly.MathFunctions._
import com.thoughtworks.deeplearning.Poly.MathMethods.{*, /}
import com.thoughtworks.deeplearning.Poly.MathOps
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.cpu.nativecpu.NDArray
import org.nd4j.linalg.factory.Nd4j
import org.nd4j.linalg.indexing.{INDArrayIndex, NDArrayIndex}
import org.nd4j.linalg.ops.transforms.Transforms
import org.nd4s.Implicits._
import shapeless._
import scala.annotation.tailrec
import scala.collection.immutable.IndexedSeq

import scala.collection.mutable

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
final class LayerSpec extends FreeSpec with Matchers with Inside {

  "INDArrayPlaceholder dot INDArrayPlaceholder" in {

    implicit val learningRate = new LearningRate {
      override def currentLearningRate() = 0.0003
    }

    def makeNetwork(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue =
        Array(
          Array(0.0, 5.0)
        )
      -weightInitialValue.toNDArray.toWeight.dot(x)
    }

    val network = makeNetwork

    val inputData =
      Array(
        Array(2.5, -3.2, -19.5),
        Array(7.5, -5.4, 4.5)
      )

    def train() = {
      val outputBatch = network.forward(inputData.toNDArray.toBatch)
      try {
        val loss = (outputBatch.value: INDArray).sumT
        outputBatch.backward(outputBatch.value)
        loss
      } finally {
        outputBatch.close()
      }
    }

    train().value should be(-33.0)

    for (_ <- 0 until 100) {
      train().value
    }

    math.abs(train().value) should be < 1.0

  }

  "INDArrayPlaceholder im2col (kernel,stride,padding) --forward" in {

    implicit val learningRate = new LearningRate {
      override def currentLearningRate() = 0.0003
    }

    def makeNetwork(kernel: Array[Int], stride: Array[Int], padding: Array[Int])(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = 1 to 54
      -weightInitialValue.toNDArray.reshape(2, 3, 3, 3).toWeight.im2col(kernel, stride, padding)
    }

    val network = makeNetwork(Array(3, 3), Array(1, 1), Array(1, 1))

    val inputData = 1 to 54

    def train() = {
      val outputBatch = network.forward(
        inputData.toNDArray.reshape(2, 3, 3, 3).toBatch
      )
      try {
        val loss = (outputBatch.value: INDArray).sumT
        outputBatch.backward(outputBatch.value)
        loss
      } finally {
        outputBatch.close()
      }
    }

    train().value should be(-8085.0)

    for (_ <- 0 until 10000) {
      train().value
    }

    math.abs(train().value) should be < 1.0

  }

  "INDArrayPlaceholder im2col (kernel,stride,padding) --backward" in {

    implicit def optimizer: Optimizer = new LearningRate {
      def currentLearningRate() = 1
    }

    def makeNetwork(kernel: Array[Int], stride: Array[Int], padding: Array[Int])(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = Array.fill(54)(0).toNDArray.reshape(2, 3, 3, 3)
      val weight = weightInitialValue.toWeight
      weight.im2col(kernel, stride, padding)
    }

    val network = makeNetwork(Array(2, 2), Array(1, 1), Array(1, 1))

    val inputNDArrayData = (23 to 76).toNDArray.reshape(2, 3, 3, 3)

    val backDelta =
      Convolution.im2col(inputNDArrayData, Array(2, 2), Array(1, 1), Array(1, 1))

    def train() = {
      val outputBatch =
        network
          .forward(
            inputNDArrayData.toBatch
          )
      try {
        outputBatch.backward(backDelta)
      } finally {
        outputBatch.close()
      }
    }

    train()

    val result = inside(network) {
      case Im2col(Weight(w), _, _, _) => w
    }

    result.sumT should be(-10692)
  }

  "INDArrayPlaceholder reshape dimensions --forward" in {

    implicit val learningRate = new LearningRate {
      override def currentLearningRate() = 0.0003
    }

    def makeNetwork(dimensions: Int*)(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = 1 to 54
      -weightInitialValue.toNDArray.reshape(2, 3, 3, 3).toWeight.reshape(dimensions: _*)
    }

    val network = makeNetwork(2, 3, 9)

    val inputData = 1 to 54

    def train() = {
      val outputBatch = network.forward(
        inputData.toNDArray.reshape(2, 3, 3, 3).toBatch
      )
      try {
        val loss = (outputBatch.value: INDArray).sumT
        outputBatch.backward(outputBatch.value)
        loss
      } finally {
        outputBatch.close()
      }
    }

    train().value should be(-1485.0)

    for (_ <- 0 until 25000) {
      train().value
    }

    math.abs(train().value) should be < 1.0

  }

  "INDArrayPlaceholder reshape dimensions --backward" in {

    implicit def optimizer: Optimizer = new LearningRate {
      def currentLearningRate() = 1
    }

    def makeNetwork(dimensions: Int*)(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = Array.fill(54)(0).toNDArray.reshape(dimensions: _*)
      weightInitialValue.toWeight.reshape(2, 3, 3, 3)
    }

    val network = makeNetwork(2, 3, 9)

    val inputData = (1 to 54).toNDArray.reshape(2, 3, 3, 3)

    def train() = {
      val outputBatch = network.forward(
        inputData.toBatch
      )
      try {
        outputBatch.backward(inputData)
      } finally {
        outputBatch.close()
      }
    }

    train()

    val result = inside(network) {
      case Reshape(Weight(w), _) => w
    }

    result.sumT should be(-1485)

    val shapeSeq = result.shape.toSeq
    //noinspection ZeroIndexToHead
    shapeSeq(0) should be(2)
    shapeSeq(1) should be(3)
    shapeSeq(2) should be(9)
  }

  "INDArrayPlaceholder permute dimensions --forward" in {

    implicit val learningRate = new LearningRate {
      override def currentLearningRate() = 0.0003
    }

    def makeNetwork(dimensions: Int*)(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = 1 to 54
      -weightInitialValue.toNDArray.reshape(2, 3, 9).toWeight.permute(dimensions: _*)
    }

    val network = makeNetwork(0, 2, 1)

    def train() = {
      val outputBatch = network.forward(
        (1 to 54).toNDArray.reshape(2, 3, 3, 3).toBatch
      )
      try {
        val loss = (outputBatch.value: INDArray).sumT
        outputBatch.backward(outputBatch.value)
        loss
      } finally {
        outputBatch.close()
      }
    }

    train().value should be(-1485.0)

    inside(network) {
      case Negative(Permute(Weight(w), _)) =>
        println(w)
    }

    for (_ <- 0 until 25000) {
      train().value
    }

    math.abs(train().value) should be < 1.0

  }

  "INDArrayPlaceholder permute dimensions --backward" in {

    implicit def optimizer: Optimizer = new LearningRate {
      def currentLearningRate() = 1
    }

    def makeNetwork(dimensions: Int*)(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = Array.fill(54 * 6)(0).toNDArray.reshape(2, 6, 3, 9) //.permute(dimensions: _*)
      weightInitialValue.toWeight.permute(dimensions: _*)
    }

    val network = makeNetwork(0, 2, 1, 3)

    val inputData = (1 to 54 * 6).toNDArray.reshape(2, 6, 3, 9)

    def train() = {
      val outputBatch = network.forward(
        inputData.toBatch
      )
      try {
        outputBatch.backward(inputData)
      } finally {
        outputBatch.close()
      }
    }

    train()

    val result = inside(network) {
      case Permute(Weight(w), _) => w
    }

    val shapeSeq = result.shape.toSeq
    //noinspection ZeroIndexToHead
    shapeSeq(0) should be(2)
    shapeSeq(1) should be(6)
    shapeSeq(2) should be(3)
    shapeSeq(3) should be(9)
  }

  "INDArrayPlaceholder maxPool dimensions --one dimension" ignore {

    implicit val learningRate = new LearningRate {
      override def currentLearningRate() = 0.0003
    }

    def makeNetwork(dimension: Int)(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = 1 to 54
      weightInitialValue.toNDArray.reshape(2, 3, 3, 3).toWeight.maxPool(dimension)
    }

    val network = makeNetwork(3)

    val inputData = 1 to 54

    def train() = {
      val outputBatch = network.forward(
        inputData.toNDArray.reshape(2, 3, 3, 3).toBatch
      )
      try {
        val loss = (outputBatch.value: INDArray).sumT
        outputBatch.backward(outputBatch.value)
        loss
      } finally {
        outputBatch.close()
      }
    }

    train().value should be(513.0)

    inside(network) {
      case MaxPool(Weight(w), _) =>
        println(w)
    }

    for (_ <- 0 until 40000) {
      train().value
    }

    math.abs(train().value) should be < 10.0

  }

  "INDArrayPlaceholder maxPool dimensions --two dimension" ignore {

    implicit val learningRate = new LearningRate {
      override def currentLearningRate() = 0.0003
    }

    def makeNetwork(dimensions: Int*)(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = 1 to 54
      weightInitialValue.toNDArray.reshape(2, 3, 3, 3).toWeight.maxPool(dimensions: _*)
    }

    val network = makeNetwork(2, 3)

    val inputData = 1 to 54

    def train() = {
      val outputBatch = network.forward(
        inputData.toNDArray.reshape(2, 3, 3, 3).toBatch
      )
      try {
        val loss = (outputBatch.value: INDArray).sumT
        outputBatch.backward(outputBatch.value)
        loss
      } finally {
        outputBatch.close()
      }
    }

    train().value should be(189.0)

    for (_ <- 0 until 30000) {
      train().value
    }

    math.abs(train().value) should be < 10.0

  }

  "INDArrayPlaceholder maxPool dimensions --three dimension" ignore {

    implicit val learningRate = new LearningRate {
      override def currentLearningRate() = 0.0003
    }

    def makeNetwork(dimensions: Int*)(implicit x: From[INDArray]##`@`) = {
      val weightInitialValue = 1 to 54
      weightInitialValue.toNDArray.reshape(2, 3, 3, 3).toWeight.maxPool(dimensions: _*)
    }

    val network = makeNetwork(1, 2, 3)

    val inputData = 1 to 54

    def train() = {
      val outputBatch = network.forward(
        inputData.toNDArray.reshape(2, 3, 3, 3).toBatch
      )
      try {
        val loss = (outputBatch.value: INDArray).sumT
        outputBatch.backward(outputBatch.value)
        loss
      } finally {
        outputBatch.close()
      }
    }

    train().value should be(81.0)

    for (_ <- 0 until 20000) {
      train().value
    }

    math.abs(train().value) should be < 10.0

  }

  "INDArrayPlaceholder shape --only forward no backward" in {

    def makeNetwork(implicit x: From[INDArray]##`@`) = {
      x.shape
    }

    def train() = {
      val outputBatch = makeNetwork.forward(
        (1 to 54).toNDArray.reshape(2, 3, 3, 3).toBatch
      )
      val seqShape = try {
        outputBatch.value
      } finally {
        outputBatch.close()
      }
      seqShape(0) should be(2)
      seqShape(1) should be(3)
      seqShape(2) should be(3)
      seqShape(3) should be(3)
    }
    train()
  }

  "INDArray convn INDArray" in {
    val input = (1 to 16).toNDArray.reshape(1, 1, 4, 4)

    def convolution(implicit input: From[INDArray]##`@`): To[INDArray]##`@` = {
      val weight: To[INDArray]##`@` = Nd4j.ones(1, 1, 3, 3)
      val bias = Nd4j.zeros(1)
      input.convn(weight, bias, (3, 3), (1, 1), (1, 1))
    }

    val result: INDArray = convolution.predict(input)

    val resultShape = result.shape.toSeq
    //noinspection ZeroIndexToHead
    resultShape(0) should be(1)
    resultShape(1) should be(1)
    resultShape(2) should be(4)
    resultShape(3) should be(4)

    val expectResult = Array(14.00, 24.00, 30.00, 22.00, 33.00, 54.00, 63.00, 45.00, 57.00, 90.00, 99.00, 69.00, 46.00,
      72.00, 78.00, 54.00).toNDArray
      .reshape(1, 1, 4, 4)

    result.eq(expectResult).sumT should be(16)

  }

}
