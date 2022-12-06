package ru.beeline.vafs
package backoffice

import com.zaxxer.hikari.HikariDataSource
import io.getquill.{Escape, JdbcContextConfig, Literal, NamingStrategy, PostgresZioJdbcContext, SnakeCase}
import io.getquill.context.ZioJdbc
import io.getquill.util.LoadConfig
import liquibase.Liquibase
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.{ClassLoaderResourceAccessor, CompositeResourceAccessor, FileSystemResourceAccessor}
import ru.beeline.vafs.backoffice.configuration.{BackOfficeConfig, Configuration}
import zio.macros.accessible
import zio.{Has, RIO, URIO, ZIO, ZLayer, ZManaged, _}

package object db {

  type DataSource = Has[javax.sql.DataSource]

  object Ctx extends PostgresZioJdbcContext(NamingStrategy(Escape, Literal))

  def hikariDS: HikariDataSource = new JdbcContextConfig(LoadConfig("backoffice.db")).dataSource

  val zioDS: ZLayer[Any, Throwable, DataSource] = ZioJdbc.DataSourceLayer.fromDataSource(hikariDS)


  @accessible
  object LiquibaseService {

    type LiquibaseService = Has[Service]

    type Liqui = Has[Liquibase]

    trait Service {
      def performMigration: RIO[Liqui, Unit]
    }

    class Impl extends Service {

      override def performMigration: RIO[Liqui, Unit] = liquibase.map(_.update("dev"))
    }
     
    def mkLiquibase(config: BackOfficeConfig): ZManaged[DataSource, Throwable, Liquibase] = for {
      ds <- ZIO.environment[DataSource].map(_.get).toManaged_
      fileAccessor <-  ZIO.effect(new FileSystemResourceAccessor()).toManaged_
      classLoader <- ZIO.effect(classOf[LiquibaseService].getClassLoader).toManaged_
      classLoaderAccessor <- ZIO.effect(new ClassLoaderResourceAccessor(classLoader)).toManaged_
      fileOpener <- ZIO.effect(new CompositeResourceAccessor(fileAccessor, classLoaderAccessor)).toManaged_
      jdbcConn <- ZManaged.makeEffect(new JdbcConnection(ds.getConnection()))(c => c.close())
      liqui <- ZIO.effect(new Liquibase(config.backoffice.liquibase.changeLog, fileOpener, jdbcConn)).toManaged_
    } yield liqui

    val liquibaseLayer: ZLayer[Configuration with DataSource, Throwable, Liqui] = ZLayer.fromManaged(
      for {
        config <- zio.config.getConfig[BackOfficeConfig].toManaged_
        liquibase <- mkLiquibase(config)
      } yield (liquibase)
    )

    def liquibase: URIO[Liqui, Liquibase] = ZIO.service[Liquibase]

    val live: ULayer[LiquibaseService] = ZLayer.succeed(new Impl)

  }
}
