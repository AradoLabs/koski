package fi.oph.koski.koskiuser

case class Palvelurooli(palveluName: String, rooli: String)

object Palvelurooli {
  def apply(rooli: String): Palvelurooli = Palvelurooli("KOSKI", rooli)
}