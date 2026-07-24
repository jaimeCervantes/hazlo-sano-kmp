# Setup: compilar y generar el APK de Android

Guía para dejar una máquina Windows lista para compilar el proyecto **HazloSano** (KMP + Compose Multiplatform) por línea de comandos, sin Android Studio.

## Requisitos de versiones

Definidos en `gradle/libs.versions.toml`:

| Herramienta | Versión |
|---|---|
| JDK | 17+ (probado con Temurin 21) |
| Gradle | 9.4.1 (vía `./gradlew`, no se instala) |
| AGP | 9.2.1 |
| Kotlin | 2.3.21 |
| Compose Multiplatform | 1.11.0 |
| compileSdk / targetSdk / minSdk | 36 / 36 / 24 |

## 1. Instalar el JDK y el Android SDK (command-line tools)

No hace falta Android Studio. Con [scoop](https://scoop.sh):

```powershell
scoop bucket add java
scoop install java/temurin21-jdk
scoop install android-clt
```

`android-clt` instala las Android SDK Command-line Tools en
`~\scoop\apps\android-clt\current` y configura las variables de entorno
`ANDROID_HOME` y `ANDROID_SDK_ROOT` automáticamente.

> Alternativa sin scoop: descargar "Command line tools only" desde
> https://developer.android.com/studio#command-line-tools-only y colocarlas en
> la estructura `cmdline-tools/latest/` que exige `sdkmanager`.

## 2. Instalar los paquetes del SDK que usa el proyecto

Abre una terminal **nueva** (para que tome `ANDROID_HOME`) y acepta las licencias:

```powershell
sdkmanager --licenses
```

Luego instala los paquetes:

```powershell
sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.1.0"
```

Verifica:

```powershell
sdkmanager --list_installed
```

> Las cmdline-tools nuevas traen también el binario `android`, que reemplaza a
> `sdkmanager` (`android sdk install ...`, `android sdk list`). Cualquiera sirve;
> `sdkmanager` solo muestra un warning de deprecación.

## 3. Configurar `local.properties`

Gradle lee la ruta del SDK de `local.properties` (este archivo **no** se versiona).
Debe apuntar a una carpeta de SDK que exista realmente:

```powershell
$sdk = "$env:USERPROFILE\scoop\apps\android-clt\current" -replace '\\','\\\\' -replace ':','\:'
Set-Content -Path "local.properties" -Value "sdk.dir=$sdk"
Get-Content "local.properties"
```

Debe quedar (barras y dos puntos escapados, como exige Gradle):

```
sdk.dir=C\:\\Users\\<usuario>\\scoop\\apps\\android-clt\\current
```

## 4. Compilar y generar el APK

```powershell
./gradlew :app:androidApp:assembleDebug
```

APK resultante:

```
app\androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

Instalar en un dispositivo (con depuración USB activada):

```powershell
adb install -r app\androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

Otros comandos útiles:

```powershell
./gradlew clean                              # limpiar build
./gradlew :app:androidApp:assembleRelease    # APK release (requiere firma configurada)
adb devices                                  # listar dispositivos
adb logcat -c; adb logcat *:E                # ver errores en runtime
```

---

## Nota técnica: workaround de recursos de Compose (crash al abrir)

### Síntoma

La app compilaba e instalaba, pero **se cerraba al abrir** con:

```
FATAL EXCEPTION: main
org.jetbrains.compose.resources.MissingResourceException: Missing resource with path:
composeResources/hazlosano.app.shared.generated.resources/values/strings.commonMain.cvr
    at ...SleepSummaryCard.kt  (stringResource(...))
```

### Causa raíz

Incompatibilidad entre **Compose Multiplatform 1.11** y **AGP 9** con el plugin
`com.android.kotlin.multiplatform.library`:

- La tarea de Compose `copyAndroidMainComposeResourcesToAndroidAssets` (que copia
  los recursos de Compose a los assets de Android) queda con su propiedad
  `outputDirectory` **sin configurar**, porque el plugin de Compose no logra
  obtener el directorio de assets del plugin KMP nuevo de AGP 9.
- Resultado: los archivos generados (`strings.commonMain.cvr`, drawables) se
  preparan en `app/shared/build/generated/compose/.../preparedResources/` pero
  **nunca se empaquetan en el APK**. En runtime, `stringResource(...)` no los
  encuentra y la app crashea.

> AGP 9 **obliga** a usar `com.android.kotlin.multiplatform.library` en módulos
> KMP: ya no permite volver al `com.android.library` clásico
> (`org.jetbrains.kotlin.multiplatform` + `com.android.library` da error a partir
> de AGP 9.0). Por eso el arreglo es un workaround y no un cambio de plugin.

### Solución aplicada

En `app/androidApp/build.gradle.kts` se añadió una tarea `Copy` que:

1. Toma los recursos ya preparados de `:app:shared`
   (`build/generated/compose/resourceGenerator/preparedResources`).
2. Los reubica bajo la ruta que el runtime espera:
   `composeResources/hazlosano.app.shared.generated.resources/...`.
3. Los registra como assets del módulo `androidApp` y engancha la tarea a
   `mergeDebugAssets` / `mergeReleaseAssets`.

No se modificó ningún código de la app; solo configuración de build.

### Verificar que el APK sí incluye los recursos

```powershell
$apk = "app\androidApp\build\outputs\apk\debug\androidApp-debug.apk"
Add-Type -AssemblyName System.IO.Compression.FileSystem
[System.IO.Compression.ZipFile]::OpenRead((Resolve-Path $apk)).Entries.FullName |
    Select-String "composeResources"
```

Debe listar varias entradas
`assets/composeResources/hazlosano.app.shared.generated.resources/...`,
incluyendo `values/strings.commonMain.cvr`.

### Cuándo quitar el workaround

- Es temporal. Cuando salga una versión de Compose Multiplatform que arregle el
  wiring de recursos con AGP 9 + `com.android.kotlin.multiplatform.library`, se
  puede eliminar el bloque de `app/androidApp/build.gradle.kts` y comprobar que
  los recursos siguen entrando al APK.
- Si en el futuro el módulo `core` empieza a tener recursos de Compose, habrá que
  replicar el mismo workaround para `core` (hoy no tiene recursos de Compose, por
  eso no le afecta).
