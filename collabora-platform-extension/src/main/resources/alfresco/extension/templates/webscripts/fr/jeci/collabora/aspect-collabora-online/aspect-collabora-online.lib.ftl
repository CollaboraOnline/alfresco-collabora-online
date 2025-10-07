<#--
SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr

SPDX-License-Identifier: Apache-2.0
-->

<#macro resultsJSON result>
   <#escape x as jsonUtils.encodeJSONString(x)>
{
   <#list result?keys as key>
      <#assign value = result[key]>
      <#if value?is_number || value?is_boolean>
   "${key}": ${value?string}<#if key_has_next>,</#if>
      <#else>
   "${key}": "${value}"<#if key_has_next>,</#if>
      </#if>
      </#list>
}
   </#escape>
</#macro>