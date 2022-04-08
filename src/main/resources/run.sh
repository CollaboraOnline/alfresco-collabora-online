#!/bin/sh



start_share() {
    docker-compose up --build -d ${share.host}
}

start_acs() {
    docker-compose up --build -d ${acs.host}
}


build_share() {
    docker-compose kill ${share.host}
    yes | docker-compose rm -f ${share.host}
    $MVN_EXEC clean package -pl collabora-share-extension,collabora-share-extension-docker
}

build_acs() {
    docker-compose kill ${acs.host}
    yes | docker-compose rm -f ${acs.host}
    $MVN_EXEC clean package -pl collabora-platform-extension,collabora-platform-extension-docker
}

prepare_test() {
    $MVN_EXEC verify -DskipTests=true -pl collabora-platform-extension,alfresco-collabora-online-integration-tests,collabora-platform-extension-docker
}

test() {
    $MVN_EXEC verify -pl collabora-platform-extension,alfresco-collabora-online-integration-tests
}
