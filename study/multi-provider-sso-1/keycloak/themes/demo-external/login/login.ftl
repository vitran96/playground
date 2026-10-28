<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username','password'); section>
  <#if section = "header">
    Partner portal sign-in
  <#elseif section = "form">
    <p class="banner-partner"><strong>Accounts are issued by our infra team</strong></p>
    <form id="kc-form-login" action="${url.loginAction}" method="post">
      <#if messagesPerField.existsError('username','password')>
        <p class="msg msg-error" style="color: var(--pico-del-color);">${kcSanitize(messagesPerField.getFirstError('username','password'))?no_esc}</p>
      </#if>
      <label for="username">Username
        <input id="username" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="username" required />
      </label>
      <label for="password">Password
        <input id="password" name="password" type="password" autocomplete="current-password" required />
      </label>
      <input type="hidden" name="credentialId" <#if auth?? && auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
      <button type="submit">Sign in</button>
    </form>
  </#if>
</@layout.registrationLayout>
