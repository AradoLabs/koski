package fi.oph.koski.henkilo

import fi.oph.koski.cache._
import fi.oph.koski.config.KoskiApplication
import fi.oph.koski.http.HttpStatus
import fi.oph.koski.koskiuser.KoskiSession
import fi.oph.koski.log.TimedProxy
import fi.oph.koski.perustiedot.OpiskeluoikeudenPerustiedotRepository
import fi.oph.koski.schema.{Henkilö, HenkilötiedotJaOid, TäydellisetHenkilötiedot, UusiHenkilö}
import fi.oph.koski.virta.VirtaHenkilöRepository
import fi.oph.koski.ytr.YtrHenkilöRepository
import scala.concurrent.duration._

trait FindByOid {
  def findByOid(oid: String): Option[TäydellisetHenkilötiedot]
}

trait FindByHetu {
  def findByHetu(query: String)(implicit user: KoskiSession): Option[HenkilötiedotJaOid]
}

object HenkilöRepository {
  def apply(application: KoskiApplication)(implicit cacheInvalidator: CacheManager): HenkilöRepository = {
    val opintopolku = new OpintopolkuHenkilöRepository(application.opintopolkuHenkilöFacade, application.koodistoViitePalvelu)
    HenkilöRepository(
      opintopolku,
      TimedProxy(VirtaHenkilöRepository(application.virtaClient, opintopolku, application.virtaAccessChecker).asInstanceOf[FindByHetu]),
      TimedProxy(YtrHenkilöRepository(application.ytrClient, opintopolku, application.ytrAccessChecker).asInstanceOf[FindByHetu]),
      application.perustiedotRepository
    )
  }
}

case class HenkilöRepository(opintopolku: OpintopolkuHenkilöRepository, virta: FindByHetu, ytr: FindByHetu, perustiedotRepository: OpiskeluoikeudenPerustiedotRepository)(implicit cacheInvalidator: CacheManager) extends FindByOid {
  private val oidCache: KeyValueCache[String, Option[TäydellisetHenkilötiedot]] =
    KeyValueCache(new ExpiringCache("HenkilöRepository", ExpiringCache.Params(1 hour, maxSize = 100, storeValuePredicate = {
      case (_, value) => value != None // Don't cache None results
    })), opintopolku.findByOid)
  // findByOid is locally cached
  def findByOid(oid: String): Option[TäydellisetHenkilötiedot] = oidCache(oid)
  // Other methods just call the non-cached implementation

  def findByOids(oids: List[String]): List[TäydellisetHenkilötiedot] = opintopolku.findByOids(oids)

  def findOrCreate(henkilö: UusiHenkilö): Either[HttpStatus, TäydellisetHenkilötiedot] = opintopolku.findOrCreate(henkilö)

  def findHenkilötiedotByOid(oid: String)(implicit user: KoskiSession): List[HenkilötiedotJaOid] = HenkilöOid.validateHenkilöOid(oid) match {
    case Right(validHetu) => findByOid(oid).map(_.toHenkilötiedotJaOid).toList
    case Left(status) => throw new Exception(status.errorString.mkString)
  }

  def findHenkilötiedotByHetu(hetu: String)(implicit user: KoskiSession): List[HenkilötiedotJaOid] = Hetu.validFormat(hetu) match {
    case Right(validHetu) => List(opintopolku, virta, ytr).iterator.map(_.findByHetu(validHetu)).find(_.isDefined).toList.flatten
    case Left(status) => throw new Exception(status.errorString.mkString)
  }

  def findHenkilötiedot(query: String)(implicit user: KoskiSession): List[HenkilötiedotJaOid] =
    findByOids(perustiedotRepository.findOids(query)).map(_.toHenkilötiedotJaOid)
}