package kairs.akka.examples

import akka.actor._
import akka.routing.FromConfig
import com.typesafe.config.ConfigFactory

/**
 * Created by mkairys on 2014.12.02.
 * Illustration of message throttling by limiting mailbox size (see bounded-mailbox.conf).
 */
object BoundedMailbox extends App {
  
  val config = ConfigFactory.load("bounded-mailbox")
  
  implicit val actorSystem = ActorSystem("AkkaTestSystem", config)

  implicit val serialActor = actorSystem.actorOf(Props(new SerialActor)
      .withDispatcher("bounded-dispatcher")
      .withRouter(FromConfig()), name = "serialActor")

  val parallelActor = actorSystem.actorOf(Props(new ParallelActor)
      .withRouter(FromConfig()), name = "parallelActor")


  for (i <- 1 to config.getInt("example.message-count")) {
    parallelActor ! ParallelMessage(i.toString)
  }
}

case class ParallelMessage(msg: String)
case class SerialMessage(msg: String)

class ParallelActor(implicit val serialActor: ActorRef) extends Actor {
  def receive = {
    case ParallelMessage(msg: String) =>
      println(s"ParallelActor(${self.path.name}) got message: $msg")
      serialActor ! SerialMessage(msg)
  }
}

class SerialActor(implicit val actorSystem: ActorSystem) extends Actor {
  def receive = {
    case SerialMessage(msg: String) =>
      println(s"SerialActor(${self.path.name}) got message: $msg")
      Thread.sleep(BoundedMailbox.config.getLong("example.serial-actor-duration"))
  }
}

