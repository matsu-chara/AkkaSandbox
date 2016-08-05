package order

import akka.actor.{Actor, ActorSystem, Props}
import akka.contrib.mailbox.PeekMailboxExtension
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

object OrderMain3 extends App {
  implicit val system = ActorSystem("MySystem", ConfigFactory.parseString(
    """
    peek-dispatcher {
      mailbox-type = "akka.contrib.mailbox.PeekMailboxType"
      max-retries = 2
    }
    """))

  val actor = system.actorOf(Props[RootActor3], "root")
  (1 to 10).foreach(actor ! _)

  Thread.sleep(300)

  (11 to 20).foreach(actor ! _)

  actor ! "AllJobProduced"
  Await.result(system.whenTerminated, Duration.Inf)
}

class RootActor3 extends Actor {

  val actor = context.actorOf(JobSupervisor3.props, "manager")

  override def receive = {
    case "AllJobFinished" => context.system.terminate
    case x => actor forward x
  }
}

class JobSupervisor3 extends Actor {

  val worker = context.actorOf(OneWorker3.props(new Connection).withDispatcher("peek-dispatcher"), "worker")
  var isAllJobProduced = false

  override def receive = {
    case "AllJobProduced" =>
      isAllJobProduced = true
      finishCheck()
    case x: Int =>
      worker ! x
      finishCheck()
  }

  private def finishCheck() = if (isAllJobProduced) context.parent ! "AllJobFinished"

}

object JobSupervisor3 {
  def props = Props[JobSupervisor3]
}

class OneWorker3(connection: Connection) extends Actor {

  override def receive = {
    case x: Int =>
      connection.doJob(x)
      PeekMailboxExtension.ack()
  }

  override def preStart(): Unit = {
    super.preStart()

    if (!connection.isConnected) {
      connection.connect()
    }
  }
}

object OneWorker3 {
  def props(connection: Connection) = Props(classOf[OneWorker3], connection)
}

class Connection3 {
  var isConnected = false

  def connect() = isConnected = true

  def close() = isConnected = false

  def doJob(x: Int) = {
    if (!isConnected) {
      throw new RuntimeException(s"Connection already closed. message = $x")
    }

    if (x % 5 == Random.nextInt(3)) {
      isConnected = false
      throw new RuntimeException(s"unlucky number $x. connection was reset")
    }

    println(s"the answer is $x")
  }
}
