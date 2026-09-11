import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import org.gradle.process.ExecOperations

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

// ─────────────── Archivar cada APK de debug, y subirlo a Drive ───────────────

/**
 * Guarda el APK de debug con un nombre que diga de dónde salió, y lo sube si hay a dónde.
 *
 * **El nombre es el trabajo de verdad.** Gradle escribe siempre `androidApp-debug.apk` y lo
 * sobrescribe, y `versionName` lleva fijo en «1.0» desde el primer día, así que dos APK cualesquiera
 * son indistinguibles. Aquí el nombre lleva la fecha y el commit: `hazlosano-1.0-2026-09-10-4449a86`.
 *
 * **Lleva la fecha y no la hora a propósito.** Compilar dos veces el mismo commit el mismo día
 * produce el mismo nombre y sobrescribe, en vez de dejar dos copias de 68 MB del mismo código; y
 * `rclone copy` no vuelve a subir lo que ya está igual. El precio es que sólo se guarda el último
 * APK de cada commit y día, que es justo el que se quiere.
 *
 * **Y si hay cambios sin commitear, lo dice.** Un APK etiquetado con un commit que no contiene lo
 * que lleva dentro es peor que uno sin etiqueta: el sufijo `-sucio` es la diferencia entre poder
 * confiar en el nombre y no.
 *
 * Nunca rompe la compilación: sin `rclone`, sin configuración o sin red, avisa y sigue. Este módulo
 * se compila en la validación de siempre, y un respaldo que tumba el build deja de usarse.
 */
abstract class ArchiveDebugApk : DefaultTask() {

    companion object {
        /** Dónde se guarda la copia local. Por defecto fuera del repositorio: son 68 MB cada una. */
        const val PROP_ARCHIVE_DIR = "hazlosano.apk.archiveDir"

        /** El destino de `rclone`, p. ej. `gdrive:HazloSano/apks`. Sin él sólo se archiva en local. */
        const val PROP_RCLONE_REMOTE = "hazlosano.apk.rcloneRemote"
    }

    @get:InputFile
    abstract val apk: RegularFileProperty

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val archiveDir: Property<String>

    @get:Input
    @get:Optional
    abstract val rcloneRemote: Property<String>

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun archive() {
        val stamp = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val dirty = if (workingTreeIsDirty()) "-sucio" else ""
        val name = "hazlosano-${versionName.get()}-$stamp-${shortCommit()}$dirty.apk"

        val target = File(archiveDir.get()).apply { mkdirs() }.resolve(name)
        apk.get().asFile.copyTo(target, overwrite = true)
        logger.lifecycle("APK archivado en $target")

        val remote = rcloneRemote.orNull
        if (remote.isNullOrBlank()) {
            logger.lifecycle(
                "Sin `$PROP_RCLONE_REMOTE`, el APK se queda aquí. Para subirlo, añade a " +
                    "gradle.properties: $PROP_RCLONE_REMOTE=gdrive:HazloSano/apks",
            )
            return
        }

        val uploaded = runCatching {
            exec.exec {
                commandLine("rclone", "copy", target.absolutePath, remote)
                isIgnoreExitValue = true
            }.exitValue == 0
        }.getOrDefault(false)

        if (uploaded) {
            logger.lifecycle("Subido a $remote")
        } else {
            // Ni excepción ni fallo del build: el APK ya está archivado en local, que es la mitad
            // que importa. Lo que falta es red o `rclone`, y eso no lo arregla romper la compilación.
            logger.warn("No se pudo subir a $remote. ¿Está `rclone` instalado y configurado?")
        }
    }

    private fun shortCommit(): String = git("rev-parse", "--short", "HEAD") ?: "sin-commit"

    private fun workingTreeIsDirty(): Boolean = !git("status", "--porcelain").isNullOrBlank()

    /** Lo que diga git, o `null` si no está, no es un repositorio, o falla por lo que sea. */
    private fun git(vararg args: String): String? {
        val out = ByteArrayOutputStream()
        val ok = runCatching {
            exec.exec {
                commandLine(listOf("git") + args)
                standardOutput = out
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }.exitValue == 0
        }.getOrDefault(false)
        return if (ok) out.toString().trim() else null
    }
}

val archiveDebugApk = tasks.register<ArchiveDebugApk>("archiveDebugApk") {
    description = "Archiva el APK de debug con fecha y commit, y lo sube con rclone si hay destino."
    group = "distribution"

    apk.set(layout.buildDirectory.file("outputs/apk/debug/androidApp-debug.apk"))
    versionName.set(android.defaultConfig.versionName ?: "sin-version")
    archiveDir.set(
        providers.gradleProperty(ArchiveDebugApk.PROP_ARCHIVE_DIR)
            .orElse(providers.systemProperty("user.home").map { "$it/HazloSano-apks" }),
    )
    rcloneRemote.set(providers.gradleProperty(ArchiveDebugApk.PROP_RCLONE_REMOTE))

    // `finalizedBy` corre aunque `assembleDebug` falle, y entonces no hay APK que archivar.
    onlyIf { apk.get().asFile.exists() }
}

tasks.matching { it.name == "assembleDebug" }.configureEach { finalizedBy(archiveDebugApk) }
