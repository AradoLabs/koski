package fi.oph.koski.documentation

import fi.oph.koski.schema.JsonSerializer
import fi.oph.koski.schema.JsonSerializer.serializeWithRoot
import org.json4s.jackson.JsonMethods

import scala.xml.Elem

object ApiTesterHtml {
  def apiOperationsHtml =
    <div> {
      KoskiApiOperations.operations.map { operation =>
        <div class="api-operation">
          <h3>
            <span class="api-method">{operation.method}</span>{operation.path}
          </h3>
          <div class="summary">{operation.summary}</div>

          <div class="api-details">
            {operation.doc}
          <div class="status-codes">
            <h4><a>Vastaukset ja paluukoodit</a></h4>
            <table>
              <thead>
                <tr>
                  <th>HTTP-status</th>
                  <th>Virhekoodi <small>(JSON-vastauksen sisällä)</small></th>
                  <th>Tilanne</th>
                  <th>Esimerkkivastaus</th>
                </tr>
              </thead>
              <tbody>
                {operation.statusCodes.flatMap(_.flatten).map { errorCategory =>
                <tr>
                  <td>
                    {errorCategory.statusCode}
                  </td>
                  <td>
                    {if (errorCategory.statusCode != 200) {errorCategory.key} else {""}}
                  </td>
                  <td>
                    {errorCategory.message}
                  </td>
                  <td>
                    <span class="example-response">
                      <a class="show-json">Näytä JSON</a>
                      <span class="json-popup">
                        <a class="close">Sulje</a>
                        <pre><code>{ JsonMethods.compact(errorCategory.exampleResponse) }</code></pre>
                      </span>
                    </span>
                  </td>
                </tr>
              }}
              </tbody>
            </table>
          </div>
          <h4>Kokeile heti</h4>
          { apiOperationTesterHtml(operation) }
        </div>
        </div>
      }
      }
    </div>

  def apiOperationTesterHtml(operation: ApiOperation) = {
    <div class="api-tester" data-method={operation.method} data-path={operation.path}>
      {apiOperationTesterParametersHtml(operation)}
      <div class="buttons">
        <button class="try button blue">Kokeile</button>
        <button class="try-newwindow button blue">uuteen ikkunaan</button>
        <button class="curl button blue"><span class="title">curl</span><span class="line"></span></button>
      </div>
      <div class="result"></div>
    </div>
  }

  def apiOperationTesterParametersHtml(operation: ApiOperation): Elem = {
    if (operation.examples.nonEmpty) {
      postDataExamplesHtml(operation)
    } else if (operation.parameters.nonEmpty) {
      queryParametersHtml(operation)
    } else {
      <div></div>
    }
  }

  def queryParametersHtml(operation: ApiOperation): Elem = {
    <div class="parameters">
      <h4>Parametrit</h4>
      <table>
        <thead>
          <tr>
            <th>Nimi</th> <th>Merkitys</th> <th>Arvo</th>
          </tr>
        </thead>
        <tbody>
          {operation.parameters.map { parameter =>
          <tr>
            <td>
              {parameter.name}
            </td> <td>
            {parameter.description}
          </td>
            <td>
            {if (parameter.hasMultipleExamples) {
              <select name={parameter.name} defaultValue={parameter.example} class="path-param">
                {parameter.examples.map(example => <option value={example} key={example}>{example}</option>)}
              </select>
            } else {
              <input name={parameter.name} value={parameter.example} class={parameter match {
                case p: QueryParameter => "query-param"
                case p: PathParameter => "path-param"
              }}></input>
            }
            }
            </td>
          </tr>
        }}
        </tbody>
      </table>
    </div>
  }

  def postDataExamplesHtml(operation: ApiOperation): Elem = {
    <div class="postdata">
      <h4>Syötedata</h4>
      <div class="examples">
        <label>Esimerkkejä
          <select>
            {operation.examples.map { example =>
            <option data-exampledata={JsonSerializer.writeWithRoot(example.data, pretty = true)}>
              {example.name}
            </option>
          }}
          </select>
        </label>
      </div>
      <textarea cols="80" rows="50">

      </textarea>
    </div>
  }
}