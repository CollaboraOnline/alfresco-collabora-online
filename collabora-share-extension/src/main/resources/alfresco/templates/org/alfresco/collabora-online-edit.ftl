<#--
SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr

SPDX-License-Identifier: Apache-2.0
-->

<#include "include/alfresco-template.ftl" />

<#-- ?fullwidth=true renders the editor alone, without the Share header/footer chrome -->
<#assign fullwidth = (page.url.args.fullwidth!"false") == "true">

<#if fullwidth>
<@templateHeader />

<body id="Share" class="yui-skin-${theme} alfresco-share claro collabora-fullscreen">

    <div id="bd">
        <@region id="collabora-online-edit" scope="template" />
    </div>

    <#-- This function call MUST come after all other component includes. -->
    <div id="alfresco-yuiloader"></div>
    <#-- <@relocateJavaScript/> -->

    <script type="text/javascript">//<![CDATA[
        Alfresco.util.YUILoaderHelper.loadComponents(true);
        <#-- Security - ensure user has a currently authenticated Session when viewing a user auth page e.g. when Back button is used -->
        <#if page?? && (page.authentication="user" || page.authentication="admin")>
        Alfresco.util.Ajax.jsonGet({
        url: Alfresco.constants.URL_CONTEXT + "service/modules/authenticated?noCache=" + new Date().getTime() + "&a=${page.authentication?html}"
        });
        </#if>
    //]]></script>

</body>
</html>
<#else>
<@templateHeader />

<@templateBody>
    <@markup id="alf-hd">
    <div id="alf-hd">
        <@region scope="global" id="share-header" chromeless="true"/>
    </div>
    </@>
    <@markup id="bd">
    <div id="bd">
        <@region id="collabora-online-edit" scope="template" />
    </div>
    </@>
</@>

<@templateFooter>
    <@markup id="alf-ft">
        <div id="alf-ft">
            <@region id="footer" scope="global"/>
        </div>
    </@>
</@>
</#if>
