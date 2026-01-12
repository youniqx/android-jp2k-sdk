#!/bin/bash

set -o errexit
set -o pipefail

CURRENT_WORK_DIR="${PWD}"
build=false
publish=false
publishLocal=false
docs=false
cleanUp=false
prepare=false

# shellcheck source=/dev/null
. "${CURRENT_WORK_DIR}"/scripts/fetchPrepareBuildEnvironment.sh

## get arguments from script call
while true; do
  if [ "${1}" = "--build" ]; then
    build=true
    shift
  elif [ "${1}" = "--publish" ]; then
    publish=true
    shift
  elif [ "${1}" = "--publishLocal" ]; then
    publishLocal=true
    shift
  elif [ "${1}" = "--docs" ]; then
    docs=true
    shift
  elif [ "${1}" = "--cleanup" ]; then
    cleanUp=true
    shift
  elif [ "${1}" = "--prepare" ]; then
    prepare=true
    shift
  else
    break
  fi
done

runAction() {
  if [ $build = true ]; then
    if [ "$CI" = "true" ]; then
	    ./gradlew lintBuildscripts lintKotlin detektDebugAll
    fi
    ./gradlew assemble test
  fi
  if [ $publish = true ]; then
    if [ -z "${CI_COMMIT_TAG}" ];then
      echo "Snapshot publishing. Use PipeNumber as version -> ${CI_PIPELINE_ID}-snapshot"
      export LIB_VERSION="${CI_PIPELINE_ID}-snapshot"
    else
      echo "Publish version ${CI_COMMIT_TAG#v}"
      export LIB_VERSION="${CI_COMMIT_TAG#v}"
    fi
    ./gradlew publish -Dorg.gradle.s3.endpoint=https://s3.gra.io.cloud.ovh.net
  fi
  if [ $publishLocal = true ]; then
    export LIB_VERSION="local"
    ./gradlew publishToMavenLocal
  fi
  if [ $docs = true ]; then
    ./gradlew dokkaGenerate || exit 1
    mkdir -p public && cp -r wallet-sdk/build/dokka/html/* public
  fi
}

main() {
  ## get build tools
  fetchPrepareBuildEnvironment
  ## prepare build environment
  prepareBuildEnvironment

  ## cleanup and exit
  if [ "$cleanUp" = true ];then
    cleanupProject
    exit 0;
  fi
  ## and some project specific files
  prepareProjectSpecificStuff
  if [ $prepare = true ];then
    exit 0;
  fi
  runAction
}

main
