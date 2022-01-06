#######################
# 1. Build Node Project
FROM ${docker.node.image}:${docker.node.version} AS builder

ENV GITHUB_ACA="https://github.com/Alfresco/alfresco-content-app"
ENV SRC_ACA="/usr/src/alfresco-content-app-${aca.version}"

# Récupération des sources
RUN curl -sSL "${GITHUB_ACA}/archive/v${aca.version}.zip" \
     -o "/tmp/alfresco-content-app-${aca.version}.zip" && \
  unzip "/tmp/alfresco-content-app-${aca.version}.zip" -d /usr/src && \
  rm /tmp/alfresco-content-app-${aca.version}.zip

COPY extensions/* /usr/src/
COPY app.config.json ${SRC_ACA}/src/app.config.json

WORKDIR ${SRC_ACA}
RUN npm install ajv
RUN npm install
RUN npm run ng add /usr/src/collabora-aca-extension-${project.version}-dist.tgz

RUN sed -i '/"build"/ s/ -- --prod//'  ${SRC_ACA}/package.json \
    && sed -i 's/--max-old-space-size=8192/--max-old-space-size=2048/' package.json \
    && sed -i '/references/ s/^.*$/"$references": ["aos.plugin.json", "app.header.json", "collabora-online.plugin.json"],/' ${SRC_ACA}/src/assets/app.extensions.json \
    && sed -i -e '/@angular/ a import { CollaboraOnlineModule } from '"'@jeci/collabora-online-extension'"\; -e '/imports:/ s/imports:.*/imports: [AosExtensionModule,CollaboraOnlineModule]/' ${SRC_ACA}/src/app/extensions.module.ts

# Ajout du viewer collabora
RUN sed -i -e '/import { NgModule } /i import { ViewerCollaboraModule } from '"'@jeci/collabora-online-extension'"\; \
       -e '/imports:/ s/imports:.*/imports: [\n \t ViewerCollaboraModule,/' \
       ${SRC_ACA}/src/app/components/viewer/viewer.module.ts \
    && sed -i '/adf-viewer-open-with>/a \\n \t <!-- Viewer Collabora --> \n \t <adf-viewer-extension [supportedExtensions]="supportedExtensions" #extension> \n \t \t <ng-template let-urlFileContent="urlFileContent"> \n \t \t \t<viewer-collabora-online urlFileContent="urlFileContent" [nodeId]="nodeId"></viewer-collabora-online> \n \t \t </ng-template> \n \t </adf-viewer-extension>' \
       ${SRC_ACA}/src/app/components/viewer/viewer.component.html \
    && sed -i -e '/import { ContentApiService } / i import { CollaboraOnlineService } from '"'@jeci/collabora-online-extension'"\; \
         -e '/OnDestroy / a \\n \tsupportedExtensions: string[] = [];'\; \
         -e '/^  constructor/ a \    private collaboraOnlineService : CollaboraOnlineService,' \
         -e '/ngOnInit() / a \    this.supportedExtensions = this.collaboraOnlineService.getExtensions();' \
       ${SRC_ACA}/src/app/components/viewer/viewer.component.ts

RUN npm run build

#######################
# 2. Build nginx image
FROM ${docker.nginx.image}:${docker.nginx.version}
LABEL version="${aca.version}"
LABEL maintainer="Jeci <info@jeci.fr>"
ENV SRC_ACA="/usr/src/alfresco-content-app-${aca.version}"

ARG GROUPNAME=Alfresco
ARG GROUPID=1000
ARG USERNAME=aca
ARG USERID=33009

WORKDIR /usr/share/nginx/html
COPY --from=builder ${SRC_ACA}/dist/app/ .
COPY --from=builder ${SRC_ACA}/docker/entrypoint.sh /
COPY nginx.conf cors_support.conf proxy_pass.conf /etc/nginx/

RUN addgroup -g ${GROUPID} ${GROUPNAME} && \
    adduser -S -u ${USERID} -G ${GROUPNAME} -s "/bin/bash" ${USERNAME} && \
    chown -R ${USERNAME}:${GROUPNAME} /usr/share/nginx/html && \
    chown -R ${USERNAME}:${GROUPNAME} /var/cache/nginx && \
    touch /var/run/nginx.pid && \
    chown -R ${USERNAME}:${GROUPNAME} /var/run/nginx.pid && \
    chmod +x /entrypoint.sh && \
    chown -R ${USERNAME}:${GROUPNAME} /entrypoint.sh

EXPOSE 8080
USER ${USERNAME}
ENTRYPOINT [ "sh", "/entrypoint.sh" ]
