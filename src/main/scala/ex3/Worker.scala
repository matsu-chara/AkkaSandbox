package ex3

import akka.actor._
import ex3.RootActor.Start
import ex3.Worker.Increment

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
  val propsForActorName =  topics.map { t => Worker.name(t) -> Worker.props(t) }.toMap

  override def supervisorStrategy = SupervisorStrategy.stoppingStrategy

  def receive = {
    case Start =>
      propsForActorName.foreach { case (t, p) =>
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
  private case object Increment
  def name(topic: String) = s"consumer-$topic"
  def props(topic: String) = Props(classOf[Worker], topic)
}

class Worker(topic: String) extends Actor {
  var i = 0
  val cancel = context.system.scheduler.schedule(0.seconds, (Random.nextInt() % 200).milliseconds, self, Increment)(context.dispatcher)

  override def preStart() = {
    super.preStart()
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

  def receive = {
    case Increment =>
      i = i + 1
      self ! i
    case 4 if topic == "mebaru" => throw new RuntimeException("接続切れた")
    case x => println(topic + " => " + x)
  }
}