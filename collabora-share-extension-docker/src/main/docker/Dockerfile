FROM ${docker.share.image}:${alfresco.share.version}

ARG TOMCAT_DIR=/usr/local/tomcat

COPY extensions/*.amp /root/amp/

RUN java -jar /root/alfresco-mmt.jar install /root/amp/ ${TOMCAT_DIR}/webapps/share -nobackup -directory

COPY share-config-custom.xml $TOMCAT_DIR/shared/classes/alfresco/web-extension

COPY log4j.properties $TOMCAT_DIR/webapps/share/WEB-INF/classes
