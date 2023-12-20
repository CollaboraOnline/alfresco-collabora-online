(function() {
	YAHOO.Bubbling
			.fire(
					"registerRenderer",
					{
						propertyName : "editingBanner",
						renderer : function collaboraEditing_renderer(record, label) {
							var jsNode = record.jsNode, properties = jsNode.properties, html = "";
							var editors = properties["cm:lockOwner"] || "";
							var txt = this.msg("details.banner.collabora-editing");
							return '<span>' + label + '</span><span>' + txt
									+ ' ' + editors.displayName + '</span>';
						}
					});
})();
