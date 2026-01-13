plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    compileSdk = 36
    namespace = "com.youniqx.jp2k"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags.clear()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            consumerProguardFile("proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path = File("CMakeLists.txt")
        }
    }

    ndkVersion = "28.2.13676358"

    buildFeatures {
        buildConfig = true
    }

    publishing {
        singleVariant("release") {}
        multipleVariants("all") {
            allVariants()
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.android.annotation)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            groupId = "com.youniqx"
            artifactId = "jp2k-sdk"
            version = System.getenv("LIB_VERSION") ?: "local"
            // only publish JARs when pushing for internal use or local builds
            afterEvaluate {
                if (System.getenv("PUBLIC_ARTIFACTS_URL").isNullOrEmpty()) {
                    from(components["all"])
                } else {
                    from(components["release"])
                }
            }
        }
    }
    // Running in CI
    if (System.getenv("CI") == "true") {
        // Release to Public AWS Artifacts Bucket if variable is set
        if (!System.getenv("PUBLIC_ARTIFACTS_URL").isNullOrEmpty()) {
            repositories {
                maven {
                    url = uri(System.getenv("PUBLIC_ARTIFACTS_URL"))
                    credentials(AwsCredentials::class) {
                        accessKey = requireNotNull(System.getenv("PUBLIC_ARTIFACTS_USER"))
                        secretKey = requireNotNull(System.getenv("PUBLIC_ARTIFACTS_PASSWORD"))
                    }
                }
            }
        } else {
            repositories {
                // Release to Gitlab Package Registry
                maven {
                    url = uri(System.getenv("GITLAB_PKG_REGISTRY"))
                    credentials(PasswordCredentials::class) {
                        username = "gitlab-ci-token"
                        password = System.getenv("CI_JOB_TOKEN")
                        authentication {
                            create<BasicAuthentication>("basic")
                        }
                    }
                }
            }
        }
    }
}
