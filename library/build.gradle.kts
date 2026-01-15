import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlinter)
    id("maven-publish")
}

tasks.withType<Detekt>().configureEach {
    exclude { it.file.path.contains("build") }
    exclude { it.file.path.contains("cpp") }
}

tasks.register("detektDebugAll") {
    dependsOn("detektDebug", "detektDebugUnitTest", "detektDebugAndroidTest")
}

android {
    compileSdk = 36
    namespace = "com.youniqx.jp2k"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testApplicationId = System.getProperty("testApplicationId", "com.youniqx.jp2k.test")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    publishing {
        multipleVariants("all") {
            allVariants()
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.android.annotation)
    implementation(libs.core.ktx)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    testImplementation(libs.kotlin.test)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            groupId = "com.youniqx"
            artifactId = "jp2k-sdk"
            version = System.getenv("LIB_VERSION") ?: "local"
            afterEvaluate {
                from(components["all"])
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
