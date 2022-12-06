package ru.beeline.vafs
package backoffice

import ru.beeline.vafs.backoffice.api.BackOfficeAPI
import ru.beeline.vafs.backoffice.configuration.Configuration
import ru.beeline.vafs.backoffice.dao.repositories.{ListNumberRepository, RuleRepository}
import ru.beeline.vafs.backoffice.db.LiquibaseService.liquibaseLayer
import ru.beeline.vafs.backoffice.db.{LiquibaseService, zioDS}
import ru.beeline.vafs.backoffice.producer.RuleProducer
import ru.beeline.vafs.backoffice.producer.RuleProducer.producer
import ru.beeline.vafs.backoffice.services.RuleService

object Server {

  val appEnvironment = Configuration.live >+> zioDS >+> liquibaseLayer ++
    RuleRepository.live >+> ListNumberRepository.live >+> producer >+> RuleProducer.live >+> RuleService.live ++
    LiquibaseService.live

  val httpApp = BackOfficeAPI.api

  val server = (LiquibaseService.performMigration *> zhttp.service.Server.start(8080, httpApp))
    .provideCustomLayer(appEnvironment)

}
