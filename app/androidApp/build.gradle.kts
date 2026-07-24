plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.hazlosano"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hazlosano"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(projects.app.shared)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
}

// Workaround for a Compose Multiplatform 1.11 + AGP 9 (`com.android.kotlin.multiplatform.library`)
// incompatibility: the plugin's task that copies Compose resources into the Android assets is left
// with an unconfigured `outputDirectory`, so files like `strings.commonMain.cvr` never reach the APK
// and the app crashes at runtime with MissingResourceException. Here we relocate the already-prepared
// resources from :app:shared into this module's assets under the package that the runtime expects.
val sharedPreparedComposeResources: Provider<Directory> =
    project(":app:shared").layout.buildDirectory
        .dir("generated/compose/resourceGenerator/preparedResources")

val sharedComposeAssets: Provider<Directory> =
    layout.buildDirectory.dir("generated/sharedComposeAssets")

val copySharedComposeResources = tasks.register<Copy>("copySharedComposeResourcesToAssets") {
    dependsOn(
        ":app:shared:prepareComposeResourcesTaskForCommonMain",
        ":app:shared:prepareComposeResourcesTaskForAndroidMain",
        ":app:shared:copyNonXmlValueResourcesForAndroidMain",
        ":app:shared:convertXmlValueResourcesForCommonMain",
        ":app:shared:copyNonXmlValueResourcesForCommonMain",
    )
    from(sharedPreparedComposeResources)
    // Input layout:  <sourceSet>/composeResources/<qualifier>/<file>
    // Runtime expects: composeResources/hazlosano.app.shared.generated.resources/<qualifier>/<file>
    eachFile {
        val tail = relativePath.segments.drop(2) // drop "<sourceSet>/composeResources"
        relativePath = RelativePath(
            true,
            *(listOf("composeResources", "hazlosano.app.shared.generated.resources") + tail).toTypedArray(),
        )
    }
    includeEmptyDirs = false
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    into(sharedComposeAssets)
}

android {
    // Static dir (AGP 9 rejects Providers here); the task dependency is wired explicitly below.
    sourceSets.getByName("main").assets.srcDir(sharedComposeAssets.get().asFile)
}

tasks.matching { it.name == "mergeDebugAssets" || it.name == "mergeReleaseAssets" }
    .configureEach { dependsOn(copySharedComposeResources) }
