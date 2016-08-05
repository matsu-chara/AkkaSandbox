package order

import akka.actor.SupervisorStrategy.{Restart, Stop}
import akka.actor.{Actor, ActorSystem, OneForOneStrategy, Props}
import order.JobSupervisor2.{AckAndNext, NackAndRetry}

import scala.collection.immutable.Queue
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.Random

object OrderMain2 extends App {
  implicit val system = ActorSystem()
  val actor = system.actorOf(Props[RootActor2], "root")
  (1 to 10).foreach(actor ! _)

  Thread.sleep(300)

  (11 to 20).foreach(actor ! _)

  actor ! "AllJobProduced"
  Await.result(system.whenTerminated, Duration.Inf)
}

class RootActor2 extends Actor {

  val actor = context.actorOf(JobSupervisor2.props, "manager")

  override def receive = {
    case "AllJobFinished" => context.system.terminate
    case x => actor forward x
  }
}

class JobSupervisor2 extends Actor {
  override val supervisorStrategy = OneForOneStrategy() {
    case _: Exception =>
      self ! NackAndRetry
      Restart
    case _ => Stop
  }

  val worker = context.actorOf(OneWorker2.props(new Connection2), "worker")
  var jobs = Queue[Int]()
  var isAllJobProduced = false

  // たかだか一つのjobをworkerに与える
  override def receive = {
    case "AllJobProduced" =>
      isAllJobProduced = true
      finishCheck()
    case x: Int =>
      // jobが無いとackの時に次が渡せないのでここで補填
      if (jobs.isEmpty) {
        worker ! x
      }
      addJob(x: Int)
    case AckAndNext =>
      removeOldJob()
      produceNext()
      finishCheck()
    case NackAndRetry => produceNext()
  }

  private def addJob(x: Int) = {
    jobs = jobs.enqueue(x)
  }

  private def removeOldJob() = {
    jobs = jobs.dequeue._2
  }

  private def produceNext() = jobs.headOption.foreach(worker ! _)
  private def finishCheck() = if (isAllJobProduced && jobs.isEmpty) context.parent ! "AllJobFinished"

}

object JobSupervisor2 {
  def props = Props[JobSupervisor2]
  case object AckAndNext
  case object NackAndRetry
}

class OneWorker2(connection: Connection2) extends Actor {

  override def receive = {
    case x: Int =>
      connection.doJob(x)
      sender ! AckAndNext
  }

  override def preStart(): Unit = {
    super.preStart()

    if (!connection.isConnected) {
      connection.connect()
    }
  }
}

object OneWorker2 {
  def props(connection: Connection2) = Props(classOf[OneWorker2], connection)
}

class Connection2 {
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
