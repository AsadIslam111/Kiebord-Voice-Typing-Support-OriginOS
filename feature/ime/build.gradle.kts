plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.kiebord.feature.ime"
    compileSdk = 35
    ndkVersion = "26.2.11394342"

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    sourceSets {
        named("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }
}

// Automated cargo-ndk bridge task to build Rust core into jniLibs
val buildRustCore = tasks.register<Exec>("buildRustCore") {
    val rustProjectDir = file("${rootDir}/native/kiebord-engine")
    val jniLibsDir = file("${projectDir}/src/main/jniLibs")
    val cargoExecutable = run {
        val userHome = System.getProperty("user.home")
        val winCargo = file("$userHome/.cargo/bin/cargo.exe")
        val unixCargo = file("$userHome/.cargo/bin/cargo")
        when {
            winCargo.exists() -> winCargo.absolutePath
            unixCargo.exists() -> unixCargo.absolutePath
            else -> "cargo"
        }
    }

    workingDir = rustProjectDir

    // Use cargo-ndk to compile for 64-bit and 32-bit ARM & x86_64
    commandLine(
        cargoExecutable, "ndk",
        "-t", "arm64-v8a",
        "-t", "armeabi-v7a",
        "-t", "x86_64",
        "-o", jniLibsDir.absolutePath,
        "build", "--release"
    )

    inputs.dir(rustProjectDir.resolve("src"))
    inputs.file(rustProjectDir.resolve("Cargo.toml"))
    outputs.dir(jniLibsDir)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }.configureEach {
    dependsOn(buildRustCore)
}

// Compile .combined dictionaries into binary .dict assets
val compileDictionaries = tasks.register<Exec>("compileDictionaries") {
    val rustProjectDir = file("${rootDir}/native/kiebord-engine")
    val cargoExecutable = run {
        val userHome = System.getProperty("user.home")
        val winCargo = file("$userHome/.cargo/bin/cargo.exe")
        val unixCargo = file("$userHome/.cargo/bin/cargo")
        when {
            winCargo.exists() -> winCargo.absolutePath
            unixCargo.exists() -> unixCargo.absolutePath
            else -> "cargo"
        }
    }

    workingDir = rustProjectDir
    commandLine(cargoExecutable, "run", "--bin", "compile_dict")

    inputs.dir(file("${rootDir}/assets-src/dictionaries"))
    outputs.dir(file("${projectDir}/src/main/assets/dictionaries"))
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(compileDictionaries)
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
    implementation(project(":core:theme"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.emoji2.emojipicker)
    implementation(libs.androidx.emoji2)

    testImplementation(libs.junit)
}
