package ex3

import akka.actor._
import ex3.RootActor.Start
import ex3.Worker._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

object Main1 extends App {
  implicit val system = ActorSystem()
  val actor = system.actorOf(RootActor.props(Seq("hamachi", "mebaru", "sake", "karei", "maguro", "mebachi")), "root")
  actor ! Start

  Thread.sleep(3000)
  Await.result(system.terminate(), Duration.Inf)
}

object RootActor {
  case object Start

  def props(topics: Seq[String]) = Props(classOf[RootActor], topics)
}

class RootActor(topics: Seq[String]) extends Actor {
  val propsForActorName = topics.map { t => Worker.name(t) -> Worker.props(t) }.toMap

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  def receive = {
    case Start =>
      propsForActorName.map { case (t, p) =>
        context.watch(context.actorOf(p, t))
      }
    case Terminated(ref: ActorRef) =>
      val name = ref.path.name
      println("新しく作ります" + name)
      val prop = propsForActorName.getOrElse(name, throw new IllegalStateException(s"${name}という名前を持つアクターが存在しません。 $propsForActorName"))
      val actor = context.actorOf(prop, name)
      context.watch(actor)
  }
}

object Worker {
  case object Connect
  private case object Increment

  sealed trait State
  case object Connecting extends State
  case object Connected extends State

  case class Data(i: Int)

  def name(topic: String) = s"consumer-$topic"

  def props(topic: String) = Props(classOf[Worker], topic)
}

class Worker(topic: String) extends FSM[State, Data] {
  val cancel = context.system.scheduler.schedule(0.seconds, (Random.nextInt() % 200).milliseconds, self, Increment)(context.dispatcher)

  override def preStart() = {
    super.preStart()
    self ! Connect
  }

  override def postStop() = {
    super.postStop()
    cancel.cancel()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    super.postRestart(reason)
  }

  startWith(Connecting, Data(0))

  when(Connecting) {
    case Event(Increment, d) =>
      println("ignoring") // send nack
      stay()
    case Event(Connect, d) =>
      Thread.sleep(1000) // throw exception when timeout
      goto(Connected)
  }

  when(Connected) {
    case Event(Increment, d) =>
      self ! d.i
      stay() using d.copy(i = d.i + 1)
    case Event(4, d) if topic == "mebaru" =>
      throw new RuntimeException("接続切れた")
      stay()
    case Event(i, d) =>
      println(topic + " => " + i)
      stay()
  }
}