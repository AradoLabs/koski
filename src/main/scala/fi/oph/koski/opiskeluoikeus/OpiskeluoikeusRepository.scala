package fi.oph.koski.opiskeluoikeus

import fi.oph.koski.db.OpiskeluoikeusRow
import fi.oph.koski.henkilo.PossiblyUnverifiedHenkilöOid
import fi.oph.koski.http.HttpStatus
import fi.oph.koski.koskiuser.KoskiSession
import fi.oph.koski.schema._
import org.json4s.JValue

trait OpiskeluoikeusRepository extends AuxiliaryOpiskeluoikeusRepository {
  def findByOid(oid: String)(implicit user: KoskiSession): Either[HttpStatus, OpiskeluoikeusRow]
  def getOppijaOidForOpiskeluoikeus(opiskeluoikeusOid: String)(implicit user: KoskiSession): Either[HttpStatus, Henkilö.Oid]
  def delete(id: Int)(implicit user: KoskiSession): HttpStatus
  def createOrUpdate(oppijaOid: PossiblyUnverifiedHenkilöOid, opiskeluoikeus: KoskeenTallennettavaOpiskeluoikeus, allowUpdate: Boolean)(implicit user: KoskiSession): Either[HttpStatus, CreateOrUpdateResult]
  def filterOppijat(oppijat: Seq[HenkilötiedotJaOid])(implicit user: KoskiSession): Seq[HenkilötiedotJaOid]
  def findByOppijaOid(oid: String)(implicit user: KoskiSession): Seq[Opiskeluoikeus]
  def findByUserOid(oid: String)(implicit user: KoskiSession): Seq[Opiskeluoikeus]
}

trait AuxiliaryOpiskeluoikeusRepository {
  def filterOppijat(oppijat: Seq[HenkilötiedotJaOid])(implicit user: KoskiSession): Seq[HenkilötiedotJaOid]
  def findByOppijaOid(oid: String)(implicit user: KoskiSession): Seq[Opiskeluoikeus]
}

sealed trait CreateOrUpdateResult {
  def changed: Boolean

  def id: Opiskeluoikeus.Id
  def oid: Opiskeluoikeus.Oid
  def versionumero: Int
  def diff: JValue
  def data: JValue
}

case class Created(id: Opiskeluoikeus.Id, oid: Opiskeluoikeus.Oid, versionumero: Opiskeluoikeus.Versionumero, diff: JValue, data: JValue) extends CreateOrUpdateResult {
  def changed = true
}
case class Updated(id: Opiskeluoikeus.Id, oid: Opiskeluoikeus.Oid, versionumero: Opiskeluoikeus.Versionumero, diff: JValue, data: JValue, old: KoskeenTallennettavaOpiskeluoikeus) extends CreateOrUpdateResult {
  def changed = true
}
case class NotChanged(id: Opiskeluoikeus.Id, oid: Opiskeluoikeus.Oid, versionumero: Opiskeluoikeus.Versionumero, diff: JValue, data: JValue) extends CreateOrUpdateResult {
  def changed = false
}
