<html>
<head>
    <title>Repository Report</title>
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

  .linkback {
    text-decoration: none;
    font-size: 1.2rem;
    padding: 0 15px;
    float: left;
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
    <a class="linkback" href="index.html">To Summary</a>
    <h1>Repository Validation Results</h1>
</header>

<div class="container">
    <div style="overflow-x: auto">
        <table>
            <thead>
            <tr>
                <th>status</th>
                <th>validation level</th>
                <th>validation type</th>
                <th>details</th>
            </tr>
            </thead>
            <tbody>
            <#list validations as validation>
            <tr>
                <td>${validation.status}</td>
                <td>${validation.validationLevel}</td>
                <td>${validation.validationType}</td>
                <td>${validation.details}</td>
            </tr>
            </#list>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>
