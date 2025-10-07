<#--
SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr

SPDX-License-Identifier: Apache-2.0
-->

<@markup id="css" >
   <#-- CSS Dependencies -->
   <@link href="${url.context}/res/fr/jeci/collabora-online/node-header/node-header.css" group="collabora"/>
</@>

<@markup id="html">
    <#if editing>
        <@uniqueIdDiv>
            <div class="collabora-node-header">
                <span class="collabora-editing">${msg("collabora.editing")}</span>
            </div>
        </@>
    </#if>
</@>
