<#import "template.ftl" as layout>
<#import "field.ftl" as field>
<#import "buttons.ftl" as buttons>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('emailCode'); section>
    <#if section = "header">
        ${msg("emailOtpTitle")}
    <#elseif section = "form">
        <form id="kc-email-otp-form" class="${properties.kcFormClass!}" action="${url.loginAction}" method="post">
            <@field.input name="emailCode" label=msg("emailOtpLabel") autocomplete="one-time-code" autofocus=true />
            <@buttons.actionGroup>
                <@buttons.button id="kc-submit" label="doSubmit"/>
                <@buttons.button id="kc-resend" name="resend" type="secondary" label="emailOtpResend"/>
                <@buttons.buttonLink href=url.loginUrl label="backToLogin"/>
            </@buttons.actionGroup>
        </form>
    </#if>
</@layout.registrationLayout>
