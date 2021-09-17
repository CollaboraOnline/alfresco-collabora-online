FROM ${docker.acs.image}:${alfresco.platform.version}

ARG TOMCAT_DIR=/usr/local/tomcat

USER root

COPY extensions/*.amp /root/amp/

RUN java -jar /root/alfresco-mmt.jar install /root/amp/ ${TOMCAT_DIR}/webapps/alfresco -nobackup -directory

COPY alfresco-global.properties $TOMCAT_DIR/shared/classes/alfresco-global.properties

COPY dev-log4j.properties $TOMCAT_DIR/shared/classes/alfresco/extension

USER ${USERNAME}
