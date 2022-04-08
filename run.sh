#!/bin/sh

export COMPOSE_FILE="${PWD}/target/classes/docker-compose.yml"

if [ -z "${M2_HOME}" ]; then
  export MVN_EXEC="mvn"
else
  export MVN_EXEC="${M2_HOME}/bin/mvn"
fi


build() {
    $MVN_EXEC clean package resources:resources -Dmaven.test.skip=true
}

build_resources() {
  if [ ! -f "$COMPOSE_FILE" ]; then
    $MVN_EXEC package -pl collabora-platform-extension-docker,collabora-share-extension-docker,collabora-aca-extension-docker
    $MVN_EXEC resources:resources
  fi
}

clean() {
    $MVN_EXEC clean
}

down() {
  if [ -f "$COMPOSE_FILE" ]; then
        docker-compose down
    fi
}


logs() {
    docker-compose logs $1
}

purge() {
    docker-compose down -v
}


ps() {
    docker-compose ps
}

start() {
    docker-compose up --build -d
}

tail() {
    docker-compose logs -f
}

tail_all() {
    docker-compose logs --tail="all"
}



build_resources
source "${PWD}/target/classes/run.sh"


case "$1" in
  build)
    build
    ;;
  build_start)
    down
    build
    start
    tail
    ;;
  build_start_it_supported)
    down
    build
    prepare_test
    start
    tail
    ;;
  build_test)
    down
    build
    prepare_test
    start
    test
    tail_all
    down
    ;;
  clean)
    clean
    ;;
  logs)
    logs $2
    ;;
  purge)
    down
    purge
    ;;
  ps)
    ps
    ;;
  reload_share)
    build_share
    start_share
    tail
    ;;
  reload_acs)
    build_acs
    start_acs
    tail
    ;;
  start)
    start
    tail
    ;;
  stop)
    down
    ;;
  tail)
    tail
    ;;
  test)
    test
    ;;
  *)
    echo "Usage: $0 {build_start|build_start_it_supported|start|stop|purge|tail|reload_share|reload_acs|build_test|test}"
esac
