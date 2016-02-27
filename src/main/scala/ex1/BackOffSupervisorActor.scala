package ex1

import java.util.concurrent.atomic.AtomicInteger

import akka.actor._
import akka.pattern.{Backoff, BackoffSupervisor}

import scala.concurrent.Await
import scala.concurrent.duration._

object Main1 {
  val x = new AtomicInteger(0)

  def main(args: Array[String]) {
    val system = ActorSystem("mofu")
    val ahoProp = BackoffSupervisor.props(
      Backoff.onFailure(
        childProps = BackOffSupervisorActor.props,
        childName = "aho",
        minBackoff = 1.milliseconds,
        maxBackoff = 2.seconds,
        randomFactor = 0
        )
    )
    val ref = system.actorOf(ahoProp)
    (1 to 100).foreach { x =>
      ref ! x
      Thread.sleep(100)
    }
    Await.result(system.whenTerminated, Duration.Inf)
  }
}

class BackOffSupervisorActor extends Actor {
  import Main1.x
  def receive: Receive = {
    case _ if x.get() < 10 =>
      x.incrementAndGet()
      throw new RuntimeException(s"restarted ${x.get()} at ${System.currentTimeMillis()}")
    case _ =>
      println("yeah!")
      context.system.terminate()
  }
}
object BackOffSupervisorActor {
  def props = Props(classOf[BackOffSupervisorActor])
}