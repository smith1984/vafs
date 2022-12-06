package ru.beeline.vafs
package backoffice

import com.typesafe.config.ConfigFactory
import zio._
import zio.config.ReadError
import zio.config.magnolia.DeriveConfigDescriptor
import zio.config.typesafe.{TypesafeConfig, TypesafeConfigSource}

package object configuration {

  case class BackOfficeConfig(backoffice: BackOffice)

  case class BackOffice(api: Api, liquibase: LiquibaseConfig, producer: Producer)

  case class LiquibaseConfig(changeLog: String)

  case class Api(host: String, port: Int)

  case class KafkaClient(bootstrapServers: String, topic: String)

  case class Producer(kafkaClient: KafkaClient)

  import zio.config.magnolia.DeriveConfigDescriptor.descriptor

  val configDescriptor: zio.config.ConfigDescriptor[BackOfficeConfig] = descriptor[BackOfficeConfig]

  type Configuration = zio.Has[BackOfficeConfig]

  object Configuration {
    val live: Layer[ReadError[String], Configuration] = TypesafeConfig.fromDefaultLoader(configDescriptor)
  }
}
