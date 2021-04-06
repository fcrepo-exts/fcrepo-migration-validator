<html>
<head>
  <title>Object Report - ${id}</title>
  <style>
  html {
    font-family: sans-serif;
  }

  body {
    margin: 0;
    font-family: var(--bs-font-sans-serif);
    font-size: 1rem;
    font-weight: 400;
    line-height: 1.5;
  }

  header {
    width: 100%;
    height: 64px;
    line-height: 64px;
    text-align: center;
    border-bottom: solid 1px;
  }

  header h1 {
    font-size: 2rem;
    padding: 0;
    margin: auto;
  }

  h2 {
    margin-bottom: 0.5rem;
  }

  table {
    border-collapse: collapse;
    border: 1px solid rgb(150,150,150);
    letter-spacing: 1px;
    font-size: 0.8rem;
  }

  td, th {
    border: 1px solid rgb(190,190,190);
    border-width: 0 0 2px;
    padding: 10px 20px;
  }

  th {
    background-color: rgb(235,235,235);
    text-transform: capitalize;
  }

  td {
    text-align: center;
  }

  tr td {
    background-color: rgb(255,255,255);
    border-width: 0 0 1px;
  }

  .summary {
    display: inline-block;
    padding: .35em .65em;
    font-size: .75rem;
    font-weight: 700;
    line-height: 1;
    text-align: center;
    white-space: nowrap;
    vertical-align: baseline;
    border-radius: .25rem;
  }

  .pass {
    background-color: #5ff27d;
  }

  .fail {
    background-color: #ff5b5b;
  }

  .container {
    padding: 0.5rem;
    margin-bottom: 0.5rem;
    display: flex;
    flex-direction: column;
  }

  .linkback {
    text-decoration: none;
    font-size: 1.2rem;
    padding: 0 15px;
    float: left;
  }
  </style>
</head>
<body>
  <header>
    <a class="linkback" href="index.html">To Summary</a>
    <h1>Validation Results For ${id}</h1>
  </header>

  <p class="summary"> Validation Status:
  <#if successCount gt 0>
  <span class="summary pass">Passed: ${successCount}</span>
  </#if>
  <#if errorCount gt 0>
  <span class="summary fail">Failed: ${errorCount}</span>
  </#if>
  </p>

  <div class="container">
    <h2>Failed Validations</h2>
    <div style="overflow-x: auto">
      <table>
        <thead>
          <tr>
            <th>index</th>
            <th>status</th>
            <th>validation level</th>
            <th>validation type</th>
            <th>details</th>
            <th>source object</th>
            <th>target object</th>
            <th>source resource</th>
            <th>target resource</th>
          </tr>
        </thead>
        <tbody>
          <#list errors as error>
          <tr>
            <td>${error.index}</td>
            <td>${error.status}</td>
            <td>${error.validationLevel}</td>
            <td>${error.validationType}</td>
            <td>${error.details}</td>
            <td>${error.sourceObjectId!'dne'}</td>
            <td>${error.targetObjectId!'dne'}</td>
            <td>${error.sourceResourceId!'dne'}</td>
            <td>${error.targetResourceId!'dne'}</td>
          </tr>
          </#list>
        </tbody>
      </table>
    </div>
  </div>

  <div class="container">
    <h2>Successful Validations</h2>
    <div style="overflow-x: auto">
      <table>
        <thead>
          <tr>
            <th>index</th>
            <th>status</th>
            <th>validation level</th>
            <th>validation type</th>
            <th>details</th>
            <th>source object</th>
            <th>target object</th>
            <th>source resource</th>
            <th>target resource</th>
          </tr>
        </thead>
        <tbody>
          <#list success as s>
          <tr>
            <td>${s.index}</td>
            <td>${s.status}</td>
            <td>${s.validationLevel}</td>
            <td>${s.validationType}</td>
            <td>${s.details}</td>
            <td>${s.sourceObjectId!'dne'}</td>
            <td>${s.targetObjectId!'dne'}</td>
            <td>${s.sourceResourceId!'dne'}</td>
            <td>${s.targetResourceId!'dne'}</td>
          </tr>
          </#list>
        </tbody>
      </table>
    </div>
  </div>
</body>
</html>
