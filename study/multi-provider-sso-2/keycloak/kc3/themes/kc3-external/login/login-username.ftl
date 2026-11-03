<#-- Identity-first page rendered by the Organization authenticator: email only; @internal.email.com is auto-redirected to internal-kc1 (keycloak-1). -->
<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
    <#elseif section = "form">
        <form id="kc-form-login" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post" novalidate="novalidate">
            <@field.input name="username" label=msg("email") value=login.username!'' autocomplete="email" autofocus=true />
            <@buttons.actionGroup>
                <@buttons.button id="kc-login" name="login" label="doContinue"/>
            </@buttons.actionGroup>
        </form>
    <#elseif section = "info">
        <div id="kc-registration">
            <span>${msg("noAccount")} <a href="${url.registrationUrl}">${msg("doRegister")}</a></span>
        </div>
    </#if>
</@layout.registrationLayout>
