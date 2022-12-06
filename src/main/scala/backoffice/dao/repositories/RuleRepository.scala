package ru.beeline.vafs
package backoffice.dao.repositories

import io.getquill.context.ZioJdbc._
import backoffice.dao.entities._
import backoffice.db

import zio.{Has, ULayer, ZLayer}

object RuleRepository {
  val ctx = db.Ctx
  import ctx._

  type RuleRepository = Has[Service]

  trait Service{
    def find(ruleId: RuleId): QIO[Option[Rule]]

    def insert(rule: Rule): QIO[Unit]

    def update(rule: Rule): QIO[Unit]

    def delete(ruleId: RuleId): QIO[Unit]
  }

  class Impl extends Service {

    val ruleSchema = quote{
      querySchema[Rule](""""Rule"""")
    }

    override def find(ruleId: RuleId): QIO[Option[Rule]] = ctx.run(
      ruleSchema.filter(_.id == lift(ruleId.id)).take(1)
    ).map(_.headOption)

    override def insert(rule: Rule): QIO[Unit] = ctx.run(
      ruleSchema.insert(lift(rule))
    ).unit

    override def update(rule: Rule): QIO[Unit] = ctx.run(
      ruleSchema.filter(_.id == lift(rule.id))
        .update(lift(rule))
    ).unit

    override def delete(ruleId: RuleId): QIO[Unit] = ctx.run(
      ruleSchema.filter(_.id == lift(ruleId.id))
        .delete
    ).unit
  }

  val live: ULayer[RuleRepository] = ZLayer.succeed(new Impl)
}
