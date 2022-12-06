package ru.beeline.vafs
package backoffice

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import ru.beeline.vafs.backoffice.dao.entities.{Rule, RuleId}

package object dto {

  case class RuleIdDTO(ruleId: String)

  object RuleIdDTO {
    implicit val decoder: Decoder[RuleIdDTO] = deriveDecoder
    implicit val encoder: Encoder[RuleIdDTO] = deriveEncoder
  }

  case class RuleDTO(
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
  )

  object RuleDTO {

    def from(rule: Rule, lstA: List[Long], lstB: List[Long]): RuleDTO = RuleDTO(
      priority = rule.priority,
      lstA = lstA,
      oprA = rule.oprA,
      lstB = lstB,
      oprB = rule.oprB,
      oprCnt = rule.oprCnt,
      trgCnt = rule.trgCnt,
      valueIsTrue = rule.valueIsTrue,
      oprAB = rule.oprAB,
      oprABCnt = rule.oprABCnt
    )
  }

  case class RuleDTOKafka(
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
  )

  object RuleDTOKafka {

    def from(rule: Rule, lstA: List[Long], lstB: List[Long], opr: String): RuleDTOKafka = RuleDTOKafka(
      id = rule.id,
      operation = opr,
      priority = rule.priority,
      lstA = lstA,
      oprA = rule.oprA,
      lstB = lstB,
      oprB = rule.oprB,
      oprCnt = rule.oprCnt,
      trgCnt = rule.trgCnt,
      valueIsTrue = rule.valueIsTrue,
      oprAB = rule.oprAB,
      oprABCnt = rule.oprABCnt
    )
  }
}
