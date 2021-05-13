/*
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
/**
 * Document List Component: Create New Node - create copy of node template in the Data Dictionary
 */
function main() {
    // get the arguments - expecting the "sourceNodeRef" and "parentNodeRef" of the source node to copy
    // and the parent node to contain the new copy of the source.
    var sourceNodeRef = json.get("sourceNodeRef");
    if (sourceNodeRef == null || sourceNodeRef.length === 0) {
        status.setCode(status.STATUS_BAD_REQUEST, "Mandatory 'sourceNodeRef' parameter missing.");
        return;
    }
    var parentNodeRef = json.get("parentNodeRef");
    if (parentNodeRef == null || parentNodeRef.length === 0) {
        status.setCode(status.STATUS_BAD_REQUEST, "Mandatory 'parentNodeRef' parameter missing.");
        return;
    }

    // get the nodes and perform the copy - permission failures etc. will produce a status code response
    var sourceNode = search.findNode(sourceNodeRef),
        parentNode = search.findNode(parentNodeRef);
    if (sourceNode == null || parentNode == null) {
        status.setCode(status.STATUS_NOT_FOUND, "Source or destination node is missing for copy operation.");
    }

    var docCopy = sourceNode.copy(parentNode);
    try {
        //Change the author of the
        docCopy.properties["cm:author"] = person.properties["cm:firstName"] + " " + person.properties["cm:lastName"];
        docCopy.save();

        model.result = docCopy;
    }
    catch(err){
        logger.error("\n\n--------- Error ---------\n There was an error copying the document:\n"+ err);
    }
}

main();