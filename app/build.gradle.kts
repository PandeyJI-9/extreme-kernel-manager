plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

import org.gradle.api.GradleException
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

abstract class BuildNativeTelemetryTask @Inject constructor(
    private val execOperations: ExecOperations,
) : DefaultTask() {
    @get:InputDirectory
    abstract val crateDir: DirectoryProperty

    @get:Input
    abstract val target: Property<String>

    @get:Input
    abstract val cargoExecutable: Property<String>

    @get:Input
    @get:Optional
    abstract val ndkHome: Property<String>

    init {
        group = "build"
        description = "Builds the optional Rust JNI telemetry library. Requires Android NDK and a Rust Android target."
    }

    @TaskAction
    fun build() {
        if (!ndkHome.isPresent) {
            throw GradleException("Native telemetry build requires ANDROID_NDK_HOME or ANDROID_NDK_ROOT to point at an installed Android NDK.")
        }

        execOperations.exec {
            workingDir = crateDir.get().asFile
            environment("ANDROID_NDK_HOME", ndkHome.get())
            commandLine(cargoExecutable.get(), "build", "--release", "--target", target.get())
        }
    }
}

configure <com.android.build.api.dsl.ApplicationExtension> {
    namespace = "id.nkz.nokontzzzmanager"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.nkz.nokontzzzmanager"
        minSdk = 31
        targetSdk = 36
        versionCode = 122
        versionName = "2.0.0-beta"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        disable.add("NullSafeMutableLiveData")
    }
    buildFeatures { compose = true }
}

val nativeTelemetryTarget = providers.gradleProperty("nkmTelemetryTarget")
    .orElse("aarch64-linux-android")
val nativeTelemetryAbi = providers.gradleProperty("nkmTelemetryAbi")
    .orElse("arm64-v8a")
val nativeTelemetryCrateDir = rootProject.layout.projectDirectory.dir("native/telemetry")
val nativeTelemetryOutputDir = layout.buildDirectory.dir("generated/jniLibs/${nativeTelemetryAbi.get()}")

tasks.register<BuildNativeTelemetryTask>("buildNativeTelemetry") {
    val ndkHomeProvider = providers.environmentVariable("ANDROID_NDK_HOME")
        .orElse(providers.environmentVariable("ANDROID_NDK_ROOT"))
    val cargo = providers.environmentVariable("CARGO").orElse("cargo")

    crateDir.set(nativeTelemetryCrateDir)
    target.set(nativeTelemetryTarget)
    cargoExecutable.set(cargo)
    ndkHome.set(ndkHomeProvider)
}

tasks.register<Copy>("copyNativeTelemetry") {
    group = "build"
    description = "Copies libnkm_telemetry.so into the app JNI libs staging directory."
    dependsOn("buildNativeTelemetry")

    from(nativeTelemetryCrateDir.file("target/${nativeTelemetryTarget.get()}/release/libnkm_telemetry.so"))
    into(nativeTelemetryOutputDir)
}

configurations.all {
    resolutionStrategy {
        force(libs.guava)
        force(libs.listenablefuture)
    }
    exclude(group = "com.google.guava", module = "listenablefuture")
}

kotlin {
    compilerOptions { 
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) 
    }
}

dependencies {
    // Core & App
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation & Lifecycle
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.hilt.android.testing)

    // Data
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    // Background Tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Utility
    implementation(libs.libsu)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.guava) {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
    implementation(libs.listenablefuture)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
}
