package ru.beeline.vafs
package callcontrol

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.persistence.typed.PersistenceId
import ru.beeline.vafs.callcontrol.process.ConsumerCall

object Application extends App{

  val persId = PersistenceId.ofUniqueId("ProcessCallApp")
  val app = ConsumerCall(persId)

  implicit val system: ActorSystem[NotUsed] = ActorSystem(app, persId.id)

}
