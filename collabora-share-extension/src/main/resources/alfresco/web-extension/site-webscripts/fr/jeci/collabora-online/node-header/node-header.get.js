<import resource="classpath:/alfresco/templates/org/alfresco/import/alfresco-util.js">

// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

function main() {
	var nodeRef = url.args.nodeRef,
		connector = remote.connect("alfresco");
	model.editing=false;
	var uuid = nodeRef.substring(nodeRef.lastIndexOf('/') + 1);
	
	var result = connector.get('/slingshot/doclib/aspects/node/workspace/SpacesStore/' + uuid );
	if (result.status.code == status.STATUS_OK) {
		var json = JSON.parse(result.response);
		
		for (index = 0; index < json.current.length; ++index) {
			if (json.current[index] == "collabora:collaboraOnline" ) {
				model.editing=true;
			}
		}
		
	}
};

main();