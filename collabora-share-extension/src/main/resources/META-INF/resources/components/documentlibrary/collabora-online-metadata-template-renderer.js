(function() {
	YAHOO.Bubbling
			.fire(
					"registerRenderer",
					{
						propertyName : "editingBanner",
						renderer : function collaboraEditing_renderer(record, label) {
							var jsNode = record.jsNode, properties = jsNode.properties, html = "";
							var editors = properties["collabora:editors"] || "";
							var txt = this.msg("details.banner.collabora-editing");
							return '<span>' + label + '</span><span>' + txt
									+ ' ' + editors + '</span>';
						}
					});
})();
