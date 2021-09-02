<html>
<head>
  <title>Validation Report</title>
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
    height: 64px;
    width: 100%;
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

  .container {
    padding: 0.5rem;
    margin-bottom: 0.5rem;
    display: flex;
    flex-direction: column;
  }
  </style>
</head>
<body>
  <header>
    <h1>Fedora migration validation summary - ${date}</h1>
  </header>

  <div class="container">
    <h2>Summary</h2>
    <table>
      <tr>
        <td>Total objects:</td>
        <td>${objectCount}</td>
      </tr>
      <tr>
        <td>Failed objects:</td>
        <td>${errorCount}</td>
      </tr>
    </table>
  </div>

  <#if errorCount gt 0>
  <div class="container">
    <h2>Objects with errors</h2>
    <ol>
      <#list errors as x>
      <li><a href='${x.reportHref}'>${x.reportFilename}</a></li>
      </#list>
    </ol>
  </div>
  </#if>

  <div class="container">
    <h2>Object result details</h2>
    <ol>
      <#list objects as x>
      <li><a href='${x.reportHref}'>${x.reportFilename}</a></li>
      </#list>
    </ol>
  </div>
</body>
</html>
