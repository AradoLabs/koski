package fi.oph.koski.schema

import fi.oph.koski.localization.LocalizedString
import fi.oph.scalaschema.annotation.Description

@Description("Tiedot aiemmin hankitun osaamisen tunnustamisesta.")
@OksaUri("tmpOKSAID629", "osaamisen tunnustaminen")
case class OsaamisenTunnustaminen(
  @Description("Aiemman, korvaavan suorituksen tiedot")
  @Flatten
  osaaminen: Option[Suoritus],
  @Description("Osaamisen tunnustamisen kautta saatavan tutkinnon osan suorituksen selite")
  @OksaUri("tmpOKSAID629", "osaamisen tunnustaminen")
  @Representative
  @MultiLineString(5)
  selite: LocalizedString
)
