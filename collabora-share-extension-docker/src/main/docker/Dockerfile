FROM ${docker.share.image}:${alfresco.share.version}

ARG TOMCAT_DIR=/usr/local/tomcat

WORKDIR $TOMCAT_DIR/amps_share/

# Ajout des modules
#  - support-tools OOTB
RUN set -x \
 && curl -sSLO "https://repo1.maven.org/maven2/org/orderofthebee/support-tools/support-tools-share/1.1.0.0/support-tools-share-1.1.0.0-amp.amp" \
 && find ${TOMCAT_DIR}/webapps/share -type d -print0 | xargs -0 chmod 755 \
 && java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt-6.0.jar \
    install ${TOMCAT_DIR}/amps_share/ ${TOMCAT_DIR}/webapps/share \
    -nobackup -directory -force

COPY share-config-custom.xml $TOMCAT_DIR/shared/classes/alfresco/web-extension
COPY log4j.properties $TOMCAT_DIR/webapps/share/WEB-INF/classes

COPY extensions/*.jar $TOMCAT_DIR/webapps/share/WEB-INF/lib/

WORKDIR $TOMCAT_DIR
