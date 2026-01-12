#!/bin/bash

set -o errexit
set -o pipefail

fetchPrepareBuildEnvironment() {
  ## get Android Tools for local and debug builds
  ## check if we already have the android tools repo cloned
  if [ ! -d "$ANDROID_TOOLS_SOURCE" ]; then
    mkdir -p "${HOME}/Projects/youniqx/mia"
    if [ "$CI" == "true" ];then
      git clone "https://gitlab-ci-token:${CI_JOB_TOKEN}@gitlab.ci.youniqx.com/youniqx/mia/android-tools.git" "$ANDROID_TOOLS_SOURCE" || exit 1
    else
      git clone https://gitlab.ci.youniqx.com/youniqx/mia/android-tools.git "$ANDROID_TOOLS_SOURCE" || exit 1
    fi
  elif [ "$CI" != "true" ]; then
    cd "$ANDROID_TOOLS_SOURCE" || exit 1
    localBranch="$(git rev-parse --abbrev-ref HEAD)"
    localBranchInRemote="$(git ls-remote --head origin "${localBranch}")"
    ## check if the local branch exits in remote…
    if [[ -n "${localBranchInRemote}" ]];then
      ## …and if the work dir is clean
      if [[ -z $(git diff --exit-code) ]];then
        echo "Android Tools working dir clean, pulling…"
        git pull
      else
        ## local branch exists, but…
        echo "Android Tools has uncommited changes. Will not pull from repo and use current working copy instead."
      fi
    else
      echo "Android Tools remote branch does not exist. Will use current working copy instead."
    fi
    cd "$CURRENT_WORK_DIR" || exit 1
  fi

  cp -r "$ANDROID_TOOLS_SOURCE/buildScripts" "$CURRENT_WORK_DIR/"

  # shellcheck source=/dev/null
  . "${CURRENT_WORK_DIR}"/buildScripts/prepareBuildEnvironment.sh
}
