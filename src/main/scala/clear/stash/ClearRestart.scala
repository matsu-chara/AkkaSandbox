package clear.stash

import akka.actor.SupervisorStrategy._
import akka.actor._
import clear.WorkerActor.{Running, Starting, State}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ClearRestart extends App {
  val system = ActorSystem("clear")
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
  private val actor = context.actorOf(Props[WorkerActor], "worker")

  override def supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
    case _: ActorInitializationException ⇒ Restart
    case _: ActorKilledException ⇒ Stop
    case _: DeathPactException ⇒ Stop
    case _: Exception ⇒ Restart
  }

  def receive: Receive = {
    case x => actor forward x
  }
}

class WorkerActor extends FSM[State, Int] with Stash {
  startWith(Starting, 0)

  self ! "connect"

  when(Starting) {
    case Event("connect", _) =>
      println("connect")
      unstashAll() // 古いジョブを実行したい場合はこちら。unstashしない場合、clearする方法は無さそうなのでいずれStashOverFlowになる？
      goto(Running)
    case Event(_, _) =>
      println("stashします")
      stash()
      stay()
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
