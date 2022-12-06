package ru.beeline.vafs

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}

package object callcontrol {

  sealed trait CommandCall

  case class Call(transactionId: Int, numberA: Long, numberB: Long, initialCall: Long) extends CommandCall

  implicit val callDecoder: Decoder[Call] = deriveDecoder

  case class ResponseForCall(transactionId: Int, response: String) extends CommandCall

  implicit val responseForCallEncoder: Encoder[ResponseForCall] = deriveEncoder

  case class Rule(
    id: String,
    operation: String,
    priority: Int,
    lstA: List[Long],
    oprA: String,
    lstB: List[Long],
    oprB: String,
    oprCnt: String,
    trgCnt: Long,
    valueIsTrue: String,
    oprAB: String,
    oprABCnt: String
  ) extends CommandCall {

    private def verifList(number: Long, opr: String, lst: List[Long]): Boolean =
      if (opr == "in")
        lst.contains(number)
      else
        !lst.contains(number)

    def verification(numberA: Long, numberB: Long, cnt: Long): String = {
      val verifLstA = verifList(numberA, oprA, lstA)

      val verifLstB = verifList(numberB, oprB, lstB)

      val verifCnt: Boolean = oprCnt match {
        case ">"  => cnt + 1 > trgCnt
        case "<"  => cnt + 1 < trgCnt
        case ">=" => cnt + 1 >= trgCnt
        case "<=" => cnt + 1 <= trgCnt
        case "!=" => cnt + 1 != trgCnt
        case "==" => cnt + 1 == trgCnt
      }

      val verifAB = oprAB match {
        case "and" => verifLstA && verifLstB
        case "or"  => verifLstA || verifLstB
      }

      val verifAll = oprABCnt match {
        case "and" => verifAB && verifCnt
        case "or"  => verifAB || verifCnt
      }

      valueIsTrue match {
        case "Continue" => if (verifAll) "Continue" else "Release"
        case "Release"  => if (verifAll) "Release" else "Continue"
      }
    }
  }

  implicit val ruleDecoder: Decoder[Rule] = deriveCodec

}
