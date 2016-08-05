package order

import akka.actor.{Actor, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

object OrderMain extends App {
  implicit val system = ActorSystem()
  implicit val executionContext = system.dispatcher
  implicit val timeout = Timeout.durationToTimeout(3.seconds)

  val actor = system.actorOf(PreRestarter.props(new Connection))
  (1 to 5).foreach(actor ! _)

  Await.result((actor ? "finished").flatMap(_ => system.terminate()), Duration.Inf)
}

class PreRestarter(connection: Connection) extends Actor {
  override def receive = {
    case "finished" => sender ! "finished"
    case x: Int => connection.doJob(x)
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    super.preRestart(reason, message)

    if (!connection.isConnected) {
      connection.connect()
    }
    message.foreach(self ! _)
  }
}

object PreRestarter {
  def props(connection: Connection) = Props(classOf[PreRestarter], connection)
}

class Connection {
  var isConnected = false

  def connect() = {
    println("connected")
    isConnected = true
  }

  def close() = {
    println("closed")
    isConnected = false
  }

  def doJob(x: Int) = {
    if (!isConnected) throw new RuntimeException(s"Connection already closed. message = $x")

    println(s"the answer is $x")
  }
}
