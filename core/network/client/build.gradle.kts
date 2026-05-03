plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.kubsu.borshchevyk.core.network.client"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"https://borshchevik.su/\"")
            buildConfigField("long", "TIMEOUT_MILLIS", "60000L")
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://borshchevik.su/\"")
            buildConfigField("long", "TIMEOUT_MILLIS", "60000L")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.play.services.nearby)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
