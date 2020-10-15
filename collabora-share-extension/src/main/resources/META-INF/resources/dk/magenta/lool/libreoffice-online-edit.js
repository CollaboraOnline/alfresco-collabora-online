/**
 * LibreOffice edit online component.
 *
 * @namespace Magenta
 * @class Magenta.LibreOfficeOnline
 */
// Ensure Magenta root object exists
if (typeof Magenta == "undefined" || !Magenta) {
    var Magenta = {};

}


(function () {

    /**
     * Alfresco.CustomisePages constructor.
     *
     * @param {string} htmlId The HTML id of the parent element
     * @return {Alfresco.CustomisePages} The new CustomisePages instance
     * @constructor
     */
    Magenta.LibreOfficeOnline = function (htmlId) {
        return Magenta.LibreOfficeOnline.superclass.constructor.call(this, "Magenta.LibreOfficeOnline", htmlId, ["container"]);
    };

    YAHOO.extend(Magenta.LibreOfficeOnline, Alfresco.component.Base, {
        /**
         * Object container for initialization options
         *
         * @property options
         * @type object
         */
        options: {
            access_token: '',
            access_token_ttl: '',
            firstName: '',
            lastName: '',
            iFrameURL: '',
            userId: '',
            wopiFileURL: '',
            nodeRef: ''
        },

        /**
         * Fired by YUILoaderHelper when required component script files have
         * been loaded into the browser.
         *
         * @method onReady
         */
        onReady: function MLO_onReady() {
            var me = this;
            require(["jquery"], (function ($) {
                var form = '<form id="loleafletform" name="loleafletform" target="loleafletframe" action="' + me.options.iFrameURL + '" method="post">' +
                    '<input name="access_token" value="' + encodeURIComponent(me.options.access_token) + '" type="hidden"/>' +
                    '<input name="access_token_ttl" value="' + encodeURIComponent(me.options.access_token_ttl) + '" type="hidden"/>' +
                    '</form>';

                var frame = '<iframe id="loleafletframe" name= "loleafletframe" allowfullscreen="true" />';

                $('#loolcontainer').remove();

                var container = '<div id="loolcontainer"></div>';
                $('#collabora-online').append(container);
                var loolContainer = $('#loolcontainer');

                loolContainer.append(form);
                loolContainer.append(frame);

                $('#loleafletframe').load(function () {
                    console.log("Loaded loleafletframe");
                });

                $('#loleafletform').submit();
            }));
            
            /**
             * PostMessage Handler
             */            
            YAHOO.util.Event.addListener(window, "message", function handlePostMessage(e) {
                var msg = JSON.parse(e.data);

                var msgId = msg.MessageId;
                var msgData = msg.Values;
                
                switch (msgId) {
                case "close":
                    // ignore - deprecated -
                    break;
                    
                case "UI_Close":
                    // Go back to directory (or details-view ?)
                    console.log("PostMessage Recev: UI_Close - move to ");
                    var $siteURL = Alfresco.util.siteURL;
                    window.location.href = $siteURL("document-details") + "?nodeRef=" + me.options.nodeRef;
                    
                    break;
                    
                case "App_LoadingStatus":
                    console.log("PostMessage Recev: App_LoadingStatus - Status:" + msgData.Status);
                    break;

                case "View_Added":
                    console.log("PostMessage Recev: View_Added - Values:" + JSON.stringify(msgData));
                    break;
                    
                default:
                    console.log("PostMessage Recev: " + e.data);
                    break;
                }
            });

        }
    });
})();


        

