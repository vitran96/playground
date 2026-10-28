<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
  <#if section = "header">
    Demo broker: choose how to sign in
  <#elseif section = "form">
    <p>You will be sent to your organisation's sign-in page.</p>
    <div style="display: flex; flex-direction: column; gap: 0.75rem; margin-top: 1rem;">
      <#if social?? && social.providers?has_content>
        <#list social.providers as p>
          <a role="button" id="social-${p.alias}" href="${p.loginUrl}">${p.displayName!}</a>
        </#list>
      </#if>
    </div>
  </#if>
</@layout.registrationLayout>
