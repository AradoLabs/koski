import React from 'react'
import {modelData} from './EditorModel.js'
import {wrapOptional} from './OptionalEditor.jsx'
import {pushModelValue, modelValid} from './EditorModel'

export const StringEditor = ({model, placeholder}) => {
  let wrappedModel = wrapOptional({model})
  let onChange = (event) => pushModelValue(wrappedModel, { data: event.target.value })
  let data = modelData(model)
  let error = !modelValid(model)
  return model.context.edit
    ? (model.maxLines
      ? <textarea className={error ? 'editor-input error' : 'editor-input valid'} defaultValue={data} placeholder={placeholder} onChange={ onChange } rows={ model.maxLines }></textarea>
      : <input className={error ? 'editor-input error' : 'editor-input valid'} type="text" defaultValue={data} placeholder={placeholder} onChange={ onChange }></input>)
    : <span className="inline string">{!data ? '' : splitToRows(data)}</span>
}

let splitToRows = (data) => data.split('\n').map((line, k) => <span key={k}>{k > 0 ? <br/> : null}{line}</span>)

const buildRegex = model => new RegExp(model.regularExpression.replace(/\\/g, '\\'))

StringEditor.handlesOptional = () => true
StringEditor.isEmpty = m => !modelData(m)
StringEditor.canShowInline = () => true
StringEditor.validateModel = (model) => {
  let data = modelData(model)
  if (!model.optional && !data) {
    return [{key: 'missing'}]
  }

  if (data && model.regularExpression && !buildRegex(model).test(data)) {
    return [{key: 'invalid.format'}]
  }
}
