package com.thoughtworks.deepLearning.any.ast

import com.thoughtworks.deepLearning.{Batch, BatchId, NeuralNetwork}

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
final case class Throw(throwable: () => Throwable) extends NeuralNetwork with Batch with BatchId {
  override type Input = Batch
  override type Output = Batch.Aux[scala.Nothing, scala.Any]
  override type Open = Output
  override type Data = scala.Nothing
  override type Delta = scala.Any

  override def forward(input: BatchId.Aux[Input]) = {
    this
  }

  override def backward(delta: Delta): Unit = {}

  override def value: Data = {
    throw throwable()
  }

  override def close(): Unit = {}

  override def open() = this
}
