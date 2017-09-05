import React from 'react'
import {PäivämääräväliEditor} from './PaivamaaravaliEditor.jsx'
import {PropertiesEditor} from './PropertiesEditor.jsx'
import {wrapOptional} from './OptionalEditor.jsx'
import {recursivelyEmpty} from './EditorModel'

export class JaksoEditor extends React.Component {
  render() {
    let {model, className} = this.props
    let wrappedModel = wrapOptional({model: model})
    return (
      <div className="jaksollinen">
        <PäivämääräväliEditor model={wrappedModel}/>
        <PropertiesEditor model={wrappedModel} propertyFilter={p => !['alku', 'loppu'].includes(p.key)} className={className} />
      </div>
    )
  }
}

JaksoEditor.validateModel = PäivämääräväliEditor.validateModel
JaksoEditor.isEmpty = recursivelyEmpty
JaksoEditor.handlesOptional = (modifier) => modifier != 'array'
