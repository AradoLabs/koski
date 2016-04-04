package fi.oph.tor.schema

import java.time.LocalDate

import fi.oph.tor.koodisto.KoodistoViite
import fi.oph.tor.log.Loggable
import fi.oph.tor.schema.generic.annotation.{Description, MinValue, ReadOnly, RegularExpression}


case class TorOppija(
  henkilö: Henkilö,
  @Description("Lista henkilön opiskeluoikeuksista. Sisältää vain ne opiskeluoikeudet, joihin käyttäjällä on oikeudet. Esimerkiksi ammatilliselle toimijalle ei välttämättä näy henkilön lukio-opintojen tietoja")
  opiskeluoikeudet: Seq[OpiskeluOikeus]
)

@Description("Henkilötiedot. Syötettäessä vaaditaan joko `oid` tai kaikki muut kentät, jolloin järjestelmään voidaan tarvittaessa luoda uusi henkilö")
sealed trait Henkilö

@Description("Täydet henkilötiedot. Tietoja haettaessa TOR:sta saadaan aina täydet henkilötiedot.")
case class FullHenkilö(
  @Description("Yksilöivä tunniste (oppijanumero) Opintopolku-palvelussa")
  @OksaUri("tmpOKSAID760", "oppijanumero")
  @RegularExpression("""1\.2\.246\.562\.24\.\d{11}""")
  oid: String,
  @Description("Suomalainen henkilötunnus")
  hetu: String,
  @Description("Henkilön kaikki etunimet. Esimerkiksi Sanna Katariina")
  etunimet:String,
  @Description("Kutsumanimi, oltava yksi etunimistä. Esimerkiksi etunimille \"Juha-Matti Petteri\" kelpaavat joko \"Juha-Matti\", \"Juha\", \"Matti\" tai \"Petteri\"")
  kutsumanimi: String,
  @Description("Henkilön sukunimi. Henkilön sukunimen etuliite tulee osana sukunimeä")
  sukunimi: String,
  @Description("Opiskelijan äidinkieli")
  @KoodistoUri("kieli")
  äidinkieli: Option[KoodistoKoodiViite],
  @Description("Opiskelijan kansalaisuudet")
  @KoodistoUri("maatjavaltiot2")
  kansalaisuus: Option[List[KoodistoKoodiViite]]
) extends HenkilöWithOid

@Description("Henkilö, jonka oppijanumero ei ole tiedossa. Tietoja syötettäessä luodaan mahdollisesti uusi henkilö Henkilöpalveluun, jolloin henkilölle muodostuu oppijanumero")
case class NewHenkilö(
  @Description("Suomalainen henkilötunnus")
  hetu: String,
  @Description("Henkilön kaikki etunimet. Esimerkiksi Sanna Katariina")
  etunimet:String,
  @Description("Kutsumanimi, oltava yksi etunimistä. Esimerkiksi etunimille \"Juha-Matti Petteri\" kelpaavat joko \"Juha-Matti\", \"Juha\", \"Matti\" tai \"Petteri\"")
  kutsumanimi: String,
  @Description("Henkilön sukunimi. Henkilön sukunimen etuliite tulee osana sukunimeä")
  sukunimi: String
) extends Henkilö

@Description("Henkilö, jonka oid on tiedossa. Tietoja syötettäessä henkilö haetaan henkilöpalvelusta.")
case class OidHenkilö(
  @Description("Yksilöivä tunniste (oppijanumero) Opintopolku-palvelussa")
  @OksaUri("tmpOKSAID760", "oppijanumero")
  @RegularExpression("""1\.2\.246\.562\.24\.\d{11}""")
  oid: String
) extends HenkilöWithOid

trait HenkilöWithOid extends Henkilö {
  def oid: String
}

object Henkilö {
  type Oid = String
  def withOid(oid: String) = OidHenkilö(oid)
  def apply(hetu: String, etunimet: String, kutsumanimi: String, sukunimi: String) = NewHenkilö(hetu, etunimet, kutsumanimi, sukunimi)
}

