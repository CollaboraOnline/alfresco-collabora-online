#!/bin/sh



start_share() {
    docker-compose up --build -d share
}

start_acs() {
    docker-compose up --build -d alfresco
}


build_share() {
    docker-compose kill share
    yes | docker-compose rm -f share
    $MVN_EXEC clean package -pl collabora-share-extension,collabora-share-extension-docker
}

build_acs() {
    docker-compose kill alfresco
    yes | docker-compose rm -f alfresco
    $MVN_EXEC clean package -pl collabora-platform-extension,collabora-platform-extension-docker
}

prepare_test() {
    $MVN_EXEC verify -DskipTests=true -pl collabora-platform-extension,alfresco-collabora-online-integration-tests,collabora-platform-extension-docker
}

test() {
    $MVN_EXEC verify -pl collabora-platform-extension,alfresco-collabora-online-integration-tests
}
