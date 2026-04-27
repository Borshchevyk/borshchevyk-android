plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ru.kubsu.borshchevyk.core.domain.call"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
}