package ru.beeline.vafs
package callcontrol.process

import callcontrol.counter.{CounterCall, Start}

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.Behavior
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.persistence.typed.PersistenceId
import akka.stream.scaladsl.{Flow, Sink}
import com.typesafe.config.ConfigFactory
import io.circe.jawn
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import ru.beeline.vafs.callcontrol.Call

object ConsumerCall {

  def apply(persId: PersistenceId): Behavior[NotUsed] =
    Behaviors.setup { ctx =>
      implicit val system = ctx.system

      val config = ConfigFactory.load()

      val consumerConfig   = config.getConfig("akka.kafka.consumer")
      consumerConfig.entrySet().forEach(el => println(el))

      val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

      val handlerRules = ctx.spawn(HandlerRules(persId), "HandlerRules")

      val counterCallWriter = ctx.spawn(CounterCall(persId), s"CounterCallWriter")

      ctx.spawn(ConsumerRules(handlerRules), "consumerRules")

      val parserMsg = Flow[ConsumerRecord[String, String]].map { msg =>
        val call = jawn.decode[Call](msg.value())
        call match {
          case Right(value) =>
            counterCallWriter ! Start(value)
            handlerRules ! value
          case Left(err)    =>
            ctx.log.error(s"Failed parse msg: ${err.getMessage}")
        }
      }

      Consumer
        .plainSource(consumerSettings, Subscriptions.topics("call-in"))
        .async
        .via(parserMsg)
        .async
        .runWith(Sink.ignore)

      Behaviors.same
    }
}
