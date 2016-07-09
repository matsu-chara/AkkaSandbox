package stream

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.duration._

object StreamMain extends App {
  val system = ActorSystem("LifeCycleDemo")
  implicit val materializer = ActorMaterializer.create(system)

  def processingStage(name: String): Flow[String, String, NotUsed] =
    Flow[String].map { s ⇒
      println(name + " started processing " + s + " on thread " + Thread.currentThread().getName)
      Thread.sleep(100) // Simulate long processing *don't sleep in your real code!*
      println(name + " finished processing " + s)
      s
    }

  val completion = Source(List("Hello", "Streams", "World!"))
    .via(processingStage("A")).async
    .via(processingStage("B")).async
    .via(processingStage("C")).async
    .runWith(Sink.foreach(s ⇒ println("Got output " + s)))

  Await.result(completion, 3.seconds) // *don't Await in your real code!*
  system.terminate()
}
