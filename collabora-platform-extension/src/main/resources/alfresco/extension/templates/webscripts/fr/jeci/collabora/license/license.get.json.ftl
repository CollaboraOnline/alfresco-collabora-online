{
  "enabled": ${enabled?c},
  "count": ${count?c},
  "maxLicenses": ${maxLicenses?c},
  "users": [<#list users as user>"${user?js_string}"<#if user_has_next>, </#if></#list>]
}
