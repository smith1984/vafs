package ru.beeline.vafs
package backoffice.dao.entities

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import ru.beeline.vafs.backoffice.dto.RuleDTO

case class RuleId(id: String) extends AnyVal

case class Rule(
  id: String,
  priority: Int,
  lstAId: String,
  oprA: String,
  lstBId: String,
  oprB: String,
  oprCnt: String,
  trgCnt: Long,
  valueIsTrue: String,
  oprAB: String,
  oprABCnt: String
) {
  def typedId: RuleId = RuleId(id)
}

object Rule {
  implicit val decoder: Decoder[Rule] = deriveDecoder
  implicit val encoder: Encoder[Rule] = deriveEncoder

  def from(id: String, lstAId: String, lstBId: String, ruleDTO: RuleDTO): Rule = Rule(
    id = id,
    priority = ruleDTO.priority,
    lstAId = lstAId,
    oprA = ruleDTO.oprA,
    lstBId = lstBId,
    oprB = ruleDTO.oprB,
    oprCnt = ruleDTO.oprCnt,
    trgCnt = ruleDTO.trgCnt,
    valueIsTrue = ruleDTO.valueIsTrue,
    oprAB = ruleDTO.oprAB,
    oprABCnt = ruleDTO.oprABCnt
  )
}

case class ListNumberId(id: String) extends AnyVal

case class NumberOfList(id: String, number: Long) {
  def typedId: ListNumberId = ListNumberId(id)
}

case class ListWithNumber(id: String, numbers: List[Long]) {
  def typedId: ListNumberId = ListNumberId(id)
}

object ListWithNumber                                      {
  implicit val decoder: Decoder[ListWithNumber] = deriveDecoder
  implicit val encoder: Encoder[ListWithNumber] = deriveEncoder

  def to(lst: ListWithNumber): List[NumberOfList] = lst.numbers.map(NumberOfList(lst.id, _))
}
