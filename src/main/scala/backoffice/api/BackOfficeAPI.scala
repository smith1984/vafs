package ru.beeline.vafs
package backoffice.api

import io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import ru.beeline.vafs.backoffice.dao.entities.RuleId
import ru.beeline.vafs.backoffice.dto.RuleDTO
import ru.beeline.vafs.backoffice.services.RuleService
import zio.ZIO
import zhttp.http._


object BackOfficeAPI {

  val api = Http.collectZIO[Request] {
    case Method.GET -> !! / "rule" / id =>
      RuleService.find(RuleId(id)).foldM(
        err => ZIO.succeed(Response.status(Status.NotFound)),
        result => ZIO.succeed(Response.json(result.asJson.noSpaces))
      )

    case req@Method.POST -> !! / "insert" =>
      (for {
        r <- req.bodyAsString
        dto <- ZIO.fromEither(decode[RuleDTO](r))
        result <- RuleService.insert(dto)
      } yield result).foldM(
        err => ZIO.succeed(Response.status(Status.BadRequest)),
        result => ZIO.succeed(Response.json(result.asJson.noSpaces))
      )

    case req@Method.PUT -> !! / "update" / id  => (for {
      r <- req.bodyAsString
      dto <- ZIO.fromEither(decode[RuleDTO](r))
      _ <- RuleService.update(RuleId(id), dto)
    } yield ()).foldM(
      err => ZIO.succeed(Response.status(Status.BadRequest)),
      result => ZIO.succeed(Response.ok)
    )

    case Method.DELETE -> !! / "delete" / id => RuleService.delete(RuleId(id)).foldM(
      err => ZIO.succeed(Response.status(Status.BadRequest)),
      result => ZIO.succeed(Response.ok)
    )

  }
}
