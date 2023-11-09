FROM ${docker.share.image}:${docker.share.version}

ARG TOMCAT_DIR=/usr/local/tomcat

COPY context.xml $TOMCAT_DIR/conf

# Copy Dockerfile to avoid an error if no JARs exist
COPY Dockerfile extensions/*.jar $TOMCAT_DIR/webapps/share/WEB-INF/lib/

# Copy Dockerfile to avoid an error if no AMPs exist
COPY Dockerfile extensions/*.amp $TOMCAT_DIR/amps_share/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
              $TOMCAT_DIR/amps_share $TOMCAT_DIR/webapps/share -directory -nobackup -force

COPY log4j.properties $TOMCAT_DIR/webapps/share/WEB-INF/classes
COPY hotswap-agent.properties $TOMCAT_DIR/webapps/share/WEB-INF/classes

# Fix https://github.com/Alfresco/acs-community-packaging/issues/367
RUN sed -i '/<show-authorization-status>/ s/true/false/'  ${TOMCAT_DIR}/shared/classes/alfresco/web-extension/share-config-custom.xml
