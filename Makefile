export GITLAB_PKG_REGISTRY = https://gitlab.ci.youniqx.com/api/v4/projects/${CI_PROJECT_ID}/packages/maven
export OPEN_JPEG_VERSION=2.4.0

default: build

.PHONY: build
build:
	./scripts/build.sh --build

.PHONY: prepare
prepare:
	./scripts/build.sh --prepare

.PHONY: uitest
uitest:
	mkdir "${RESULT_DIR}" \
 	&& uitest --key-file-env "UI_TEST_SERVICE_ACCOUNT" \
     --app-apk "${APP_APK}" \
     --test-apk "${TEST_APK}" \
     --type "${TEST_TYPE}" \
     --timeout "${TEST_TIMEOUT}" \
     --use-orchestrator \
     --environment-variables "clearPackageData=true" \
     --output-dir "${RESULT_DIR}" \
     --project "${FIREBASE_PROJECT}" \
     --device locale=de,orientation=portrait

.PHONY: publish
publish:
	./scripts/build.sh --publish

.PHONY: publish-local
publish-local:
	./scripts/build.sh --publishLocal

.PHONY: detekt-and-lint
detekt-and-lint:
	./gradlew lintKotlin detektDebugAll; \
	if [ $$? != 0 ]; then \
		./gradlew formatKotlin; \
    fi

.PHONY: clean
clean:
	./scripts/build.sh --cleanup
