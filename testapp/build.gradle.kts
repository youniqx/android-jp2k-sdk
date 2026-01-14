
plugins {
    alias(libs.plugins.android.library)
}

android {
    compileSdk = 36
    namespace = "com.youniqx.jp2k.test"

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
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
