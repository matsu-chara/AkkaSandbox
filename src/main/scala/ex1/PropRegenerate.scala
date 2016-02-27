package ex1

import akka.actor.{Actor, Props, ActorSystem}
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.collection.mutable

object Main4 {
  def main(args: Array[String]) {
    val system = ActorSystem("mofu")
    val ref = system.actorOf(PropRegenerate.prop(mutable.Buffer(1,2,3)))

    (4 to 6).foreach { x => ref ! x }
    ref ! "answer"

    Await.result(system.whenTerminated, Duration.Inf)
  }
}

// restartしてもmutable.Bufferの状態はリセットされないことの確認
class PropRegenerate(xs: mutable.Buffer[Int]) extends Actor {
  def receive: Receive = {
    case x: Int =>
      xs.append(x)
      throw new RuntimeException(s"fooo! $xs")
    case "answer" =>
      println(xs)
      context.system.terminate()
  }
}

object PropRegenerate {
  def prop(xs: mutable.Buffer[Int]) = Props(classOf[PropRegenerate], xs)
}