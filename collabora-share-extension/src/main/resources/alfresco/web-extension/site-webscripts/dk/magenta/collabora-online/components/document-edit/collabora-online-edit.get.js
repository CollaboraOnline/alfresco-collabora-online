<import resource="classpath:alfresco/site-webscripts/org/alfresco/callutils.js">
<import resource="classpath:/alfresco/templates/org/alfresco/import/alfresco-util.js">

    function main() {

        var wopi_host_url = '';
        var nodeRef = url.args.nodeRef,
            connector = remote.connect("alfresco");
        //try and get the Wopi service url first
        try {
            var result = connector.get('/lool/host/url');
            if (result.status.code == status.STATUS_OK) {
                wopi_host_url = JSON.parse(result.response).lool_host_url;
                if(wopi_host_url.charAt(wopi_host_url.length - 1) == '/')
                    wopi_host_url = wopi_host_url.substring(0, wopi_host_url.length -1);
                logger.log("\n\t\t wopi host url resolved: " + wopi_host_url);
            }
            else throw ("\t\twopi service url host returned: "+ result.status.code);
        }catch(e){
            logger.log('Error getting host url. (Using enpoint URL now):\n' + e);
            wopi_host_url = remote.getEndpointURL("alfresco");
        }

        try {
            var result = connector.get('/lool/token?nodeRef=' + encodeURI(nodeRef) + '&action=edit');
            if (result.status.code == status.STATUS_OK) {
                var post = JSON.parse(result.response);
                var wopi_src_url = post.wopi_src_url;
                var fileId = nodeRef.substring(nodeRef.lastIndexOf('/') + 1);
                var wopiFileURL = wopi_host_url + "/wopi/files/" + fileId;
                var params = "WOPISrc=" + encodeURI(wopiFileURL);
                params += "&closebutton=1";

                model.wopiFileURL = wopiFileURL;
                model.iFrameURL = wopi_src_url + params;
                model.access_token = post.access_token;
                model.access_token_ttl = post.access_token_ttl;
            }
            else
                throw "Unable to get permission token for document edit"
        }
        catch (err) {
            logger.log('\n\nThere was an error retrieving the lool token:\n' + err + '\n\n')
        }

        model.userId = user.id;
        model.firstName = user.firstName;
        model.lastName = user.lastName;

        // Widget instantiation metadata...
        var libreOfficeOnlineWidget = {
            id: "loolWidget",
            name: "Magenta.LibreOfficeOnline",
            options: {
                access_token: model.access_token,
                access_token_ttl: model.access_token_ttl,
                firstName: model.firstName,
                lastName: model.lastName,
                iFrameURL: model.iFrameURL,
                userId: model.userId,
                wopiFileURL: model.wopiFileURL,
                nodeRef: nodeRef
            }

        };
        model.widgets = [libreOfficeOnlineWidget];

        logger.log("\n---- Ending the show ----");
        logger.log("\n\t\t WOPI File url : " + model.wopiFileURL);
        logger.log("\n\t\t iFrame url : " + model.iFrameURL);
        logger.log("\n\t\t Access token : " + model.access_token);
        logger.log("\n\t\t Access token TTL : " + model.access_token_ttl);
    };

main();