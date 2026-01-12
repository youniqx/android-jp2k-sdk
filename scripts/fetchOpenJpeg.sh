#!/bin/bash

set -o errexit
set -o pipefail

CURRENT_WORK_DIR="${PWD}"
OPEN_JPEG_DIR="${CURRENT_WORK_DIR}/library/src/main/cpp/openjpeg"
# Cloning openjpeg C library from [openjpeg](https://github.com/uclouvain/openjpeg)
cloneFromOpenJpeg() {
  if [ -d "${OPEN_JPEG_DIR}" ];then
    cd "${OPEN_JPEG_DIR}" || exit 1
    git checkout v"${OPEN_JPEG_VERSION}"
    git pull
  else
    git clone --branch v"${OPEN_JPEG_VERSION}" --depth=1 "https://github.com/uclouvain/openjpeg.git" "${OPEN_JPEG_DIR}"
  fi
}

cloneFromOpenJpeg
