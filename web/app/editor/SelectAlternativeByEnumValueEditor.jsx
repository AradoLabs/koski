import React from 'baret'
import Bacon from 'baconjs'
import R from 'ramda'
import * as L from 'partial.lenses'
import {modelData, modelLookup} from './EditorModel.js'
import {fetchAlternativesBasedOnPrototypes} from './EnumEditor'
import {lensedModel, modelSetValue, oneOfPrototypes} from './EditorModel'
import {EnumEditor} from './EnumEditor'

export const SelectAlternativeByEnumValueEditor = ({ model, path, className }) => {
  return (<span className={className}>
        {
          fetchAlternativesBasedOnPrototypes(oneOfPrototypes(model), path).map(protos => {
            let enumValues = R.uniqBy(R.prop('value'), protos.map(proto => modelLookup(proto, path).value))
            let tunnisteModel = lensedModel(model, L.lens(
              (m) => modelLookup(m, path),
              (enumModel, m) => {
                // The proto with least options is the most specific one, choose that one.
                let protosMostSpecificFirst = R.compose(R.flatten, R.sortBy(a => a.length), R.values, R.groupBy(proto => proto.value.classes[0]))(protos)
                let foundProto = protosMostSpecificFirst.find(proto => R.equals(modelData(enumModel), modelData(proto, path)))
                return modelSetValue(m, foundProto.value)
              }
            ))

            return <EnumEditor model={ tunnisteModel } fetchAlternatives = { () => Bacon.constant(enumValues) }/>
          })
        }
  </span>)
}
