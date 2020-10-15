<import resource="classpath:/alfresco/extension/templates/webscripts/fr/jeci/collabora/aspect-collabora-online/aspect-collabora-online.lib.js">

function editNode(result, node) {
    result.action = "addAspect";

    if (!node.hasAspect(ASPECT_COLLABORA)) {
        node.addAspect(ASPECT_COLLABORA);
    }
}

main();
