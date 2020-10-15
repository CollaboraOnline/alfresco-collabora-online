<import resource="classpath:/alfresco/site-webscripts/org/alfresco/share/imports/share-header.lib.js">

var header = widgetUtils.findObject(model.jsonModel.widgets, "id", "HEADER_TITLE_BAR");
var titleBar = widgetUtils.findObject(model.jsonModel.widgets, "id", "HEADER_TITLE_MENU");
var navMenu = widgetUtils.findObject(model.jsonModel.widgets, "id", "HEADER_NAVIGATION_MENU_BAR");

/*
if (titleBar) {
    widgetUtils.deleteObjectFromArray(header.config.widgets, "id", "HEADER_TITLE_MENU");
    delete titleBar;
}
*/

if (navMenu){
    logger.log('--- Found Nav menu ---');
    navMenu.config.widgets = getSubNavigationWidgets();

}