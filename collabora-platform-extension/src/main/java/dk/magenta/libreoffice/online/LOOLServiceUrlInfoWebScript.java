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
package dk.magenta.libreoffice.online;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class LOOLServiceUrlInfoWebScript extends DeclarativeWebScript {
    private static final Logger logger = LoggerFactory.getLogger(LOOLServiceUrlInfoWebScript.class);

    private static final String LOOL_HOST_URL = "lool_host_url";

    private String loolServiceUrl;

    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<>();
        if (logger.isDebugEnabled()) {
            logger.debug("The service url for WOPI is:" + loolServiceUrl);
        }
        model.put(LOOL_HOST_URL, loolServiceUrl);

        return model;
    }

    public void setLoolServiceUrl(String loolServiceUrl) {
        this.loolServiceUrl = loolServiceUrl;
    }
}