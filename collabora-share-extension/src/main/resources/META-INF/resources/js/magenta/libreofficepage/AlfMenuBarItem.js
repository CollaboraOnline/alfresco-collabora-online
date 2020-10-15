define(["dojo/_base/declare", "alfresco/menus/AlfMenuBarItem"],
    function(declare, AlfMenuBarItem) {

        return declare([AlfMenuBarItem], {
            /**
             * An array of the i18n files to use with this widget.
             *
             * @instance
             * @type {object[]}
             * @default [{i18nFile: "./i18n/AlfMenuBarToggle.properties"}]
             */
            i18nRequirements: [{i18nFile: "./i18n/AlfMenuBarItemExt.properties"}],

            onClick: function alfresco_menus__AlfMenuItemMixin__onClick(evt) {
                if(this.targetUrl == '#back')
                    console.log('Target url is back');
                window.location.href = document.referrer;
            }

        });
    });