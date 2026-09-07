import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("HazloSanoDatabase") {
            packageName.set("com.hazlosano.data.db")
        }
    }
}

kotlin {
    android {
        namespace = "com.hazlosano.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm()

    js {
        browser()
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xir-incremental-disable")
                }
            }
        }
    }

    // wasmJs omitted: SQLDelight runtime has no wasmJs artifact

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            // El gesto de volver atras del sistema, en codigo comun: Compose Multiplatform
            // lo trae desde 1.11 y no hace falta expect/actual. En los targets sin gesto
            // propio no hace nada.
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(compose.materialIconsExtended)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.play.services.location)
            implementation(libs.androidx.work.ktx)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.uiTooling)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.maplibre.android.sdk)
            implementation(libs.maplibre.annotation)
            implementation(libs.maplibre.geojson)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.jdbc.driver)
            implementation(libs.sqlite.jdbc)
            implementation(libs.ktor.client.okhttp)
        }
        jvmTest.dependencies {
            // Compose UI tests live here and not in commonTest: runComposeUiTest needs a real
            // toolkit to compose against, and a common test also runs on the browser target, where
            // every one of them fails. The desktop artifact is that toolkit.
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(compose.desktop.currentOs)
        }
        jsMain.dependencies {
            implementation(libs.ktor.client.js)
        }
    }
}

/**
 * Deja pasar `-Dtrace=<nombre>` y `-Droute=<nombre>` a las pruebas.
 *
 * Los arneses de calibración leen `traces/` entera; con `trace` se replaya una sola salida, que es
 * como se mira una traza concreta. `route` elige de qué otra traza sale la ruta que se seguía, para
 * `RouteFollowingReplay`. Ver `TraceFiles.kt`.
 */
tasks.withType<Test>().configureEach {
    listOf("trace", "route").forEach { name ->
        System.getProperty(name)?.let { systemProperty(name, it) }
    }
}
