package ru.beeline.vafs
package callcontrol.process

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.scaladsl.{Sink, Source}
import com.typesafe.config.ConfigFactory
import io.circe.syntax.EncoderOps
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import ru.beeline.vafs.callcontrol.{CommandCall, ResponseForCall}

object ProducerResponse {

  val config = ConfigFactory.load()

  val producerConfig = config.getConfig("akka.kafka.producer")
  val producerSettings = ProducerSettings(producerConfig, new StringSerializer, new StringSerializer)

  def apply(): Behavior[CommandCall] = Behaviors.setup { ctx =>

    implicit val system = ctx.system

    Behaviors.receiveMessage {
      case callResponse @ ResponseForCall(transactionId, response) =>

        Source
          .single(ProducerMessage
            .single(new ProducerRecord[String, String]("call-response", callResponse.asJson.noSpaces)))
          .via(Producer.flexiFlow(producerSettings))
          .runWith(Sink.head)

            Behaviors.same
    }
  }
}
