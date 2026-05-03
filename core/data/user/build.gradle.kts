plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.kubsu.borshchevyk.core.data.user"
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
    implementation(project(":core:domain:user"))
    implementation(project(":core:network:user"))
    implementation(project(":core:network:client"))
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(project(":core:database"))
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
