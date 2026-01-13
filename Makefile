export PROJECT_NAME = jp2k-lib
export GROUP_NAME = check-at
export ANDROID_TOOLS_SOURCE = ${HOME}/Projects/youniqx/mia/android-tools
export GITLAB_PKG_REGISTRY = https://gitlab.ci.youniqx.com/api/v4/projects/${CI_PROJECT_ID}/packages/maven
export OPEN_JPEG_VERSION=2.4.0

default: build

.PHONY: build
build:
	./scripts/build.sh --build

.PHONY: prepare
prepare:
	./scripts/build.sh --prepare

#.PHONY: publish
#publish:
#	./scripts/build.sh --publish

.PHONY: publish-local
publish-local:
	./scripts/build.sh --publishLocal

#.PHONY: detekt-and-lint
#detekt-and-lint:
#	./gradlew lintBuildscripts lintKotlin detektDebugAll; \
#	if [ $$? != 0 ]; then \
#		./gradlew formatBuildscripts formatKotlin; \
#    fi

.PHONY: clean
clean:
	./scripts/build.sh --cleanup
