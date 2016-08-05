package stream2

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ThrottleMode.Shaping
import akka.stream._
import akka.stream.scaladsl._

import scala.concurrent.Await
import scala.concurrent.duration._

object StreamMain extends App {
  val system = ActorSystem("LifeCycleDemo")
  implicit val materializer = ActorMaterializer.create(system)

  val factorials = Source(1 to 100).scan(BigInt(1))((acc, next) => acc * next)

//  val result = factorials
//    .map(n => ByteString(s"$n\n"))
//    .runWith(FileIO.toPath(Paths.get("factorials.txt")))

  val done = factorials
    .zipWith(Source(0 to 100))((num, idx) => s"$idx! = $num")
    .throttle(1, 1.second, 1, Shaping)
    .runForeach(println)


  Await.result(done, 3.seconds) // *don't Await in your real code!*
  system.terminate()
}

object St3 extends App {
  final case class Author(handle: String)
  final case class Hashtag(name: String)
  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body.split(" ").collect { case t if t.startsWith("#") => Hashtag(t) }.toSet
  }
  val akka = Hashtag("#akka")

  implicit val system = ActorSystem("reactive-tweets")
  implicit val materializer = ActorMaterializer()

//  val tweets: Source[Tweet, NotUsed]

}