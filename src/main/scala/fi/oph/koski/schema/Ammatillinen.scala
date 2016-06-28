package fi.oph.koski.schema

import java.time.LocalDate

import fi.oph.koski.localization.LocalizedString
import fi.oph.koski.localization.LocalizedString.finnish
import fi.oph.scalaschema.annotation.{Description, MaxItems, MinItems}

@Description("Ammatillisen koulutuksen opiskeluoikeus")
case class AmmatillinenOpiskeluoikeus(
  id: Option[Int] = None,
  versionumero: Option[Int] = None,
  lähdejärjestelmänId: Option[LähdejärjestelmäId] = None,
  alkamispäivä: Option[LocalDate] = None,
  arvioituPäättymispäivä: Option[LocalDate] = None,
  päättymispäivä: Option[LocalDate] = None,
  oppilaitos: Oppilaitos,
  koulutustoimija: Option[OrganisaatioWithOid] = None,
  @MinItems(1) @MaxItems(2)
  suoritukset: List[AmmatillinenPäätasonSuoritus],
  @Description("Opiskelijan suorituksen tavoite-tieto kertoo sen, suorittaako opiskelija tutkintotavoitteista koulutusta (koko tutkintoa) vai tutkinnon osa tavoitteista koulutusta (tutkinnon osaa)")
  @KoodistoUri("suorituksentyyppi")
  @KoodistoKoodiarvo("ammatillinentutkinto")
  @KoodistoKoodiarvo("ammatillisentutkinnonosa")
  tavoite: Koodistokoodiviite,
  tila: Option[AmmatillinenOpiskeluoikeudenTila] = None,
  läsnäolotiedot: Option[YleisetLäsnäolotiedot] = None,
  @KoodistoKoodiarvo("ammatillinenkoulutus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("ammatillinenkoulutus", "opiskeluoikeudentyyppi"),
  lisätiedot: Option[AmmatillisenOpiskeluoikeudenLisätiedot] = None
) extends KoskeenTallennettavaOpiskeluoikeus {
  override def withIdAndVersion(id: Option[Int], versionumero: Option[Int]) = this.copy(id = id, versionumero = versionumero)
  override def withKoulutustoimija(koulutustoimija: OrganisaatioWithOid) = this.copy(koulutustoimija = Some(koulutustoimija))
}

sealed trait AmmatillinenPäätasonSuoritus extends Suoritus

case class AmmatillisenOpiskeluoikeudenLisätiedot(
  @Description("Jos kyseessä erityisopiskelija, jolle on tehty henkilökohtainen opetuksen järjestämistä koskeva suunnitelma (hojks), täytetään tämä tieto. Kentän puuttuminen tai null-arvo tulkitaan siten, että suunnitelmaa ei ole tehty.")
  hojks: Option[Hojks],
  oikeusMaksuttomaanAsuntolapaikkaan: Boolean = false,
  @Description("Tieto siitä liittyykö opintoihin ulkomaanjaksoja")
  ulkomaanjakso: Option[List[Ulkomaanjakso]] = None
)

case class AmmatillinenOpiskeluoikeudenTila(
  opiskeluoikeusjaksot: List[AmmatillinenOpiskeluoikeusjakso]
) extends OpiskeluoikeudenTila

@Description("Sisältää myös tiedon opintojen rahoituksesta jaksoittain.")
case class AmmatillinenOpiskeluoikeusjakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  @KoodistoUri("ammatillinenopiskeluoikeudentila")
  tila: Koodistokoodiviite,
  @Description("Opintojen rahoitus")
  @KoodistoUri("opintojenrahoitus")
  opintojenRahoitus: Option[Koodistokoodiviite]
) extends Opiskeluoikeusjakso

