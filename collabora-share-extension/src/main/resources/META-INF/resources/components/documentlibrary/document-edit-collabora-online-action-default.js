YAHOO.Bubbling.fire("registerAction", {
    actionName: "onActionEditInCollaboraOnline",
    fn: function (node) {
        var COLLABORA_GET_TOKEN = Alfresco.constants.PROXY_URI + "collabora/token";

        Alfresco.util.Ajax.jsonGet(
            {
                url: COLLABORA_GET_TOKEN + "?nodeRef=" + encodeURIComponent(node.nodeRef) + "&action=edit",
                successCallback: {
                    fn: function (response) {
                        var access_token = response.json.access_token;
                        var wopi_src_url = response.json.wopi_src_url;
                        console.log(access_token, wopi_src_url);

                        // Get fileId from nodeRef (just use the uuid part)
                        var fileId = Alfresco.util.NodeRef(node.nodeRef).id;

                        var wopiFileURL = window.location.origin+"/alfresco/s/wopi/files/" + fileId;

                        var frameSrcURL = wopi_src_url + "WOPISrc=" + encodeURIComponent(wopiFileURL);

                        require(["jquery"], (function ($) {
                            var form = '<form id="loleafletform" name="loleafletform" target="loleafletframe" action="' + frameSrcURL + '" method="post">' +
                                '<input name="access_token" value="' + encodeURIComponent(access_token) + '" type="hidden"/></form>';

                            var frame = '<iframe id="loleafletframe" name= "loleafletframe" allowfullscreen />';

                            $('#coolcontainer').remove();

                            var container = '<div id="coolcontainer"></div>';
                            $('body').append(container);
                            var coolContainer = $('#coolcontainer');

                            coolContainer.append(form);
                            coolContainer.append(frame);

                            $('#loleafletframe').load(function () {
                                console.log("Loaded loleafletframe");
                            });

                            $('#loleafletform').submit();
                        }));
                    },
                    scope: this
                },
                failureMessage: "Server error - could not lookup token!"
            }
        );
    }
});