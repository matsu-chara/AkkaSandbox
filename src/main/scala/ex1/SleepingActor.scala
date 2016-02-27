package ex1

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object Main3 {
  def main(args: Array[String]) {
    val system = ActorSystem("mofu")
    implicit val ec = system.dispatcher
    implicit val to = Timeout(10.minutes)

    val refs = (1 to 100).map { i =>
      val disp = "pinned-dispatcher"
      system.actorOf(SleepingActor.props.withDispatcher(disp), s"sleeper-$i")
    }
    val fs = Future.sequence(for {
      i <- 1 to 3
      (ref, j) <- refs.zipWithIndex
    } yield (ref ? (j, i)).mapTo[Int])

    Await.ready(fs, Duration.Inf)
    Await.result(system.terminate(), Duration.Inf)
  }
}

class SleepingActor extends Actor {
  println(Thread.currentThread().getName)

  def receive: Receive = {
    case (actorNum:Int, count: Int) =>
      println(s"sleep: アクター$actorNum ${count}回目")
      Thread.sleep(1000)
      println(s"wakeup: アクター$actorNum ${count}回目")
      sender ! actorNum * count
  }
}

object SleepingActor {
  def props = Props(classOf[SleepingActor])
}