<#if ejb3?if_exists>
<#if pojo.isComponent()>
@${pojo.importType("jakarta.persistence.Embeddable")}
<#else>
@${pojo.importType("jakarta.persistence.Entity")}
@${pojo.importType("jakarta.persistence.Table")}(name="${clazz.table.name}"
<#assign uniqueConstraint=pojo.generateAnnTableUniqueConstraint()>
<#if uniqueConstraint?has_content>
    , uniqueConstraints = ${uniqueConstraint}
</#if>)
</#if>
</#if>
