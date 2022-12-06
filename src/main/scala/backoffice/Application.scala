package ru.beeline.vafs
package backoffice

import zio.{ExitCode, URIO, App}

object Application extends App {

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.server.exitCode
}
