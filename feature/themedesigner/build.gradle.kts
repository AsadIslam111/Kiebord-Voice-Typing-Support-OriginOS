plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/*
 * Theme Designer (Theme Studio) — optional feature module.
 *
 * This module is intentionally self-contained: the host app never references its classes
 * directly. It is discovered at runtime through `io.kiebord.theme.designer.ThemeDesignerRegistry`.
 *
 * To ship a "lite" build without the designer, depend on this module only from the
 * full/regular flavor, e.g. in app/build.gradle.kts:
 *
 *     "regularImplementation"(project(":feature:themedesigner"))
 *
 * Nothing else needs to change: the "Create" entry point hides itself when the module is absent.
 */
android {
    namespace = "io.kiebord.feature.themedesigner"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:theme"))
    implementation(project(":feature:ime"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    testImplementation(libs.junit)
}
