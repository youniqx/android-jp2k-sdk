
plugins {
    alias(libs.plugins.android.application)
}

android {
    compileSdk = 35
    namespace = "com.youniqx.jp2.test"

    defaultConfig {
        applicationId = "com.youniqx.openjpeg.test"
        targetSdk = 35
        minSdk = 21
        versionCode = 1
        versionName = "0.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

}

dependencies {
    implementation(project(":library"))
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
}
