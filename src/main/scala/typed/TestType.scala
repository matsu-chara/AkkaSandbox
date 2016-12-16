package typed

import akka.typed._
import akka.typed.ScalaDSL._
import akka.typed.AskPattern._
import typed.HelloWorld.{Greet, Greeted}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await

object HelloWorld {
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String)

  val greeter = Static[Greet] { msg =>
    println(s"Hello ${msg.whom}")
    msg.replyTo ! Greeted(msg.whom)
  }
}

object TestType extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val to = akka.util.Timeout(3, java.util.concurrent.TimeUnit.SECONDS)
  val system: ActorSystem[Greet] = ActorSystem("hello", Props(HelloWorld.greeter))

  val future: Future[Greeted] = system ? (Greet("world", _))

  for {
    greeting <- future.recover { case ex => ex.getMessage }
    done <- { println(s"result: $greeting"); system.terminate() }
  } println("system terminated")
}
