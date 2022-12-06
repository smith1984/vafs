package ru.beeline.vafs
package callcontrol.counter

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior, RetentionCriteria}

object CounterCall {

  def apply(persId: PersistenceId): Behavior[Command] =
    Behaviors.setup { ctx =>
      EventSourcedBehavior[Command, Event, State](
        persistenceId = persId,
        State.empty,
        (state, command) => handleCommand(state, command, ctx),
        (state, event) => handleEvent(state, event, ctx)
      )
        .snapshotWhen {
          case (_, _, seqNumber) if seqNumber % 10 == 0 => true
          case (_, _, _) => false
        }
        .withRetention(RetentionCriteria.snapshotEvery(numberOfEvents = 100, keepNSnapshots = 2))
    }

  def handleCommand(
    state: State,
    command: Command,
    ctx: ActorContext[Command]
  ): Effect[Event, State] =
    command match {
      case Start(call) =>
        ctx.log.info(s"Receive calling: ${call.numberA}_${call.numberB}")
        val started = Started(call)
        Effect
          .persist(started)
          .thenRun { x =>
            ctx.log.info(s"The state result persisted")
          }
    }

  def handleEvent(state: State, event: Event, ctx: ActorContext[Command]): State =
    event match {
      case Started(call) =>
        ctx.log.info(s"Handing event is start call and add in state counter")
        state.start(call)
    }
}
