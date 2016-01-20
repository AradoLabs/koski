import React from 'react'
import Bacon from 'baconjs'
import Http from './http'
import {routeP} from './router'
import {CreateOppija} from './CreateOppija.jsx'
import {OpiskeluOikeus, opiskeluOikeusChange} from './OpiskeluOikeus.jsx'
import Immutable from 'immutable'

export const selectOppijaE = routeP.map('.oppijaId').flatMap(oppijaId => {
  return oppijaId ? Bacon.once({loading: true}).concat(Http.get(`/tor/api/oppija/${oppijaId}`)) : Bacon.once({ empty: true})
})

export const updateResultE = Bacon.Bus()

const applyChange = (oppija, opiskeluOikeusId, change) => {
  let current = Immutable.fromJS(oppija)
  return current.set('opiskeluoikeudet', current.get('opiskeluoikeudet').map(opiskeluOikeus => {
      return (opiskeluOikeus.get('id') == opiskeluOikeusId
        ? change(opiskeluOikeus)
        : opiskeluOikeus)}
  )).toJS()
}

export const oppijaP = Bacon.update({ loading: true },
  selectOppijaE, (previous, oppija) => oppija,
  updateResultE.map('.opiskeluoikeudet').flatMap(Bacon.fromArray), (currentOppija, {id, versionumero}) => {
    return applyChange(currentOppija, id, (opiskeluoikeus) => opiskeluoikeus.set('versionumero', versionumero))
  },
  opiskeluOikeusChange, (currentOppija, [opiskeluOikeusId, change]) => applyChange(currentOppija, opiskeluOikeusId, change)
)

updateResultE.plug(oppijaP.sampledBy(opiskeluOikeusChange).flatMapLatest(oppijaUpdate => Http.put('/tor/api/oppija', oppijaUpdate)))

export const uusiOppijaP = routeP.map(route => { return !!route.uusiOppija })

export const Oppija = ({oppija}) =>
  oppija.valittuOppija.loading
    ? <Loading/>
    : (!oppija.valittuOppija.empty
      ? <ExistingOppija oppija={oppija.valittuOppija}/>
      : (
      oppija.uusiOppija
        ? <CreateOppija/>
        : <div></div>
      ))

const Loading = () => <div className='main-content oppija loading'></div>

const ExistingOppija = React.createClass({
  render() {
    let {oppija: { henkilö: henkilö, opiskeluoikeudet: opiskeluoikeudet}} = this.props
    return (
      <div className='main-content oppija'>
        <h2>{henkilö.sukunimi}, {henkilö.etunimet} <span className='hetu'>{henkilö.hetu}</span></h2>
        <hr></hr>
        { opiskeluoikeudet.map( opiskeluOikeus =>
          <OpiskeluOikeus key={opiskeluOikeus.id} opiskeluOikeus={ opiskeluOikeus } />
        ) }
      </div>
    )
  }
})