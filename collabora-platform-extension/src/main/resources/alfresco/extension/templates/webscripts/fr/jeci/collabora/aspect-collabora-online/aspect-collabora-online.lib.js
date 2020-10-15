<import resource="classpath:/alfresco/templates/webscripts/org/alfresco/slingshot/documentlibrary/parse-args.lib.js">

var ASPECT_COLLABORA = "collabora:collaboraOnline";

/**
 * Main script entry point
 *
 * @method main
 */
function main() {
    // Params object contains commonly-used arguments
    var params = {}, files, rootNode, result;

    logger.log("StoreType=" + url.templateArgs.store_type);

    if (url.templateArgs.store_type != undefined) {
        params = getNodeRefInputParams();
    } else {
        status.setCode(status.STATUS_BAD_REQUEST, params);
        return;
    }

    logger.log("Params="+params);
    if (typeof params == "string") {
        status.setCode(status.STATUS_BAD_REQUEST, params);
        return;
    }

    try {
        var node = params.rootNode;

        result = {
            nodeRef : node.nodeRef.toString(),
            action : "_action_",
            success : false
        }

        result.id = node.name;

        policies.disableForNode(node);

        editNode(result, node);

        node.save();
        node.reset();
        policies.enableForNode(node);

        result.success = true;

    } catch (e) {
        e.code = status.STATUS_INTERNAL_SERVER_ERROR;
        e.message = e.toString();
        throw e;
    }

    model.result = result;
}

/**
 * Get and check existence of mandatory input parameters (nodeRef-based)
 *
 * @method getNodeRefInputParams
 * @return {object|string} object literal containing parameters value or string
 *         error
 */
function getNodeRefInputParams() {
    var params = {}, error = null;
logger.log("getNodeRefInputParams");
    try {
        // First try to get the parameters from the URI
        var storeType = url.templateArgs.store_type,
            storeId = url.templateArgs.store_id,
            id = url.templateArgs.id;

        var nodeRef = storeType + "://" + storeId + (id == null ? "" : ("/" + id)),
            rootNode = ParseArgs.resolveNode(nodeRef);

        if (rootNode === null) {
            return "'" + nodeRef + "' is not a valid nodeRef.";
        }

        var rootNodeRef = String(rootNode.nodeRef);
        if (rootNodeRef != nodeRef) {
            nodeRef = rootNodeRef;
        }

        // Populate the return object
        params = {
            nodeRef : nodeRef,
            rootNode : rootNode
        };
    } catch (e) {
        error = e.toString();
    }

    // Return the params object, or the error string if it was set
    return (error !== null ? error : params);
}
