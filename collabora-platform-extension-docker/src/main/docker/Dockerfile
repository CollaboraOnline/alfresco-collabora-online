FROM ${docker.acs.image}:${docker.acs.version}

ARG TOMCAT_DIR=/usr/local/tomcat
ARG USERNAME=${docker.acs.user}

USER root

WORKDIR ${TOMCAT_DIR}/keystore

COPY keystore/keystore ${TOMCAT_DIR}/keystore/keystore
COPY keystore/ssl.keystore ${TOMCAT_DIR}/keystore/ssl.keystore
COPY keystore/ssl.truststore ${TOMCAT_DIR}/keystore/ssl.truststore

RUN chown -R ${USERNAME}:${USERNAME} ${TOMCAT_DIR}/keystore

# Copy Dockerfile to avoid an error if no JARs exist
COPY Dockerfile extensions/*.jar $TOMCAT_DIR/webapps/alfresco/WEB-INF/lib/

# Copy Dockerfile to avoid an error if no AMPs exist
COPY Dockerfile extensions/*.amp $TOMCAT_DIR/amps/
RUN java -jar $TOMCAT_DIR/alfresco-mmt/alfresco-mmt*.jar install \
              $TOMCAT_DIR/amps $TOMCAT_DIR/webapps/alfresco -directory -nobackup -force

COPY alfresco-global.properties $TOMCAT_DIR/shared/classes/alfresco-global.properties
COPY dev-log4j.properties $TOMCAT_DIR/shared/classes/alfresco/extension
COPY disable-webscript-caching-context.xml $TOMCAT_DIR/shared/classes/alfresco/extension

RUN mkdir -p /var/log/alfresco && chown -R ${USERNAME}:${USERNAME} /var/log/alfresco

USER ${USERNAME}

COPY alfresco-keystore.jar ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/lib/
COPY keystore-context.xml ${TOMCAT_DIR}/shared/classes/alfresco/extension/

WORKDIR ${TOMCAT_DIR}
