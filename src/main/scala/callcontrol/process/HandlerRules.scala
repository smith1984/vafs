package ru.beeline.vafs
package callcontrol.process

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import ru.beeline.vafs.callcontrol.counter.CounterCallReadSide
import ru.beeline.vafs.callcontrol.{Call, CommandCall, ResponseForCall, Rule}

import scala.collection.mutable.ListBuffer

object HandlerRules {
  var lst = ListBuffer[Rule]()

  def apply(persId: PersistenceId): Behavior[CommandCall] = Behaviors.setup { ctx =>

    val producerResponse = ctx.spawn(ProducerResponse(), "producerCallResponse")

    val counterCallReader = CounterCallReadSide(ctx.system, persId)

    Behaviors.receiveMessage {
      case call @ Call(transactionId, numberA, numberB, initialCall) => {
        val callStatus: ActorRef[CommandProcessCall] =
          ctx.spawn(CallStatus(ResultOfRule.default, ctx.self), s"callStatus${transactionId}")
        lst.foreach(rule => callStatus ! ProcessCall(rule, call, counterCallReader))

        callStatus ! GetResult(transactionId)
      Behaviors.same
      }
      case rule @ Rule(id, operation, _, _, _, _, _, _, _, _, _, _) => {
        operation match {
          case "activate" => lst += rule
          case "deactivate" => lst -= lst.filter(_.id == id)(0)
          case "update" =>
            lst -= lst.filter(_.id == id)(0)
            lst += rule
        }
        Behaviors.same
      }

      case call @ ResponseForCall(transactionId, response) =>
        producerResponse ! call
        Behaviors.same
    }
  }
}
