package ex1

import akka.actor.ActorSystem
import akka.testkit.{EventFilter, TestActorRef}
import org.scalatest.{BeforeAndAfterAll, FunSpec}

import scala.concurrent.Await
import scala.concurrent.duration.Duration


class BackOffSupervisorActorSpec extends FunSpec with BeforeAndAfterAll {
  implicit val system = ActorSystem("testsystem")

  override def afterAll = {
    Await.result(system.terminate(), Duration.Inf)
  }

  describe("when it starts with 0") {
    it("throws exceptions until state up to 10") {
      val actor = TestActorRef(BackOffSupervisorActor.props)
      EventFilter[RuntimeException](occurrences = 10) intercept {
        (1 to 11).foreach{ actor ! _ }
      }
    }
  }
}
