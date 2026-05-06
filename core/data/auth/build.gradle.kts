plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.kubsu.borshchevyk.core.data.auth"
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
    implementation(project(":core:domain:auth"))
    implementation(project(":core:network:auth"))
    implementation(project(":core:network:client"))
    implementation(project(":core:network:mesh"))
    implementation(project(":core:network:websocket"))
    implementation(project(":core:security"))
    implementation(project(":core:database"))
    
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}