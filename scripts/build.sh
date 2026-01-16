#!/bin/bash

set -o errexit
set -o pipefail

CURRENT_WORK_DIR="${PWD}"
build=false
publish=false
publishLocal=false
cleanUp=false
prepare=false

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
	    ./gradlew lintKotlin detektDebugAll
    fi
    ./gradlew assemble
    mkdir -p "${CURRENT_WORK_DIR}/tmp"
    ./gradlew assembleDebugAndroidTest
    cp "${CURRENT_WORK_DIR}/library/build/outputs/apk/androidTest/debug/library-debug-androidTest.apk" "${CURRENT_WORK_DIR}/tmp/test.apk"
    ./gradlew assembleDebugAndroidTest -DtestApplicationId="com.youniqx.jp2k.test.app"
    cp "${CURRENT_WORK_DIR}/library/build/outputs/apk/androidTest/debug/library-debug-androidTest.apk" "${CURRENT_WORK_DIR}/tmp/app.apk"
  fi
  if [ $publish = true ]; then
    if [ -z "${CI_COMMIT_TAG}" ];then
      echo "Snapshot publishing. Use PipeNumber as version -> ${CI_PIPELINE_ID}-snapshot"
      export LIB_VERSION="${CI_PIPELINE_ID}-snapshot"
    else
      echo "Publish version ${CI_COMMIT_TAG#v}"
      export LIB_VERSION="${CI_COMMIT_TAG#v}"
    fi
    ./gradlew publish "${ADDITIONAL_GRADLE_OPTION}"
  fi
  if [ $publishLocal = true ]; then
    export LIB_VERSION="local"
    ./gradlew publishToMavenLocal
  fi
}

# Cloning openjpeg C library from [openjpeg](https://github.com/uclouvain/openjpeg)
cloneFromOpenJpeg() {
  echo "--------- CLONE OPEN JPEG ${OPEN_JPEG_VERSION} ---------"
  OPEN_JPEG_DIR="${CURRENT_WORK_DIR}/library/src/main/cpp/openjpeg"
  if [ -d "${OPEN_JPEG_DIR}" ];then
    cd "${OPEN_JPEG_DIR}" || exit 1
    git checkout v"${OPEN_JPEG_VERSION}"
    git pull
  else
    git clone --branch v"${OPEN_JPEG_VERSION}" --depth=1 "https://github.com/uclouvain/openjpeg.git" "${OPEN_JPEG_DIR}"
  fi
  cd "${CURRENT_WORK_DIR}" || exit 1
  echo "--------- DONE CLONE OPEN JPEG ---------"
}

cleanupProject() {
  echo "--------- CLEANUP PROJECT ---------"
  ./gradlew clean
   rm -rf "${CURRENT_WORK_DIR}/library/src/main/cpp/openjpeg"
   rm -rf "${CURRENT_WORK_DIR}/tmp"
  echo "--------- CLEANUP DONE ---------"
}

main() {
  ## cleanup and exit
  if [ "$cleanUp" = true ];then
    cleanupProject
    exit 0;
  fi

  ## get external resources
  cloneFromOpenJpeg

  ## no need to build if prepare action
  if [ $prepare = true ];then
    exit 0;
  fi

  ## build, publish,…
  runAction
}

main
