package ru.beeline.vafs
package callcontrol


package object counter {

  sealed trait Command

  case class Start(call: Call) extends Command

  sealed trait Event

  case class Started(call: Call) extends Event

  final case class State(counters: Map[String, Int]) {
    def start(call: Call): State = {

      val newCounters = this.counters + (s"${call.numberA}_${call.numberB}" ->
        (counters.getOrElse(s"${call.numberA}_${call.numberB}", 0) + 1))

      copy(counters = newCounters)
    }
  }

  object State {
    val empty = State(Map.empty[String, Int])
  }
}
