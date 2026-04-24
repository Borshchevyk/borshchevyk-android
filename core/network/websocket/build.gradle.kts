plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.kubsu.borshchevyk.core.network.websocket"
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
    implementation(project(":core:network:client"))
    implementation(libs.ktor.client.core)
    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.ktor.client.websockets)
    implementation(libs.krossbow.stomp.core)
    implementation(libs.krossbow.websocket.ktor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
