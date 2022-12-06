package ru.beeline.vafs
package backoffice.dao.repositories

import io.getquill.context.ZioJdbc._
import backoffice.dao.entities._
import backoffice.db

import zio.{Has, ULayer, ZLayer}

object ListNumberRepository {

  val ctx = db.Ctx
  import ctx._

  type ListNumberRepository = Has[Service]

  trait Service {
    def find(lstId: ListNumberId): QIO[List[NumberOfList]]

    def insert(lst: List[NumberOfList]): QIO[Unit]

    def delete(lstId: ListNumberId): QIO[Unit]
  }

  class ServiceImpl extends Service {

    val listNumberSchema = quote {
      querySchema[NumberOfList](""""ListNumber"""")
    }

    override def find(lstId: ListNumberId): QIO[List[NumberOfList]] = ctx.run(
      listNumberSchema.filter(_.id == lift(lstId.id))
    )

    override def insert(lst: List[NumberOfList]): QIO[Unit] = ctx.run(
      liftQuery(lst).foreach(listNumberSchema.insert(_))
    ).unit


    override def delete(lstId: ListNumberId): QIO[Unit] = ctx.run(
      listNumberSchema.filter(_.id == lift(lstId.id))
        .delete
    ).unit
  }

  val live: ULayer[ListNumberRepository] = ZLayer.succeed(new ServiceImpl)
}
