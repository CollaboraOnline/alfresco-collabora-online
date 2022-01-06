FROM ${docker.acs.image}:${alfresco.platform.version}

ARG TOMCAT_DIR=/usr/local/tomcat

USER root

WORKDIR $TOMCAT_DIR/amps/

# Ajout des modules
#  - support-tools OOTB
RUN set -x \
  && curl -sSLO "https://repo1.maven.org/maven2/org/orderofthebee/support-tools/support-tools-repo/1.1.0.0/support-tools-repo-1.1.0.0-amp.amp" \
  && find ${TOMCAT_DIR}/webapps/alfresco -type d -print0 | xargs -0 chmod 755 \
  && java -jar ${TOMCAT_DIR}/alfresco-mmt/alfresco-mmt-6.0.jar \
      install ${TOMCAT_DIR}/amps/ ${TOMCAT_DIR}/webapps/alfresco \
      -nobackup -directory -force

COPY alfresco-global.properties $TOMCAT_DIR/shared/classes/alfresco-global.properties
COPY dev-log4j.properties $TOMCAT_DIR/shared/classes/alfresco/extension

COPY extensions/*.jar ${TOMCAT_DIR}/webapps/alfresco/WEB-INF/lib/

WORKDIR $TOMCAT_DIR
USER ${USERNAME}
