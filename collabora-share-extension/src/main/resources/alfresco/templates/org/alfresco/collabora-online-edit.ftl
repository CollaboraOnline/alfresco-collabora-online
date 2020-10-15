<#include "include/alfresco-template.ftl" />

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
