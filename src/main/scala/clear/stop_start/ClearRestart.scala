package clear.stop_start

import akka.actor.SupervisorStrategy._
import akka.actor._
import clear.WorkerActor.{Running, Starting, State}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ClearRestart extends App {
  val system = ActorSystem("clear", ConfigFactory.parseString("akka.log-dead-letters = 0")) // deadletter logをオフ（システムグローバルにオフだとちょっと不安・・）
  val actor = system.actorOf(Props[SupervisorActor], "supervisor")

  try {
    Thread.sleep(200)
    (1 to 10).foreach(actor ! _)  // 10を受け取ると例外が出てworkerがrestartされる
    (11 to 20).foreach(actor ! _) // restart時のconnectより先に積まれるのでunhandled

    Thread.sleep(1000)
    (21 to 30).foreach(actor ! _) // 以前のjobは実行されなくてもいいので、unhandledにならないで欲しい。そして、準備が出来たらそこから先のジョブは実行して欲しい
  } finally {
    Thread.sleep(3000)
    Await.result(system.terminate(), Duration.Inf)
  }
}

class SupervisorActor extends Actor {
  private var actor = createWorker()

  private def createWorker() = {
    val a = context.actorOf(Props[WorkerActor], "worker")
    context.watch(a)
    a
  }

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException ⇒ Restart
    case _: ActorKilledException ⇒ Stop
    case _: DeathPactException ⇒ Stop
    case _: Exception ⇒ Stop // 止めると未処理メッセージがdeadletterになる
  }

  def receive: Receive = {
    case Terminated(ref) =>
      actor = createWorker()
    case x => actor forward x
  }
}

class WorkerActor extends FSM[State, Int] {
  startWith(Starting, 0)

  self ! "connect"

  when(Starting) {
    case Event("connect", _) =>
      println("connect")
      goto(Running)
  }

  when(Running) {
    case Event(10, _) =>
      throw new RuntimeException("die")
      stay()
    case Event(x, _) =>
      Thread.sleep(100)
      println(x)
      stay()
  }

  whenUnhandled {
    case _ =>
      println("unhandledになったのでstopします")
      stop()
  }
}

object WorkerActor {
  sealed trait State
  case object Starting extends State
  case object Running extends State
}
