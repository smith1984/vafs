package ru.beeline.vafs
package callcontrol.process

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import ru.beeline.vafs.callcontrol.{CommandCall, ResponseForCall}

object CallStatus {
  def apply(result: ResultOfRule, parent: ActorRef[CommandCall]): Behavior[CommandProcessCall] =
    process(result, parent)

  def process(result: ResultOfRule, parent: ActorRef[CommandCall]): Behavior[CommandProcessCall] = Behaviors.setup {
    ctx =>
      Behaviors.receiveMessage {
        case ProcessCall(rule, call, counterCallRead) =>
          val route = s"${call.numberA}_${call.numberB}"
          val cnt = counterCallRead.stateCounter.filter(_._2 == route)(0)._3
          val responseOfRule = rule.verification(call.numberA, call.numberB, cnt)
          val newResult = ResultOfRule(
            priority = if (result.priority > rule.priority) result.priority else rule.priority,
            value = if (result.priority > rule.priority) result.value else responseOfRule
          )
          ctx.log.info(
            s"Add rule with priority ${rule.priority} and response ${responseOfRule} " +
              s"Current status is $newResult"
          );
          process(newResult, parent)

        case GetResult(transactionId) =>
          parent ! ResponseForCall(transactionId = transactionId, result.value)
          ctx.log.info(s"Call status: $result");
          Behaviors.same
      }
  }
}
