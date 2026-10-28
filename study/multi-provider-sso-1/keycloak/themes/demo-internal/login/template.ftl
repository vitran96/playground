<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false>
<!doctype html>
<html lang="en" data-theme="light">
<head>
  <meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${msg("loginTitle",(realm.displayName!realm.name))}</title>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css">
  <#if properties.styles?has_content><#list properties.styles?split(' ') as s>
    <link rel="stylesheet" href="${url.resourcesPath}/${s}"></#list></#if>
</head>
<body class="${bodyClass}">
  <main class="container">
    <article>
      <header><span class="realm-badge">${realm.name}</span><h2><#nested "header"></h2></header>
      <#if displayMessage && message?has_content>
        <p class="msg msg-${message.type}">${kcSanitize(message.summary)?no_esc}</p>
      </#if>
      <#nested "form">
      <#nested "socialProviders">
      <#if displayInfo><footer><#nested "info"></footer></#if>
    </article>
  </main>
</body>
</html>
</#macro>