case class NäyttötutkintoonValmistavanKoulutuksenSuoritus(
  koulutusmoduuli: AmmatillinenTutkintoKoulutus,
  @Description("Tieto siitä mihin tutkintonimikkeeseen oppijan tutkinto liittyy")
  @KoodistoUri("tutkintonimikkeet")
  @OksaUri("tmpOKSAID588", "tutkintonimike")
  tutkintonimike: Option[List[Koodistokoodiviite]] = None,
  @Description("Tieto siitä mihin osaamisalaan/osaamisaloihin oppijan tutkinto liittyy")
  @KoodistoUri("osaamisala")
  @OksaUri(tunnus = "tmpOKSAID299", käsite = "osaamisala")
  osaamisala: Option[List[Koodistokoodiviite]] = None,
  suorituskieli: Option[Koodistokoodiviite] = None,
  tila: Koodistokoodiviite,
  override val alkamispäivä: Option[LocalDate],
  @Description("Suorituksen päättymispäivä. Muoto YYYY-MM-DD")
  val päättymispäivä: Option[LocalDate],
  toimipiste: OrganisaatioWithOid,
  vahvistus: Option[Henkilövahvistus] = None,
  @Description("Valmistavan koulutuksen osat")
  override val osasuoritukset: Option[List[NäyttötutkintoonValmistavanKoulutuksenOsanSuoritus]] = None,
  @KoodistoKoodiarvo("nayttotutkintoonvalmistavakoulutus")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("nayttotutkintoonvalmistavakoulutus", "suorituksentyyppi")
) extends AmmatillinenPäätasonSuoritus with Toimipisteellinen {
  def arviointi: Option[List[AmmatillinenArviointi]] = None
}

case class AmmatillisenTutkinnonSuoritus(
  koulutusmoduuli: AmmatillinenTutkintoKoulutus,
  @Description("Tieto siitä mihin tutkintonimikkeeseen oppijan tutkinto liittyy")
  @KoodistoUri("tutkintonimikkeet")
  @OksaUri("tmpOKSAID588", "tutkintonimike")
  tutkintonimike: Option[List[Koodistokoodiviite]] = None,
  @Description("Tieto siitä mihin osaamisalaan/osaamisaloihin oppijan tutkinto liittyy")
  @KoodistoUri("osaamisala")
  @OksaUri(tunnus = "tmpOKSAID299", käsite = "osaamisala")
  osaamisala: Option[List[Koodistokoodiviite]] = None,
  @Description("Tutkinnon suoritustapa")
  @OksaUri("tmpOKSAID141", "ammatillisen koulutuksen järjestämistapa")
  @KoodistoUri("ammatillisentutkinnonsuoritustapa")
  suoritustapa: Option[Koodistokoodiviite] = None,
  @Description("Koulutuksen järjestämismuoto")
  @OksaUri("tmpOKSAID140", "koulutuksen järjestämismuoto")
  järjestämismuoto: Option[Järjestämismuoto] = None,

  suorituskieli: Option[Koodistokoodiviite],
  tila: Koodistokoodiviite,
  override val alkamispäivä: Option[LocalDate],
  toimipiste: OrganisaatioWithOid,
  vahvistus: Option[Henkilövahvistus] = None,
  @Description("Ammatilliseen tutkintoon liittyvät tutkinnonosan suoritukset")
  override val osasuoritukset: Option[List[AmmatillisenTutkinnonOsanSuoritus]] = None,
  @KoodistoKoodiarvo("ammatillinentutkinto")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("ammatillinentutkinto", "suorituksentyyppi")
) extends AmmatillinenPäätasonSuoritus with Toimipisteellinen {
  def arviointi: Option[List[AmmatillinenArviointi]] = None
}

case class AmmatillisenTutkinnonOsanSuoritus(
  koulutusmoduuli: AmmatillisenTutkinnonOsa,
  tunnustettu: Option[Tunnustaminen] = None,
  @Description("Suoritukseen liittyvän näytön tiedot")
  näyttö: Option[Näyttö] = None,
  lisätiedot: Option[List[AmmatillisenTutkinnonOsanLisätieto]] = None,
  @Description("Tutkinto, jonka rakenteeseen tutkinnon osa liittyy. Käytetään vain tapauksissa, joissa tutkinnon osa on poimittu toisesta tutkinnosta.")
  tutkinto: Option[AmmatillinenTutkintoKoulutus] = None,
  suorituskieli: Option[Koodistokoodiviite],
  tila: Koodistokoodiviite,
  override val alkamispäivä: Option[LocalDate],
  @Description("Oppilaitoksen toimipiste, jossa opinnot on suoritettu")
  @OksaUri("tmpOKSAID148", "koulutusorganisaation toimipiste")
  toimipiste: Option[OrganisaatioWithOid],
  arviointi: Option[List[AmmatillinenArviointi]] = None,
  vahvistus: Option[Henkilövahvistus] = None,
  @KoodistoKoodiarvo("ammatillisentutkinnonosa")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("ammatillisentutkinnonosa", koodistoUri = "suorituksentyyppi"),
  työssäoppimisjaksot: Option[List[Työssäoppimisjakso]] = None
) extends Suoritus

