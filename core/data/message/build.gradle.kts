plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.kubsu.borshchevyk.core.data.message"
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
    implementation(project(":core:domain:message"))
    implementation(project(":core:data:chat"))
    implementation(project(":core:network:client"))
    implementation(project(":core:network:mesh"))
    implementation(project(":core:network:message"))
    implementation(project(":core:network:media"))
    implementation(project(":core:network:websocket"))
    
    implementation(project(":core:database"))
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
