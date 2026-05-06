plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "ru.kubsu.borshchevyk.feature.chat"
    compileSdk = 36
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:ui"))
    implementation(project(":core:model"))
    implementation(project(":core:domain:auth"))
    implementation(project(":core:domain:chat"))
    implementation(project(":core:domain:message"))
    implementation(project(":core:domain:user"))
    implementation(project(":core:domain:call"))
    implementation(project(":core:network:client"))
    implementation(project(":core:network:mesh"))
    
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.coil.video)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
