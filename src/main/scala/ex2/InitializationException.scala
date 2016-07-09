package ex2

import akka.actor.SupervisorStrategy._
import akka.actor._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main1 extends App {
  val system = ActorSystem()
  try {
    val actor = system.actorOf(Props[SupervisorExceptionActor])
    actor ! 1
    actor ! 2
    Thread.sleep(1000)
    (3 to 10).foreach {actor ! _}
  } finally {
    Thread.sleep(1000)
    Await.result(system.terminate(), Duration.Inf)
  }
}

class SupervisorExceptionActor extends Actor {
  val actor = context.actorOf(Props[InitializationException])

  override def supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 2) {
    case _: ActorInitializationException ⇒ Restart
    case _: ActorKilledException         ⇒ Stop
    case _: DeathPactException           ⇒ Stop
    case _: Exception                    ⇒ Restart
  }

  def receive = {
    case Terminated(ref: ActorRef) => println(ref)
    case x => actor forward x
  }
}

class InitializationException extends Actor {
  override def preStart() = {
    super.preStart()
    self ! "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"
    println("starting")
  }

  override def postStop() = {
    super.postStop()
    println("postStop")
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
    self ! "yyyyyyyyyyyyyyyyyyyyyyyy"
    println("preRestart")
  }

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
    println("postRestart")
  }

  def receive = {
    case 1 =>
      println(1)
      Thread.sleep(1000)
    case 2 =>
      println(2)
      throw new RuntimeException("a")
    case x =>
      println(x)
  }
}