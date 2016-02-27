package ex1

import java.util.concurrent.atomic.AtomicReference

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.{CircuitBreaker, ask, pipe}
import akka.util.Timeout
import ex1.CircuitBreakerActor.{Broken, BreakState, Ok}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.control.NonFatal

object Main2 {
  val cbDispatcher = "pinned-dispatcher"

  def main(args: Array[String]) {
    val system = ActorSystem("mofu")
    implicit val to = Timeout(1.minutes)
    implicit val ec = system.dispatcher

    val ref = system.actorOf(CircuitBreakerActor.props.withDispatcher(cbDispatcher))

    val reqs = Future.sequence{
      (1 to 30).map { i =>
        Thread.sleep(100)
        (ref ? i).map(println).recover { case NonFatal(e) => println(e) }
      }
    }

    reqs.onComplete { _ => system.terminate() }
    Await.result(system.whenTerminated, Duration.Inf)
  }
}

class CircuitBreakerActor extends Actor {
  println(Thread.currentThread().getName)
  implicit val ec = context.dispatcher

  var breakState: BreakState = Ok

  val breaker = CircuitBreaker(
    scheduler = context.system.scheduler,
    maxFailures = 3,
    callTimeout = 2.seconds,
    resetTimeout = 200.millis
  )
  breaker.onClose(println("CIRCUIT_BREAKER: closed"))
  breaker.onHalfOpen(println("CIRCUIT_BREAKER: halfopend"))
  breaker.onOpen(println("CIRCUIT_BREAKER: opend"))

  def receive: Receive = {
    case i: Int =>
      if(i % 10 == 0) {
        println(breakState + "終わり " + breakState.next + "開始")
        breakState = breakState.next
      }
      pipe(breaker.withCircuitBreaker(maybeCall(i, breakState))) to sender()
  }

  def maybeCall(i: Int, state: BreakState) = Future {
    state match {
      case Ok => s"行くぜ(ง ˘ω˘ )ว $i"
      case Broken => throw new RuntimeException(s"落ちとるがな(´･_･`) $i")
    }
  }
}

object CircuitBreakerActor {
  def props = Props(classOf[CircuitBreakerActor])

  sealed trait BreakState {
    def next: BreakState = this match {
      case Ok => Broken    // 正常だったけどサーバーがおちた
      case Broken => Ok      // 完全に復帰した
    }
  }
  case object Ok extends BreakState
  case object Broken extends BreakState
}