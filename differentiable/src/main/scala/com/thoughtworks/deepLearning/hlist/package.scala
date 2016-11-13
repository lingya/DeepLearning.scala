package com.thoughtworks.deepLearning
//
//import com.thoughtworks.deepLearning.NeuralNetwork.{NeuralNetwork.Aux, IsNeuralNetwork}
//import hlist.ast._
//import any._
//import com.thoughtworks.deepLearning.any.ast.Identity
//
import com.thoughtworks.deepLearning.Batch.Aux
import com.thoughtworks.deepLearning.NeuralNetwork.Aux
import com.thoughtworks.deepLearning.hlist.ast._

import scala.language.implicitConversions
import scala.language.existentials

/**
  * @author 杨博 (Yang Bo) &lt;pop.atry@gmail.com&gt;
  */
package object hlist {

  /** @template */
  type HList = Type[_ <: shapeless.HList, _ <: shapeless.Coproduct]

  /** @template */
  type HNil = Type[shapeless.HNil, shapeless.CNil]
  val HNil: HNil = implicitly

  /** @template */
  type ::[Head <: Type[_, _], Tail <: HList] =
    Type[shapeless.::[head.Data, tail.Data], shapeless.:+:[head.Delta, tail.Delta]] forSome {
      val head: Head
      val tail: Tail
    }

  implicit final class RichHListType[TailData <: shapeless.HList, TailDelta <: shapeless.Coproduct](
      tail: Type[TailData, TailDelta]) {
    def ::[HeadData, HeadDelta](head: Type[HeadData, HeadDelta]) =
      new Type[shapeless.::[HeadData, TailData], shapeless.:+:[HeadDelta, TailDelta]]
  }
//
//  implicit final class HListOps[TailAst](val tail: TailAst) {
//
//    def ::[Input0 <: Batch,
//           HeadAst,
//           HeadData,
//           HeadDelta,
//           TailData <: shapeless.HList,
//           TailDelta <: shapeless.Coproduct](head: HeadAst)(
//        implicit unapplyHead: IsNeuralNetwork[HeadAst, Input0, HeadData, HeadDelta],
//        unapplyTail: IsNeuralNetwork[TailAst, Input0, TailData, TailDelta]
//    ): NeuralNetwork.Aux[Input0, Batch.Aux[shapeless.::[HeadData, TailData], shapeless.:+:[HeadDelta, TailDelta]]] = {
//      HCons[Input0, HeadData, HeadDelta, TailData, TailDelta](unapplyHead(head), unapplyTail(tail))
//    }
//
//  }

  final class HConsOps[Input <: Batch, HeadData, HeadDelta, TailData <: shapeless.HList,
  TailDelta <: shapeless.Coproduct](
      hcons: NeuralNetwork.Aux[Input, Batch.Aux[shapeless.::[HeadData, TailData], shapeless.:+:[HeadDelta, TailDelta]]]) {
    def head: NeuralNetwork.Aux[Input, Type[HeadData, HeadDelta]#Batch] =
      Head[Input, HeadData, HeadDelta, TailData, TailDelta](hcons)

    def tail: NeuralNetwork.Aux[Input, Type[TailData, TailDelta]#Batch] =
      Tail[Input, HeadData, HeadDelta, TailData, TailDelta](hcons)
  }

  implicit def toHConsOps[From,
                          Input <: Batch,
                          OutputData,
                          OutputDelta,
                          HeadData,
                          HeadDelta,
                          TailData <: shapeless.HList,
                          TailDelta <: shapeless.Coproduct](from: From)(
    implicit isNeuralNetwork: IsNeuralNetwork.Aux[From, Input, OutputData, OutputDelta],
    toHListAst: NeuralNetwork.Aux[Input, Batch.Aux[OutputData, OutputDelta]] <:< NeuralNetwork.Aux[
        Input,
        Batch.Aux[shapeless.::[HeadData, TailData], shapeless.:+:[HeadDelta, TailDelta]]]
  ): HConsOps[Input, HeadData, HeadDelta, TailData, TailDelta] = {
    new HConsOps[Input, HeadData, HeadDelta, TailData, TailDelta](toHListAst(isNeuralNetwork(from)))
  }

}
