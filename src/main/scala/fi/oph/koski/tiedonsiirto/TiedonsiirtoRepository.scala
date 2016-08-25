package fi.oph.koski.tiedonsiirto

import fi.oph.koski.db.KoskiDatabase.DB
import fi.oph.koski.db.Tables.{Tiedonsiirto, TiedonsiirtoRow, TiedonsiirtoWithAccessCheck}
import fi.oph.koski.db.{Futures, GlobalExecutionContext}
import fi.oph.koski.koskiuser.KoskiUser
import org.json4s.JsonAST.JValue
import fi.oph.koski.db.PostgresDriverWithJsonSupport.api._

class TiedonsiirtoRepository(db: DB) extends  GlobalExecutionContext with Futures {
  def create(kayttajaOid: String, tallentajaOrganisaatioOid:  String, error: Option[TiedonsiirtoError]) = {

    val (data, virheet) = error.map(e => (Some(e.data), Some(e.virheet))).getOrElse((None, None))

    db.run {
      Tiedonsiirto.map { row => (row.kayttajaOid, row.tallentajaOrganisaatioOid, row.data, row.virheet) } += (kayttajaOid, tallentajaOrganisaatioOid, data, virheet)
    }
  }

  def findByOrganisaatio(koskiUser: KoskiUser): Seq[TiedonsiirtoRow] =
    await(db.run(TiedonsiirtoWithAccessCheck(koskiUser).result))
}

case class TiedonsiirtoError(data: JValue, virheet: JValue)

