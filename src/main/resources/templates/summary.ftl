<html>
<head>
  <title>Validation Report</title>
</head>
<body>
  <h1>Fedora migration validation summary - ${date}</h1>

  <h2>Summary</h2>
  <table>
    <tr>
        <td>Total objects:</td>
        <td>${objectCount}</td>
    </tr>
  </table>

  <h2>Object result details</h2>
  <ol>
    <#list objects as x>
        <li><a href='${x}'>${x}</a></li>
    </#list>
  </ol>
</body>
</html>
