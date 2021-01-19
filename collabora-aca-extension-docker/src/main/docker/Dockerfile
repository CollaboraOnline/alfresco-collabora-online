#######################
# 1. Build Node Project
FROM ${docker.node.image}:${docker.node.version} AS builder

ENV GITHUB_ACA="https://github.com/Alfresco/alfresco-content-app"
ENV SRC_ACA="/usr/src/alfresco-content-app-${aca.version}"

# Récupération des sources
RUN curl -sSL "${GITHUB_ACA}/archive/v${aca.version}.zip" \
     -o "alfresco-content-app-${aca.version}.zip" && \
  unzip "alfresco-content-app-${aca.version}.zip" -d /usr/src && \
  rm alfresco-content-app-${aca.version}.zip

WORKDIR ${SRC_ACA}
RUN npm install ajv

COPY extensions/* /usr/src/
RUN npm install
RUN npm run ng add /usr/src/collabora-aca-extension-0.3.0-SNAPSHOT-dist.tgz

RUN sed -i '/references/ s/^.*$/"$references": ["aos.plugin.json", "app.header.json", "collabora-online.plugin.json"],/' ${SRC_ACA}/src/assets/app.extensions.json && \
    sed -i '/"build"/ s/ -- --prod//' ${SRC_ACA}/package.json && \
    sed -i -e '/@angular/ a\
import { CollaboraOnlineModule } from '"'@jeci/collabora-online-extension'"\; \
-e '/imports:/ s/imports:.*/imports: [AosExtensionModule,CollaboraOnlineModule]/' ${SRC_ACA}/src/app/extensions.module.ts

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
