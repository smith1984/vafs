package ru.beeline.vafs
package backoffice.services

import ru.beeline.vafs.backoffice.dao.entities.{ListNumberId, ListWithNumber, Rule, RuleId}
import ru.beeline.vafs.backoffice.dao.repositories.{ListNumberRepository, RuleRepository}
import ru.beeline.vafs.backoffice.db
import ru.beeline.vafs.backoffice.db.DataSource
import ru.beeline.vafs.backoffice.dto.{RuleDTO, RuleIdDTO}
import ru.beeline.vafs.backoffice.producer.RuleProducer
import zio.{Has, RIO, ZIO, ZLayer}
import zio.macros.accessible
import zio.random.Random

import java.sql.SQLException

@accessible
object RuleService {

  type RuleService = Has[Service]

  type RuleServiceEnv = RuleProducer.RuleProducerEnv with DataSource with Random

  trait Service {

    def find(ruleId: RuleId): ZIO[DataSource, Serializable, (RuleIdDTO, RuleDTO)]

    def insert(ruleDTO: RuleDTO): ZIO[RuleServiceEnv, Throwable, (String, String, String)]

    def update(ruleId: RuleId, ruleDTO: RuleDTO): ZIO[RuleServiceEnv, Serializable, Unit]

    def delete(ruleId: RuleId): ZIO[RuleServiceEnv, Serializable, Unit]
  }

  class Impl(ruleRepository: RuleRepository.Service,
             listNumberRepository: ListNumberRepository.Service,
             ruleProducer: RuleProducer.Service)
      extends Service {

    val ctx = db.Ctx

    override def find(ruleId: RuleId): ZIO[DataSource, Serializable, (RuleIdDTO, RuleDTO)] = for {
      rule   <- ruleRepository.find(ruleId).some
      lstA   <- listNumberRepository.find(ListNumberId(rule.lstAId))
      lstARes = lstA.map(_.number)
      lstB   <- listNumberRepository.find(ListNumberId(rule.lstBId))
      lstBRes = lstB.map(_.number)
    } yield (RuleIdDTO(ruleId.id), RuleDTO.from(rule, lstARes, lstBRes))

    override def insert(ruleDTO: RuleDTO): ZIO[RuleServiceEnv, Throwable, (String, String, String)] = for {
      uuidRule <- zio.random.nextUUID.map(_.toString())
      uuidLstA <- zio.random.nextUUID.map(_.toString())
      uuidLstB <- zio.random.nextUUID.map(_.toString())
      rule      = Rule.from(uuidRule, uuidLstA, uuidLstB, ruleDTO)
      lstA      = ListWithNumber.to(ListWithNumber(uuidLstA, ruleDTO.lstA))
      lstB      = ListWithNumber.to(ListWithNumber(uuidLstB, ruleDTO.lstB))
      _        <- ctx.transaction(
                    for {
                      _ <- ruleRepository.insert(rule)
                      _ <- listNumberRepository.insert(lstA)
                      _ <- listNumberRepository.insert(lstB)
                    } yield ()
                  )
      _ <- ruleProducer.sendMsg(rule, lstA.map(_.number), lstB.map(_.number), "activate").debug("producer")
    } yield (uuidRule, uuidLstA, uuidLstB)

    override def update(ruleId: RuleId, ruleDTO: RuleDTO): ZIO[RuleServiceEnv, Serializable, Unit] = for {
      ruleOld <- ruleRepository.find(ruleId).some
      uuidRule = ruleOld.id
      lstAId   = ListNumberId(ruleOld.lstAId)
      lstBId   = ListNumberId(ruleOld.lstBId)
      rule     = Rule.from(uuidRule, lstAId.id, lstBId.id, ruleDTO)
      lstA     = ListWithNumber.to(ListWithNumber(lstAId.id, ruleDTO.lstA))
      lstB     = ListWithNumber.to(ListWithNumber(lstBId.id, ruleDTO.lstB))
      _       <- ctx.transaction(
                   for {
                     _ <- ruleRepository.update(rule)
                     _ <- listNumberRepository.delete(lstAId)
                     _ <- listNumberRepository.delete(lstBId)
                     _ <- listNumberRepository.insert(lstA)
                     _ <- listNumberRepository.insert(lstB)
                   } yield ()
                 )
      _ <- ruleProducer.sendMsg(rule, lstA.map(_.number), lstB.map(_.number), "update")
    } yield ()

    override def delete(ruleId: RuleId): ZIO[RuleServiceEnv, Serializable, Unit] = for {
      rule  <- ruleRepository.find(ruleId).some
      lstAId = ListNumberId(rule.lstAId)
      lstBId = ListNumberId(rule.lstBId)
      lstA <- listNumberRepository.find(lstAId)
      lstB <- listNumberRepository.find(lstBId)
      _     <- ctx.transaction(
                 for {
                   _ <- ruleRepository.delete(ruleId)
                   _ <- listNumberRepository.delete(lstAId)
                   _ <- listNumberRepository.delete(lstBId)
                 } yield ()
               )
      _ <- ruleProducer.sendMsg(rule, lstA.map(_.number), lstB.map(_.number), "deactivate")
    } yield ()
  }

  val live: ZLayer[
    RuleRepository.RuleRepository with ListNumberRepository.ListNumberRepository with RuleProducer.RuleProducer,
    Nothing,
    RuleService.RuleService
  ] =
    ZLayer.fromServices[RuleRepository.Service, ListNumberRepository.Service, RuleProducer.Service, RuleService.Service](
      (ruleRepo, lstNumberRepo, ruleProducer) => new Impl(ruleRepo, lstNumberRepo, ruleProducer)
    )
}