case class OpiskeluOikeus(
  @Description("Opiskeluoikeuden uniikki tunniste, joka generoidaan TOR-järjestelmässä. Tietoja syötettäessä kenttä ei ole pakollinen. " +
    "Tietoja päivitettäessä TOR tunnistaa opiskeluoikeuden joko tämän id:n tai muiden kenttien (oppijaOid, organisaatio, diaarinumero) perusteella")
  id: Option[Int],
  @Description("Versionumero, joka generoidaan TOR-järjestelmässä. Tietoja syötettäessä kenttä ei ole pakollinen. " +
    "Ensimmäinen tallennettu versio saa versionumeron 1, jonka jälkeen jokainen päivitys aiheuttaa versionumeron noston yhdellä. " +
    "Jos tietoja päivitettäessä käytetään versionumeroa, pitää sen täsmätä viimeisimpään tallennettuun versioon. " +
    "Tällä menettelyllä esimerkiksi käyttöliittymässä varmistetaan, ettei tehdä päivityksiä vanhentuneeseen dataan.")
  versionumero: Option[Int],
  @Description("Lähdejärjestelmän tunniste ja opiskeluoikeuden tunniste lähdejärjestelmässä. " +
    "Käytetään silloin, kun opiskeluoikeus on tuotu TOR:iin tiedonsiirrolla ulkoisesta järjestelmästä, eli käytännössä oppilashallintojärjestelmästä.")
  lähdejärjestelmänId: Option[LähdejärjestelmäId],
  @Description("Opiskelijan opiskeluoikeuden alkamisaika joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  alkamispäivä: Option[LocalDate],
  @Description("Opiskelijan opiskeluoikeuden arvioitu päättymispäivä joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  arvioituPäättymispäivä: Option[LocalDate],
  @Description("Opiskelijan opiskeluoikeuden päättymispäivä joko tutkintotavoitteisessa koulutuksessa tai tutkinnon osa tavoitteisessa koulutuksessa. Muoto YYYY-MM-DD")
  päättymispäivä: Option[LocalDate],
  @Description("Oppilaitos, jossa opinnot on suoritettu")
  oppilaitos: Oppilaitos,
  @Description("Opiskeluoikeuteen liittyvän (tutkinto-)suorituksen tiedot")
  suoritus: AmmatillinenTutkintoSuoritus,
  hojks: Option[Hojks],
  @Description("Opiskelijan suorituksen tavoite-tieto kertoo sen, suorittaako opiskelija tutkintotavoitteista koulutusta (koko tutkintoa) vai tutkinnon osa tavoitteista koulutusta (tutkinnon osaa)")
  @KoodistoUri("opintojentavoite")
  tavoite: Option[KoodistoKoodiViite],
  opiskeluoikeudenTila: Option[OpiskeluoikeudenTila],
  läsnäolotiedot: Option[Läsnäolotiedot]
) extends Loggable {
  override def toString = id match {
    case None => "opiskeluoikeus"
    case Some(id) => "opiskeluoikeus " + id
  }
}

object OpiskeluOikeus {
  type Id = Int
  type Versionumero = Int
  val VERSIO_1 = 1
}

trait Suoritus {
  def koulutusmoduuli: Koulutusmoduuli
  @Description("Paikallinen tunniste suoritukselle. Tiedonsiirroissa tarpeellinen, jotta voidaan varmistaa päivitysten osuminen oikeaan suoritukseen")
  def paikallinenId: Option[String]
  @Description("Opintojen suorituskieli")
  @KoodistoUri("kieli")
  @OksaUri("tmpOKSAID309", "opintosuorituksen kieli")
  def suorituskieli: Option[KoodistoKoodiViite]
  @Description("Suorituksen tila (KESKEN, VALMIS, KESKEYTYNYT)")
  @KoodistoUri("suorituksentila")
  def tila: KoodistoKoodiViite
  def alkamispäivä: Option[LocalDate]
  @Description("Oppilaitoksen toimipiste, jossa opinnot on suoritettu")
  @OksaUri("tmpOKSAID148", "koulutusorganisaation toimipiste")
  def toimipiste: OrganisaatioWithOid
  @Description("Arviointi. Jos listalla useampi arviointi, tulkitaan myöhemmät arvioinnit arvosanan korotuksiksi. Jos aiempaa, esimerkiksi väärin kirjattua, arviota korjataan, ei listalle tule uutta arviota")
  def arviointi: Option[List[Arviointi]]
  @Description("Suorituksen virallinen vahvistus (päivämäärä, henkilöt). Vaaditaan silloin, kun suorituksen tila on VALMIS.")
  def vahvistus: Option[Vahvistus]
  def osasuoritukset: Option[List[Suoritus]]
  def osasuoritusLista: List[Suoritus] = osasuoritukset.toList.flatten
  def rekursiivisetOsasuoritukset: List[Suoritus] = {
    osasuoritusLista ++ osasuoritusLista.flatMap(_.rekursiivisetOsasuoritukset)
  }
}

case class AmmatillinenTutkintoSuoritus(
  koulutusmoduuli: TutkintoKoulutus,
  @Description("Tieto siitä mihin tutkintonimikkeeseen oppijan tutkinto liittyy")
  @KoodistoUri("tutkintonimikkeet")
  @OksaUri("tmpOKSAID588", "tutkintonimike")
  tutkintonimike: Option[List[KoodistoKoodiViite]] = None,
  @Description("Osaamisala")
  @KoodistoUri("osaamisala")
  @OksaUri(tunnus = "tmpOKSAID299", käsite = "osaamisala")
  osaamisala: Option[List[KoodistoKoodiViite]] = None,
  @Description("Tutkinnon tai tutkinnon osan suoritustapa")
  @OksaUri("tmpOKSAID141", "ammatillisen koulutuksen järjestämistapa")
  suoritustapa: Option[Suoritustapa] = None,
  @Description("Koulutuksen järjestämismuoto")
  @OksaUri("tmpOKSAID140", "koulutuksen järjestämismuoto")
  järjestämismuoto: Option[Järjestämismuoto] = None,

  paikallinenId: Option[String],
  suorituskieli: Option[KoodistoKoodiViite],
  tila: KoodistoKoodiViite,
  alkamispäivä: Option[LocalDate],
  toimipiste: OrganisaatioWithOid,
  arviointi: Option[List[Arviointi]] = None,
  vahvistus: Option[Vahvistus] = None,
  osasuoritukset: Option[List[AmmatillinenTutkinnonosaSuoritus]] = None
) extends Suoritus

trait AmmatillinenTutkinnonosaSuoritus extends Suoritus
  case class AmmatillinenOpsTutkinnonosaSuoritus(
    koulutusmoduuli: OpsTutkinnonosa,
    hyväksiluku: Option[Hyväksiluku] = None,
    @Description("Suoritukseen liittyvän näytön tiedot")
    näyttö: Option[Näyttö] = None,
    lisätiedot: Option[List[AmmatillisenTutkinnonOsanLisätieto]] = None,
    @Description("Tutkinto, jonka rakenteeseen tutkinnon osa liittyy. Käytetään vain tapauksissa, joissa tutkinnon osa on poimittu toisesta tutkinnosta.")
    tutkinto: Option[TutkintoKoulutus] = None,

    paikallinenId: Option[String],
    suorituskieli: Option[KoodistoKoodiViite],
    tila: KoodistoKoodiViite,
    alkamispäivä: Option[LocalDate],
    toimipiste: OrganisaatioWithOid,
    arviointi: Option[List[Arviointi]] = None,
    vahvistus: Option[Vahvistus] = None,
    osasuoritukset: Option[List[AmmatillinenOpsTutkinnonosaSuoritus]] = None
  ) extends AmmatillinenTutkinnonosaSuoritus

  case class AmmatillinenPaikallinenTutkinnonosaSuoritus(
    koulutusmoduuli: PaikallinenTutkinnonosa,
    hyväksiluku: Option[Hyväksiluku] = None,
    @Description("Suoritukseen liittyvän näytön tiedot")
    näyttö: Option[Näyttö] = None,
    lisätiedot: Option[List[AmmatillisenTutkinnonOsanLisätieto]] = None,

    paikallinenId: Option[String],
    suorituskieli: Option[KoodistoKoodiViite],
    tila: KoodistoKoodiViite,
    alkamispäivä: Option[LocalDate],
    toimipiste: OrganisaatioWithOid,
    arviointi: Option[List[Arviointi]] = None,
    vahvistus: Option[Vahvistus] = None,
    osasuoritukset: Option[List[AmmatillinenPaikallinenTutkinnonosaSuoritus]] = None
  ) extends AmmatillinenTutkinnonosaSuoritus

trait Koulutusmoduuli {
  def tunniste: KoodiViite
}
  @Description("Tutkintoon johtava koulutus")
  case class TutkintoKoulutus(
   @Description("Tutkinnon 6-numeroinen tutkintokoodi")
   @KoodistoUri("koulutus")
   @OksaUri("tmpOKSAID560", "tutkinto")
   tunniste: KoodistoKoodiViite,
   @Description("Tutkinnon perusteen diaarinumero (pakollinen). Ks. ePerusteet-palvelu")
   perusteenDiaarinumero: Option[String]
  ) extends Koulutusmoduuli

  @Description("Opetussuunnitelmaan kuuluva tutkinnon osa")
  case class OpsTutkinnonosa(
    @Description("Tutkinnon osan kansallinen koodi")
    @KoodistoUri("tutkinnonosat")
    tunniste: KoodistoKoodiViite,
    @Description("Onko pakollinen osa tutkinnossa")
    pakollinen: Boolean,
    laajuus: Option[Laajuus],
    paikallinenKoodi: Option[Paikallinenkoodi] = None,
    kuvaus: Option[String] = None
  ) extends Koulutusmoduuli

  @Description("Paikallinen tutkinnon osa")
  case class PaikallinenTutkinnonosa(
    tunniste: Paikallinenkoodi,
    kuvaus: String,
    @Description("Onko pakollinen osa tutkinnossa")
    pakollinen: Boolean,
    laajuus: Option[Laajuus]
  ) extends Koulutusmoduuli

case class AmmatillisenTutkinnonOsanLisätieto(
  @Description("Lisätiedon tyyppi kooditettuna")
  @KoodistoUri("ammatillisentutkinnonosanlisatieto")
  tunniste: KoodistoKoodiViite,
  @Description("Lisätiedon kuvaus siinä muodossa, kuin se näytetään todistuksella")
  kuvaus: String
)
case class Arviointi(
  @Description("Arvosana. Kullekin arviointiasteikolle löytyy oma koodistonsa")
  @KoodistoUri("arviointiasteikkoammatillinenhyvaksyttyhylatty")
  @KoodistoUri("arviointiasteikkoammatillinent1k3")
  arvosana: KoodistoKoodiViite,
  @Description("Päivämäärä, jolloin arviointi on annettu")
  päivä: Option[LocalDate],
  @Description("Tutkinnon osan suorituksen arvioinnista päättäneen henkilön nimi")
  arvioitsijat: Option[List[Arvioitsija]] = None
)

case class Arvioitsija(
  nimi: String
)

case class Vahvistus(
  @Description("Tutkinnon tai tutkinnonosan vahvistettu suorituspäivämäärä, eli päivämäärä jolloin suoritus on hyväksyttyä todennettua osaamista")
  päivä: Option[LocalDate],
  myöntäjäOrganisaatio: Option[Organisaatio] = None,
  myöntäjäHenkilöt: Option[List[OrganisaatioHenkilö]] = None
)

case class OrganisaatioHenkilö(
  nimi: String,
  titteli: String,
  organisaatio: Organisaatio
)

case class Suoritustapa(
  @KoodistoUri("suoritustapa")
  tunniste: KoodistoKoodiViite
)

trait Järjestämismuoto {
  def tunniste: KoodistoKoodiViite
}

@Description("Järjestämismuoto ilman lisätietoja")
case class DefaultJärjestämismuoto(
  @KoodistoUri("jarjestamismuoto")
  tunniste: KoodistoKoodiViite
) extends Järjestämismuoto

case class OppisopimuksellinenJärjestämismuoto(
  @KoodistoUri("jarjestamismuoto")
  @KoodistoKoodiarvo("20")
  tunniste: KoodistoKoodiViite,
  oppisopimus: Oppisopimus
) extends Järjestämismuoto

case class Hyväksiluku(
  @Description("Aiemman, korvaavan suorituksen kuvaus")
  osaaminen: Koulutusmoduuli,
  @Description("Osaamisen tunnustamisen kautta saatavan tutkinnon osan suorituksen selite")
  @OksaUri("tmpOKSAID629", "osaamisen tunnustaminen")
  selite: Option[String]
)

@Description("Näytön kuvaus")
case class Näyttö(
  @Description("Vapaamuotoinen kuvaus suoritetusta näytöstä")
  kuvaus: String,
  suorituspaikka: NäytönSuorituspaikka,
  arviointi: Option[NäytönArviointi]
)

@Description("Ammatillisen näytön suorituspaikka")
case class NäytönSuorituspaikka(
  @Description("Suorituspaikan tyyppi 1-numeroisella koodilla")
  @KoodistoUri("ammatillisennaytonsuorituspaikka")
  tunniste: KoodistoKoodiViite,
  @Description("Vapaamuotoinen suorituspaikan kuvaus")
  kuvaus: String
)

case class NäytönArviointi (
  @Description("Näytön eri arviointikohteiden (Työprosessin hallinta jne) arvosanat.")
  arviointiKohteet: List[NäytönArviointikohde],
  @KoodistoUri("ammatillisennaytonarvioinnistapaattaneet")
  @Description("Arvioinnista päättäneet tahot, ilmaistuna 1-numeroisella koodilla")
  arvioinnistaPäättäneet: KoodistoKoodiViite,
  @KoodistoUri("ammatillisennaytonarviointikeskusteluunosallistuneet")
  @Description("Arviointikeskusteluun osallistuneet tahot, ilmaistuna 1-numeroisella koodilla")
  arviointikeskusteluunOsallistuneet: KoodistoKoodiViite
)

case class NäytönArviointikohde(
  @Description("Arviointikohteen tunniste")
  @KoodistoUri("ammatillisennaytonarviointikohde")
  tunniste: KoodistoKoodiViite,
  @Description("Arvosana. Kullekin arviointiasteikolle löytyy oma koodistonsa")
  @KoodistoUri("arviointiasteikkoammatillinenhyvaksyttyhylatty")
  @KoodistoUri("arviointiasteikkoammatillinent1k3")
  arvosana: KoodistoKoodiViite
)

@Description("Oppisopimuksen tiedot")
case class Oppisopimus(
  työnantaja: Yritys
)

case class Läsnäolotiedot(
  @Description("Läsnä- ja poissaolojaksot päivämääräväleinä.")
  läsnäolojaksot: List[Läsnäolojakso]
)

trait Jakso {
  def alku: LocalDate
  def loppu: Option[LocalDate]
}

case class Läsnäolojakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  @Description("Läsnäolotila (läsnä, poissa...)")
  @KoodistoUri("lasnaolotila")
  tila: KoodistoKoodiViite
) extends Jakso

case class OpiskeluoikeudenTila(
  @Description("Opiskeluoikeuden tilahistoria (aktiivinen, keskeyttänyt, päättynyt...) jaksoittain. Sisältää myös tiedon opintojen rahoituksesta jaksoittain.")
  opiskeluoikeusjaksot: List[Opiskeluoikeusjakso]
)

case class Opiskeluoikeusjakso(
  alku: LocalDate,
  loppu: Option[LocalDate],
  @Description("Opiskeluoikeuden tila (aktiivinen, keskeyttänyt, päättynyt...)")
  @KoodistoUri("opiskeluoikeudentila")
  tila: KoodistoKoodiViite,
  @Description("Opintojen rahoitus")
  @KoodistoUri("opintojenrahoitus")
  opintojenRahoitus: Option[KoodistoKoodiViite]
) extends Jakso

case class Kunta(koodi: String, nimi: Option[String])

trait KoodiViite {
  def koodiarvo: String
  def koodistoUri: String
}

case class KoodistoKoodiViite(
  @Description("Koodin tunniste koodistossa")
  koodiarvo: String,
  @Description("Koodin selväkielinen, kielistetty nimi")
  @ReadOnly("Tiedon syötössä kuvausta ei tarvita; kuvaus haetaan Koodistopalvelusta")
  nimi: Option[String],
  @Description("Käytetyn koodiston tunniste")
  koodistoUri: String,
  @Description("Käytetyn koodiston versio. Jos versiota ei määritellä, käytetään uusinta versiota")
  koodistoVersio: Option[Int]
) extends KoodiViite {
  override def toString = koodistoUri + "/" + koodiarvo
  def koodistoViite = koodistoVersio.map(KoodistoViite(koodistoUri, _))
}

object KoodistoKoodiViite {
  def apply(koodiarvo: String, koodistoUri: String): KoodistoKoodiViite = KoodistoKoodiViite(koodiarvo, None, koodistoUri, None)
}

@Description("Henkilökohtainen opetuksen järjestämistä koskeva suunnitelma, https://fi.wikipedia.org/wiki/HOJKS")
@OksaUri("tmpOKSAID228", "erityisopiskelija")
case class Hojks(
  hojksTehty: Boolean,
  @KoodistoUri("opetusryhma")
  opetusryhmä: Option[KoodistoKoodiViite]
)

@Description("Paikallinen, koulutustoimijan oma kooditus koulutukselle. Käytetään kansallisen koodiston puuttuessa")
case class Paikallinenkoodi(
  @Description("Koodin tunniste koodistossa")
  koodiarvo: String,
  @Description("Koodin selväkielinen nimi")
  nimi: String,
  @Description("Koodiston tunniste")
  koodistoUri: String
) extends KoodiViite

@Description("Tutkinnon tai tutkinnon osan laajuus. Koostuu opintojen laajuuden arvosta ja yksiköstä")
case class Laajuus(
  @Description("Opintojen laajuuden arvo")
  @MinValue(0)
  arvo: Float,
  @Description("Opintojen laajuuden yksikkö")
  @KoodistoUri("opintojenlaajuusyksikko")
  yksikkö: KoodistoKoodiViite
)

case class LähdejärjestelmäId(
  @Description("Opiskeluoikeuden paikallinen uniikki tunniste lähdejärjestelmässä. Tiedonsiirroissa tarpeellinen, jotta voidaan varmistaa päivitysten osuminen oikeaan opiskeluoikeuteen.")
  id: String,
  @Description("Lähdejärjestelmän yksilöivä tunniste. Tällä tunnistetaan järjestelmä, josta tiedot on tuotu TOR:iin. " +
    "Kullakin erillisellä tietojärjestelmäinstanssilla tulisi olla oma tunniste. " +
    "Jos siis oppilaitoksella on oma tietojärjestelmäinstanssi, tulee myös tällä instanssilla olla uniikki tunniste.")
  @KoodistoUri("lahdejarjestelma")
  lähdejärjestelmä: KoodistoKoodiViite
)

@Description("Organisaatio. Voi olla Opintopolun organisaatiosta löytyvä oid:illinen organisaatio, y-tunnuksellinen yritys tai tutkintotoimikunta.")
sealed trait Organisaatio
  @Description("Opintopolun organisaatiopalvelusta löytyvä organisaatio. Esimerkiksi koulutustoimijat, oppilaitokset ja toimipisteet ovat tällaisia organisaatioita.")
  case class OidOrganisaatio(
    @Description("Organisaation tunniste Opintopolku-palvelussa")
    @RegularExpression("""1\.2\.246\.562\.10\.\d{11}""")
    oid: String,
    @Description("Organisaation (kielistetty) nimi")
    @ReadOnly("Tiedon syötössä nimeä ei tarvita; kuvaus haetaan Organisaatiopalvelusta")
    nimi: Option[String] = None
  ) extends OrganisaatioWithOid

  @Description("Opintopolun organisaatiopalvelusta löytyvä oppilaitos-tyyppinen organisaatio.")
  case class Oppilaitos(
     @Description("Organisaation tunniste Opintopolku-palvelussa")
     @RegularExpression("""1\.2\.246\.562\.10\.\d{11}""")
     oid: String,
     @Description("5-numeroinen oppilaitosnumero, esimerkiksi 00001")
     @ReadOnly("Tiedon syötössä oppilaitosnumeroa ei tarvita; numero haetaan Organisaatiopalvelusta")
     @KoodistoUri("oppilaitosnumero")
     oppilaitosnumero: Option[KoodistoKoodiViite] = None,
     @Description("Organisaation (kielistetty) nimi")
     @ReadOnly("Tiedon syötössä nimeä ei tarvita; kuvaus haetaan Organisaatiopalvelusta")
     nimi: Option[String] = None
  ) extends OrganisaatioWithOid

  @Description("Yritys, jolla on y-tunnus")
  case class Yritys(
    nimi: String,
    @RegularExpression("\\d{7}-\\d")
    yTunnus: String
  ) extends Organisaatio

  @Description("Tutkintotoimikunta")
  case class Tutkintotoimikunta(
    nimi: String,
    @MinValue(1)
    tutkintotoimikunnanNumero: Int
  ) extends Organisaatio

trait OrganisaatioWithOid extends Organisaatio {
  @Description("Organisaation tunniste Opintopolku-palvelussa")
  def oid: String
  @Description("Organisaation (kielistetty) nimi")
  @ReadOnly("Tiedon syötössä nimeä ei tarvita; kuvaus haetaan Organisaatiopalvelusta")
  def nimi: Option[String]
}