case class Työssäoppimisjakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  @KoodistoUri("kunta")
  @Description("Kunta, jossa työssäoppiminen on tapahtunut")
  paikkakunta: Koodistokoodiviite,
  @Description("Maa, jossa työssäoppiminen on tapahtunut")
  @KoodistoUri("maatjavaltiot2")
  maa: Koodistokoodiviite,
  työtehtävät: LocalizedString,
  laajuus: LaajuusOsaamispisteissä
) extends Jakso

@Description("Ammatillisen tutkinnon tunnistetiedot")
case class AmmatillinenTutkintoKoulutus(
 tunniste: Koodistokoodiviite,
 perusteenDiaarinumero: Option[String]
) extends Koulutus with EPerusteistaLöytyväKoulutusmoduuli {
  override def laajuus = None
  override def isTutkinto = true
}

sealed trait AmmatillisenTutkinnonOsa extends Koulutusmoduuli {
  def laajuus: Option[LaajuusOsaamispisteissä]
}

@Description("Opetussuunnitelmaan kuuluvan tutkinnon osan tunnistetiedot")
case class OpsTutkinnonosa(
  @Description("Tutkinnon osan kansallinen koodi")
  @KoodistoUri("tutkinnonosat")
  tunniste: Koodistokoodiviite,
  @Description("Onko pakollinen osa tutkinnossa")
  pakollinen: Boolean,
  override val laajuus: Option[LaajuusOsaamispisteissä],
  paikallinenKoodi: Option[PaikallinenKoodi] = None,
  kuvaus: Option[LocalizedString] = None
) extends AmmatillisenTutkinnonOsa with KoodistostaLöytyväKoulutusmoduuli

@Description("Paikallisen tutkinnon osan tunnistetiedot")
case class PaikallinenTutkinnonosa(
  tunniste: PaikallinenKoodi,
  kuvaus: LocalizedString,
  @Description("Onko pakollinen osa tutkinnossa")
  pakollinen: Boolean,
  override val laajuus: Option[LaajuusOsaamispisteissä]
) extends AmmatillisenTutkinnonOsa with PaikallinenKoulutusmoduuli

case class AmmatillisenTutkinnonOsanLisätieto(
  @Description("Lisätiedon tyyppi kooditettuna")
  @KoodistoUri("ammatillisentutkinnonosanlisatieto")
  tunniste: Koodistokoodiviite,
  @Description("Lisätiedon kuvaus siinä muodossa, kuin se näytetään todistuksella")
  kuvaus: LocalizedString
)

case class OppisopimuksellinenJärjestämismuoto(
  @KoodistoUri("jarjestamismuoto")
  @KoodistoKoodiarvo("20")
  tunniste: Koodistokoodiviite,
  oppisopimus: Oppisopimus
) extends Järjestämismuoto

trait AmmatillinenKoodistostaLöytyväArviointi extends KoodistostaLöytyväArviointi with ArviointiPäivämäärällä {
  override def hyväksytty = arvosana.koodiarvo match {
    case "0" => false
    case "Hylätty" => false
    case _ => true
  }
}

case class AmmatillinenArviointi(
  @KoodistoUri("arviointiasteikkoammatillinenhyvaksyttyhylatty")
  @KoodistoUri("arviointiasteikkoammatillinent1k3")
  arvosana: Koodistokoodiviite,
  päivä: LocalDate,
  @Description("Tutkinnon osan suorituksen arvioinnista päättäneen henkilön nimi")
  arvioitsijat: Option[List[Arvioitsija]] = None
) extends AmmatillinenKoodistostaLöytyväArviointi

