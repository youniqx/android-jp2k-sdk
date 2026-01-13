
plugins {
    alias(libs.plugins.android.application)
}

android {
    compileSdk = 36
    namespace = "com.youniqx.jp2.test"

    defaultConfig {
        applicationId = "com.youniqx.openjpeg.test"
        targetSdk = 36
        minSdk = 26
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
