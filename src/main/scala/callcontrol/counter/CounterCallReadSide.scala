package ru.beeline.vafs
package callcontrol.counter

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.persistence.cassandra.query.scaladsl.CassandraReadJournal
import akka.persistence.query.{EventEnvelope, PersistenceQuery}
import akka.persistence.typed.PersistenceId
import akka.stream.alpakka.slick.scaladsl.{Slick, SlickSession}
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

case class CounterCallReadSide(system: ActorSystem[Nothing], persId: PersistenceId) {

  implicit val materializer = system.classicSystem

  implicit val session = SlickSession.forConfig("slick-postgres")
  import session.profile.api._

  val query = sql"""select
            write_side_offset,
            route,
            count
        from
            public.counter
        where
            id = ${persId.id}"""

  val result: Future[ListBuffer[(Long, String, Long)]] = Slick
    .source(query.as[(Long, String, Long)])
    .async
    .runWith(
      Sink.fold(ListBuffer.empty[(Long, String, Long)])((acc, next: (Long, String, Long)) => acc += next)
    )

  val stateCounter: ListBuffer[(Long, String, Long)] = Await.result(result, 10.second)

  val startOffset = stateCounter.map(_._1).max + 1

  val readJournal: CassandraReadJournal =
    PersistenceQuery(system).readJournalFor[CassandraReadJournal](CassandraReadJournal.Identifier)

  val source: Source[EventEnvelope, NotUsed] = readJournal
    .eventsByPersistenceId(persId.id, startOffset, Long.MaxValue)

  val updateState: Flow[EventEnvelope, (EventEnvelope, Boolean), NotUsed] = Flow[EventEnvelope].map { event =>
    val existsRoute: Boolean = event.event match {
      case Started(call) =>
        val route       = s"${call.numberA}_${call.numberB}"
        val existsRoute = stateCounter.map(_._2).contains(route)
        val offset      = event.sequenceNr
        if (existsRoute) {
          val oldRoute = stateCounter.filter(_._2 == route)(0)
          val newRoute = (offset, route, oldRoute._3 + 1L)
          stateCounter -= oldRoute
          stateCounter += newRoute
        } else {
          val newRoute = (offset, route, 1L)
          stateCounter += newRoute
        }

        existsRoute
    }
    (event, existsRoute)
  }

  val updateResultInDb: Flow[(EventEnvelope, Boolean), Int, NotUsed] = Slick.flow { event =>
    event._1.event match {
      case Started(call) =>
        val route       = s"${call.numberA}_${call.numberB}"
        val existsRoute = event._2
        val offset      = event._1.sequenceNr

        if (existsRoute) {
          sqlu"""update
                   public.counter
               set
                   count = count + 1,
                   write_side_offset = ${offset}
               where id =  ${persId.id} and route = ${route}"""
        } else
          sqlu"""insert into
                 public.counter
               values (${persId.id}, ${route}, 1, ${offset})"""
    }
  }

  source.async
    .via(updateState).async
    .via(updateResultInDb).async
    .runWith(Sink.ignore)
}