@Description("Näytön kuvaus")
case class Näyttö(
  @Description("Vapaamuotoinen kuvaus suoritetusta näytöstä")
  kuvaus: LocalizedString,
  suorituspaikka: NäytönSuorituspaikka,
  arviointi: Option[NäytönArviointi],
  työssäoppimisenYhteydessä: Boolean = false
)

@Description("Ammatillisen näytön suorituspaikka")
case class NäytönSuorituspaikka(
  @Description("Suorituspaikan tyyppi 1-numeroisella koodilla")
  @KoodistoUri("ammatillisennaytonsuorituspaikka")
  tunniste: Koodistokoodiviite,
  @Description("Vapaamuotoinen suorituspaikan kuvaus")
  kuvaus: LocalizedString
)

case class NäytönArviointi (
  @Description("Näytön eri arviointikohteiden (Työprosessin hallinta jne) arvosanat.")
  arviointiKohteet: List[NäytönArviointikohde],
  @KoodistoUri("ammatillisennaytonarvioinnistapaattaneet")
  @Description("Arvioinnista päättäneet tahot, ilmaistuna 1-numeroisella koodilla")
  arvioinnistaPäättäneet: Koodistokoodiviite,
  @KoodistoUri("ammatillisennaytonarviointikeskusteluunosallistuneet")
  @Description("Arviointikeskusteluun osallistuneet tahot, ilmaistuna 1-numeroisella koodilla")
  arviointikeskusteluunOsallistuneet: Koodistokoodiviite
)

case class NäytönArviointikohde(
  @Description("Arviointikohteen tunniste")
  @KoodistoUri("ammatillisennaytonarviointikohde")
  tunniste: Koodistokoodiviite,
  @Description("Arvosana. Kullekin arviointiasteikolle löytyy oma koodistonsa")
  @KoodistoUri("arviointiasteikkoammatillinenhyvaksyttyhylatty")
  @KoodistoUri("arviointiasteikkoammatillinent1k3")
  arvosana: Koodistokoodiviite
)

@Description("Oppisopimuksen tiedot")
case class Oppisopimus(
  työnantaja: Yritys
)

trait Järjestämismuoto {
  def tunniste: Koodistokoodiviite
}

@Description("Järjestämismuoto ilman lisätietoja")
case class DefaultJärjestämismuoto(
  @KoodistoUri("jarjestamismuoto")
  tunniste: Koodistokoodiviite
) extends Järjestämismuoto

@Description("Henkilökohtainen opetuksen järjestämistä koskeva suunnitelma, https://fi.wikipedia.org/wiki/HOJKS")
@OksaUri("tmpOKSAID228", "erityisopiskelija")
case class Hojks(
  @KoodistoUri("opetusryhma")
  opetusryhmä: Koodistokoodiviite,
  @KoodistoUri("ammatillisenerityisopetuksenperuste")
  peruste: Koodistokoodiviite
)

case class LaajuusOsaamispisteissä(
  arvo: Float,
  @KoodistoKoodiarvo("6")
  yksikkö: Koodistokoodiviite = Koodistokoodiviite("6", Some(finnish("Osaamispistettä")), "opintojenlaajuusyksikko")
) extends Laajuus

case class NäyttötutkintoonValmistavanKoulutuksenOsanSuoritus(
  suorituskieli: Option[Koodistokoodiviite] = None,
  tila: Koodistokoodiviite,
  @KoodistoKoodiarvo("nayttotutkintoonvalmistavankoulutuksenosa")
  tyyppi: Koodistokoodiviite = Koodistokoodiviite("nayttotutkintoonvalmistavankoulutuksenosa", koodistoUri = "suorituksentyyppi"),
  koulutusmoduuli: NäyttötutkintoonValmistavanKoulutuksenOsa
) extends Suoritus {
  override def osasuoritukset = None
  def arviointi = None
  def vahvistus = None
}

@Description("Ammatilliseen peruskoulutukseen valmentavan koulutuksen osan tunnistetiedot")
case class NäyttötutkintoonValmistavanKoulutuksenOsa(
  tunniste: PaikallinenKoodi
) extends PaikallinenKoulutusmoduuli {
  def laajuus = None
}
