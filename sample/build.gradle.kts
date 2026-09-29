plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.jalloft.promo.sample"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jalloft.promo.sample"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        val local = if (project.hasProperty("dev")) "http://10.0.2.2:3000/api/apps" else ""
        buildConfigField("String", "LOCAL_ENDPOINT", "\"$local\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":promo"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
}
