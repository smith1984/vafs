package ru.beeline.vafs
package backoffice.producer

import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import ru.beeline.vafs.backoffice.configuration.BackOfficeConfig
import ru.beeline.vafs.backoffice.dao.entities.Rule
import ru.beeline.vafs.backoffice.dto.RuleDTOKafka
import zio.blocking.Blocking
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde.Serde
import zio.random.Random
import zio.{Has, RIO, UIO, ULayer, ZIO, ZLayer}

object RuleProducer {

  val producer = ZLayer.fromServiceManaged[BackOfficeConfig, Any, Throwable, Producer.Service[Any, String, String]]{
    backOfficeConfig =>
      val bs = backOfficeConfig.backoffice.producer.kafkaClient.bootstrapServers.split(",").toList
      val producerSettings = ProducerSettings(bs)
      Producer.make[Any, String, String](producerSettings, Serde.string, Serde.string)
  }

  type RuleProducer = Has[Service]

  type RuleProducerEnv = Any with Blocking with Random with Producer[Any, String, String]

  trait Service {
    def sendMsg(rule: Rule, lstA: List[Long], lstB: List[Long], opr: String): ZIO[RuleProducerEnv, Throwable, Unit]
  }

  class Impl(backOfficeConfig: BackOfficeConfig) extends Service {

    override def sendMsg(
      rule: Rule,
      lstA: List[Long],
      lstB: List[Long],
      opr: String
    ): ZIO[RuleProducerEnv, Throwable, Unit] =
      for {
        uuid <- zio.random.nextUUID.map(_.toString())
        msg   = RuleDTOKafka.from(rule, lstA, lstB, opr).asJson.noSpaces
        topic = backOfficeConfig.backoffice.producer.kafkaClient.topic
        _    <- Producer.produce[Any, String, String](topic, uuid, msg)
      } yield ()
  }

  val live = ZLayer.fromService[BackOfficeConfig, RuleProducer.Service]{cfg => new Impl(cfg)}


}
