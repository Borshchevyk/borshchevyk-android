plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.kubsu.borshchevyk.core.network.mesh"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:network:client"))
    
    implementation(libs.play.services.nearby)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
