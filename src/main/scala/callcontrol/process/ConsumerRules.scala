package ru.beeline.vafs
package callcontrol.process

import akka.NotUsed
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Sink}
import com.typesafe.config.ConfigFactory
import io.circe.jawn
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import ru.beeline.vafs.callcontrol.{CommandCall, Rule}

object ConsumerRules {
  def apply(parent: ActorRef[CommandCall]): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      implicit val system = ctx.system

      val config = ConfigFactory.load()

      val consumerConfig = config.getConfig("akka.kafka.consumer")
      val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

      val parserMsg = Flow[ConsumerRecord[String, String]].map { msg =>
        val rule = jawn.decode[Rule](msg.value())
        rule match {
          case Right(value) =>
            parent ! value
          case Left(err) =>
            ctx.log.error(s"Failed parse msg: ${err.getMessage}")
        }
      }

      Consumer
        .plainSource(consumerSettings, Subscriptions.topics("rules"))
        .async
        .via(parserMsg)
        .async
        .runWith(Sink.ignore)

      Behaviors.same
    }
}
