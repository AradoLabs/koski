import React from 'baret'
import Bacon from 'baconjs'
import Atom from 'bacon.atom'
import {koodiarvoMatch, koodistoValues} from './koodisto'
import {makeSuoritus, oppiaineetP} from './PerusopetuksenSuoritus'
import Suoritustyyppi from './Suoritustyyppi'
import Peruste from './Peruste'

export default ({suoritusAtom, oppilaitosAtom, suorituskieliAtom}) => {
  const suoritustyyppiAtom = Atom()
  const perusteAtom = Atom()
  const suoritustyypitP = koodistoValues('suorituksentyyppi/perusopetuksenoppimaara')
  suoritustyypitP.onValue(tyypit => suoritustyyppiAtom.set(tyypit.find(koodiarvoMatch('perusopetuksenoppimaara'))))

  Bacon.combineWith(oppilaitosAtom, suoritustyyppiAtom, perusteAtom, oppiaineetP(suoritustyyppiAtom), suorituskieliAtom, makeSuoritus)
    .onValue(suoritus => suoritusAtom.set(suoritus))

  return (<span>
    <Suoritustyyppi suoritustyyppiAtom={suoritustyyppiAtom} suoritustyypitP={suoritustyypitP} title="Oppimäärä"/>
    <Peruste {...{suoritusTyyppiP: suoritustyyppiAtom, perusteAtom}} />
  </span>)
}