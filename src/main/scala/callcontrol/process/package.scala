package ru.beeline.vafs
package callcontrol

import callcontrol.counter.CounterCallReadSide

package object process {

  case class ResultOfRule(priority: Int, value: String)
  object ResultOfRule{
    val default = ResultOfRule(0, "Continue")
  }

  sealed trait CommandProcessCall

  case class ProcessCall(rule: Rule, call: Call, counterCallRead: CounterCallReadSide) extends CommandProcessCall

  case class GetResult(transactionId: Int) extends CommandProcessCall
}
