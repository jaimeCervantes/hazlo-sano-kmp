# Bitácora — Movement tracker

Append-only. Newest entries at the bottom. Spec: [`features/movement_tracker.feature`](../../features/movement_tracker.feature).

---

## Slice 1 — Open the tracker screen with a map

- **Objective:** Tapping the "Movimiento" pillar card opens a tracker screen showing a map. Previously `onNavigateToTracker` was unwired, so nothing happened.
- **Decisions + rationale:**
  - `TrackerMap` as `expect/actual` (Android = MapLibre, other targets = placeholder) so the map is platform-specific while `TrackerScreen` stays common. Aligns with AGENTS.md ("Compose UI + platform impls live in `app/shared`").
  - Moved the MapLibre helpers (`MapLayers`, `MapBitmaps`) from `:app:androidApp` into `:app:shared/androidMain` and the MapLibre deps to the shared android source set, so the `actual` can use them.
  - `TrackerNavState` holds open/closed state outside the Composable (unit-testable).
- **Files touched:**
  - shared/commonMain: `feature/movement/tracker/ui/TrackerScreen.kt`, `TrackerMap.kt`, `feature/movement/tracker/presentation/TrackerNavState.kt`, `feature/main/ui/MainScreen.kt`
  - shared/androidMain: `TrackerMap.android.kt`, `.../ui/map/MapLayers.kt`, `MapBitmaps.kt` (moved)
  - shared/{iosMain,jvmMain,jsMain}: `TrackerMap` placeholder actuals
  - shared/commonTest: `TrackerNavStateTest.kt`
  - Gradle: `gradle/libs.versions.toml`, `app/shared/build.gradle.kts`, `app/androidApp/build.gradle.kts`
- **Key commands:** `./gradlew :app:shared:jvmTest`, `./gradlew :app:androidApp:assembleDebug`
- **Validation results:** jvmTest BUILD SUCCESSFUL; assembleDebug BUILD SUCCESSFUL; APK contains 12 `composeResources` (verified).
- **Deviations:** none.
- **Follow-ups:** live GPS, recording, persistence.

**Recap:** The tracker opens from the home pillar and renders a placeholder/real map surface per platform, with a back control and testable navigation state.

**Próximos pasos (opciones):** (1) live location on the map; (2) also open the tracker from the "Movimiento" bottom tab; (3) map style/zoom polish.

---

## Slice 2 — Show the user's live location on the map

- **Objective:** After granting the location permission, the map centers on the user's GPS position and updates it live.
- **Decisions + rationale:**
  - Implemented `core`'s `LocationRepository` as `AndroidLocationRepository` (fused provider via `callbackFlow`); `createLocationRepository()` is `expect/actual` (no-op on non-Android).
  - `TrackerViewModel` exposes `userLocation: StateFlow`, started via `startTracking()` after permission — keeps logic out of the Composable and unit-testable with a fake repo.
  - `LocationPermissionEffect` as `expect/actual` (Android requests `ACCESS_FINE_LOCATION`; others grant immediately).
  - `MovementServiceLocator` supplies the app context (init in `MainActivity`), mirroring `SleepServiceLocator`.
- **Files touched:**
  - shared/commonMain: `data/movement/LocationRepositoryFactory.kt`, `feature/movement/tracker/presentation/TrackerViewModel.kt`, `feature/movement/tracker/ui/LocationPermission.kt`, updated `TrackerScreen.kt`, `TrackerMap.kt`
  - shared/androidMain: `AndroidLocationRepository.kt`, `MovementServiceLocator.kt`, `LocationRepositoryFactory.android.kt`, `LocationPermission.android.kt`, updated `TrackerMap.android.kt`
  - shared/{iosMain,jvmMain,jsMain}: `LocationRepositoryFactory` + `LocationPermission` actuals; `TrackerMap` signature update
  - shared/commonTest: `TrackerViewModelTest.kt`
  - androidApp: `AndroidManifest.xml` (location permissions), `MainActivity.kt`
- **Key commands:** `./gradlew :app:shared:jvmTest`, `./gradlew :app:androidApp:assembleDebug`
- **Validation results:** jvmTest BUILD SUCCESSFUL (initial no-location + emits after `startTracking()`); assembleDebug BUILD SUCCESSFUL.
- **Deviations:** none.
- **Follow-ups:** record the session (path + distance + time), then persist it.

**Recap:** The tracker requests location permission and shows the live GPS position on the map, driven by a tested ViewModel over a platform location repository.

**Próximos pasos (opciones):** (1) record the traveled session; (2) compass-fused bearing (reference does this); (3) persistence.

---

## Slice 3 — Record a session (live path, distance, time) + real map style

- **Objective:** Start/Stop records the traveled path; the map draws it and the UI shows live distance and elapsed time. Also fix the map showing only country borders.
- **Decisions + rationale:**
  - Recording state in `TrackerViewModel` (`isRecording`, `traveledPoints`, `distanceMeters`, `elapsedSeconds`); distance via `core`'s `haversineMeters`; kept persistence out of this slice (needs `RouteRepository`/SQLDelight).
  - `TrackerMap` now takes `traveledPoints` and draws them on MapLibre's `traveled` layer.
  - **Map fix:** replaced the `demotiles` style (borders only) with the OSM style hosted at `jaimeCervantes/HazloSano-MapStyles`, added as `MapConstants` (matches the working reference project).
  - Validated the MapLibre API usage and the fused-location approach against the working reference at `C:\Users\S2G52\AndroidStudioProjects\HazloSano` (used as an API/behavior source, not copied — per skill).
- **Files touched:**
  - shared/commonMain: `core/util/map/MapConstants.kt`, updated `TrackerViewModel.kt`, `TrackerScreen.kt` (controls + metrics), `TrackerMap.kt`
  - shared/androidMain: updated `TrackerMap.android.kt` (OSM style, user + traveled rendering)
  - shared/{iosMain,jvmMain,jsMain}: `TrackerMap` signature update
  - shared/commonTest: expanded `TrackerViewModelTest.kt` (recording accumulates path/distance; not recorded when idle)
  - features: `movement_tracker.feature` (live-location + recording scenarios)
- **Key commands:** `./gradlew :app:shared:jvmTest`, `./gradlew :app:androidApp:assembleDebug`, `adb install -r app\androidApp\build\outputs\apk\debug\androidApp-debug.apk`
- **Validation results:** jvmTest BUILD SUCCESSFUL (5 `TrackerViewModel` tests). assembleDebug + on-device check pending user run at time of writing.
- **Deviations:** a recording unit test first hung under `advanceUntilIdle` because of the 1s timer loop; fixed by using `runCurrent()` and stopping before assertions.
- **Follow-ups:** persist the finished session (`SaveSessionUseCase` + `RouteRepository`/SQLDelight); add Compose UI tests (`runComposeUiTest`) for the screen; enable `withHostTest`/Robolectric for the Android `actual`s; instrumented tests for real map/GPS.

**Recap:** The tracker records a live session (path, distance, time) with Start/Stop, draws the route on a real street map, and is covered by ViewModel unit tests; UI and platform pieces still need Compose UI / instrumented tests.

**Próximos pasos (opciones):** (1) persist the session; (2) close the test gap (Compose UI test setup + `TrackerScreen` tests, then Robolectric/instrumented); (3) satellite style toggle using `MapConstants.SATELLITE_STYLE_URL`.

---

## Slice 4 — Persist the recorded session (single multi-pillar DB)

- **Objective:** On **Detener**, compute final stats and persist the session (metrics + path) so it survives restarts, with a "Sesión guardada" confirmation.
- **Decisions + rationale:**
  - **Interface segregation:** instead of implementing the huge `RouteRepository` (routes + GPX + sessions), introduced a focused `MovementSessionRepository` (sessions only) and retargeted `SaveSessionUseCase` to it. Routes/GPX get their own slice.
  - **Single multi-pillar DB:** movement tables live in the shared `HazloSanoDatabase`, pillar-prefixed (`MovementSessionEntity`, `MovementPointEntity`) alongside sleep/nutrition tables, 1-to-N session→points with FK + indexes.
  - Persist scalar session metrics + the key stats columns; preview is derived from points on read (no duplicate storage). Live-only stats (altitude distribution, slopes) are follow-up.
  - `createTrackerViewModel()` assembles the VM; uses the SQLDelight repo when the DB is initialized, else `NoOpMovementSessionRepository` (recording is Android-only for now).
  - **On-device migration:** Android's `AndroidSqliteDriver` does not create new tables on existing installs, so extended the project's existing `ensureNewTablesExist` hack to create the movement tables (fresh installs get them via `Schema.create`).
- **Files touched:**
  - core: `feature/movement/repository/MovementSessionRepository.kt`, retargeted `usecase/SaveSessionUseCase.kt`
  - shared/commonMain: `sqldelight/.../MovementSession.sq`, `data/movement/SqlDelightMovementSessionRepository.kt`, `MovementSessionMapping.kt`, `NoOpMovementSessionRepository.kt`, `presentation/TrackerViewModel.kt` (save-on-stop), `presentation/TrackerViewModelFactory.kt`, `ui/TrackerScreen.kt` (confirmation)
  - shared/androidMain: `data/db/DriverFactory.android.kt` (ensure movement tables)
  - shared/commonTest: `TrackerViewModelTest.kt` (save-on-stop)
  - shared/jvmTest: `data/movement/SqlDelightMovementSessionRepositoryTest.kt` (in-memory integration)
  - features: `movement_tracker.feature` (save scenario)
- **Key commands:** `./gradlew :app:shared:jvmTest`, `./gradlew :app:androidApp:assembleDebug`, `adb install -r app\androidApp\build\outputs\apk\debug\androidApp-debug.apk`
- **Validation results:** pending user run (this slice was implemented without running Gradle at the user's request). Outer test = `SqlDelightMovementSessionRepositoryTest` (integration); component test = `TrackerViewModelTest.stoppingRecordingPersistsTheSession`.
- **Deviations:** no new test dependency needed — the in-memory integration test reuses the existing JVM JDBC/SQLite driver via a single-connection `JdbcDriver`.
- **Follow-ups:** proper SQLDelight migrations (replace the `ensureNewTablesExist` hack — tech debt); sessions history screen; persist full `SessionStats`; Compose UI / instrumented tests.

**Recap:** Stopping a recording now computes stats and persists the session (metrics + path) into the shared multi-pillar database through a segregated `MovementSessionRepository`, verified by an in-memory integration test and a ViewModel save-on-stop test; the history UI and richer stats remain follow-ups.

**Próximos pasos (opciones):** (1) sessions history screen (list + detail reading them back); (2) migrate the DB versioning to real SQLDelight migrations; (3) close the UI test gap (Compose UI test for `TrackerScreen`).

---

## Session handoff — 2026-07-24 (resuming under a different account)

**Branch:** `feat/movement-tracker-screen` (base `main`). Not pushed; no PR opened yet.

**Committed (in history):** slices 1 & 2, plus the tooling commits (CLAUDE.md, Autonomous delivery mode, permission allowlist + `auto` mode). Last commit: `3d585aa`.

**Implemented but NOT yet committed and NOT yet validated with Gradle** (Gradle was not run at the user's request — treat as unverified until `jvmTest`/`assembleDebug` pass):

- **Slice 3 (recording + real map style)** — uncommitted:
  - `app/shared/.../tracker/presentation/TrackerViewModel.kt` (recording state), `.../tracker/ui/TrackerScreen.kt` (controls + metrics), `.../tracker/ui/TrackerMap.*` (traveled path + `MapConstants.OSM_STYLE_URL`), `core/util/map/MapConstants.kt`, `features/movement_tracker.feature`.
- **Slice 4 (persistence)** — uncommitted: all `data/movement/*` (repo, mapping, no-op, factory), `sqldelight/.../MovementSession.sq`, `core/.../repository/MovementSessionRepository.kt`, retargeted `SaveSessionUseCase.kt`, `DriverFactory.android.kt` (ensure movement tables), `jvmTest/.../SqlDelightMovementSessionRepositoryTest.kt`, `commonTest` `TrackerViewModelTest.kt`.
- **Docs/skill (uncommitted):** `.agents/skills/feature-delivery/SKILL.md` (outside-in double-loop + one-time tooling bootstrap + bitácora rule + "improve reference / mandatory tests"), `README.md` ("Validating a change" section), `docs/features/movement-tracker-bitacora.md` (this file).

**To resume — validate first:**
```powershell
.\gradlew.bat :app:shared:jvmTest
.\gradlew.bat :app:androidApp:assembleDebug
adb install -r app\androidApp\build\outputs\apk\debug\androidApp-debug.apk
```
Then on device: Movimiento → Iniciar → move → Detener → expect "Sesión guardada". (No uninstall needed; `ensureNewTablesExist` creates the movement tables on the existing DB.)

**Suggested commits (after validation):**
```powershell
# Slice 3
git add core/src/commonMain/kotlin/com/hazlosano/core/util app/shared/src/*/kotlin/com/hazlosano/feature/movement/tracker features
git commit -m "feat(movement): record session with live path, distance and time; use OSM map style"
# Slice 4
git add core app/shared/src/commonMain/kotlin/com/hazlosano/data/movement app/shared/src/commonMain/sqldelight app/shared/src/androidMain/kotlin/com/hazlosano/data/db app/shared/src/jvmTest app/shared/src/commonTest
git commit -m "feat(movement): persist recorded sessions in the shared database"
# Docs/skill
git add .agents/skills/feature-delivery/SKILL.md README.md docs/features
git commit -m "docs: outside-in double-loop skill, bitacora, and validation workflow"
```

**Known risks:** the persistence code was written without compiling — SQLDelight generated names (`movementSessionQueries`, insert param names) should be verified by the first `jvmTest`. If it fails, the fix is local to `SqlDelightMovementSessionRepository.kt` / `MovementSession.sq`.

**Follow-ups (open):** sessions history screen; real SQLDelight migrations (replace the `ensureNewTablesExist` hack — tech debt); close the UI test gap (Compose UI test `runComposeUiTest` for `TrackerScreen`; Robolectric + `withHostTest` for Android `actual`s; instrumented `androidTest` for real map/GPS); persist full `SessionStats`; open tracker from the "Movimiento" bottom tab too; push branch + open PR.

**Note for the next session:** the Claude Code permission allowlist + `auto` mode (`.claude/settings.json`) take effect on a fresh session, so under the new account you should get far fewer permission prompts.

---

## Slice 5 — Session history screen

- **Objective:** Show the sessions already persisted: a "Historial" action on the tracker opens a list of recorded sessions (date, distance, time), newest first, with an empty state and back to the tracker. Spec: [`features/movement_history.feature`](../../features/movement_history.feature).
- **Decisions + rationale:**
  - **Navigation:** replaced `TrackerNavState` (open/closed) with `MovementNavState` (`Closed | Tracker | History`) under `feature/movement/presentation/`, so pillar-level navigation is one testable state holder instead of one boolean per screen. Back from the history returns to the tracker (`openTracker()`), back from the tracker closes the pillar.
  - **`GetSessionsUseCase` retargeted** from `RouteRepository` to the segregated `MovementSessionRepository` (same ISP move as `SaveSessionUseCase` in slice 4), so the history needs only session persistence.
  - **Ordering + formatting in the ViewModel**, not in the query or the Composable: `MovementHistoryViewModel` sorts by date descending and maps to `SessionListItem` display labels, so the history is correct regardless of the repository feeding it and is fully unit-testable.
  - **`MovementFormat`** extracted as the single formatter for distance/duration/date shared by the tracker and the history (the tracker's private `formatDistance`/`formatDuration` were deleted). Multiplatform-safe (no platform date/number formatting) and the time zone is a parameter, so labels are deterministic under test.
  - **`movementSessionRepository()` provider** extracted from `TrackerViewModelFactory` — both factories now resolve SQLDelight-or-no-op through one place.
  - **`inMemoryHazloSanoDatabase()`** moved out of `SqlDelightMovementSessionRepositoryTest` into `jvmTest/data/db/` so the history integration test reuses the same real persistence stack.
  - UI state as a sealed interface (`Loading | Empty | Sessions | Error`) so the screen renders a closed set of cases.
- **Files touched:**
  - core: `usecase/GetSessionsUseCase.kt` (now depends on `MovementSessionRepository`)
  - shared/commonMain: `feature/movement/presentation/MovementNavState.kt`, `MovementFormat.kt`, `feature/movement/history/presentation/MovementHistoryViewModel.kt`, `MovementHistoryViewModelFactory.kt`, `feature/movement/history/ui/MovementHistoryScreen.kt`, `data/movement/MovementSessionRepositoryProvider.kt`, updated `feature/main/ui/MainScreen.kt`, `feature/movement/tracker/ui/TrackerScreen.kt` ("Historial" action + shared formatting), `tracker/presentation/TrackerViewModelFactory.kt`; deleted `tracker/presentation/TrackerNavState.kt`
  - shared/commonTest: `movement/presentation/MovementNavStateTest.kt`, `MovementFormatTest.kt`, `movement/history/presentation/MovementHistoryViewModelTest.kt`; deleted `TrackerNavStateTest.kt`
  - shared/jvmTest: `feature/movement/history/MovementHistoryIntegrationTest.kt`, `data/db/InMemoryHazloSanoDatabase.kt`, simplified `data/movement/SqlDelightMovementSessionRepositoryTest.kt`
  - features: `movement_history.feature`
- **Key commands:** `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check :app:webApp:check`
- **Validation results:** `:app:shared:jvmTest` BUILD SUCCESSFUL — 43 tests, 0 failures (outer: `MovementHistoryIntegrationTest` 2 — save through SQLDelight then read back as rows, newest first; component: `MovementHistoryViewModelTest` 4, `MovementFormatTest` 6, `MovementNavStateTest` 5). `:app:androidApp:assembleDebug` BUILD SUCCESSFUL. `:app:desktopApp:check :app:webApp:check` BUILD SUCCESSFUL (common UI stays multiplatform-safe).
- **Deviations:** none. Note that `kotlinx.datetime` 0.8.0 deprecates `Instant`/`monthNumber`/`dayOfMonth`; `MovementFormat` follows the idiom already used by `TimeFormat.kt` (warnings only) — migrating both to `kotlin.time.Instant` is separate tech debt.
- **Follow-ups:** session detail screen (map of the saved path — `getSessionPoints` already exists); delete a session; Compose UI test for the list/empty states.

**Recap:** Recorded sessions are now visible: the tracker's "Historial" action opens a list of saved sessions with date, distance and time, newest first, with an empty state and back to the tracker — covered end-to-end by an integration test over the real SQLDelight stack plus ViewModel, formatting, and navigation unit tests.

**Próximos pasos (opciones):** (1) session detail with the saved route drawn on the map; (2) real SQLDelight migrations (replace the `ensureNewTablesExist` hack); (3) Compose UI test setup for the movement screens.

---

## Slice 6 — Session detail with the recorded route on the map

- **Objective:** Tapping a session in the history opens its detail: the stored route drawn on the map (framed to the path), plus distance, time, average pace and elevation gain. Spec: [`features/movement_session_detail.feature`](../../features/movement_session_detail.feature).
- **Decisions + rationale:**
  - **Framing logic moved into the domain:** `GeoBounds` + `List<UserLocation>.boundingBox()` in `core` — the clean, target-neutral version of the reference's `resolveInitialViewportPoints`. The decision "where should the camera look" is now unit-tested on the JVM, and the Android map code only translates it to MapLibre (`CameraUpdateFactory.newLatLngBounds`), with `spansAnArea`/center covering the degenerate single-coordinate path.
  - **One map for the pillar:** `TrackerMap` was generalized into `MovementMap(userLocation, path, fitPathInView)` under `feature/movement/ui/`, reused by the tracker (follows the live position) and the detail (frames the finished path). Avoids duplicating the MapLibre + lifecycle plumbing in a second `expect/actual`.
  - **`GetSessionDetailUseCase`** combines `getAllSessions()` with `getSessionPoints(id)` instead of adding a `getSessionById` query: the repository contract stays narrow and no `.sq`/schema change (and therefore no migration) is needed for this slice.
  - **`MovementDestination` became a sealed interface** so `SessionDetail(sessionId)` can carry its argument; back from the detail returns to the history, keeping the pillar's navigation in one tested state holder.
  - `SessionDetailUiState` is sealed (`Loading | Missing | Detail | Error`), and "no stored route" is modeled as `hasPath` on the UI model instead of a separate state, since the summary is still shown.
  - **Reference comparison** (`C:\Users\S2G52\AndroidStudioProjects\HazloSano`, `SessionDetailScreen`/`RouteMap`) was used for the MapLibre camera APIs only. Deliberately not carried over: satellite toggle, the `Route`/GPX concept, the 8-metric `CompactStatsHeader`, and the viewport logic living inside the Composable.
- **Files touched:**
  - core: `model/GeoBounds.kt`, `model/SessionDetail.kt`, `usecase/GetSessionDetailUseCase.kt`
  - core/commonTest: `model/GeoBoundsTest.kt`, `usecase/GetSessionDetailUseCaseTest.kt`
  - shared/commonMain: `feature/movement/ui/MovementMap.kt` (expect, replaces `tracker/ui/TrackerMap.kt`), `feature/movement/detail/presentation/SessionDetailViewModel.kt`, `SessionDetailViewModelFactory.kt`, `feature/movement/detail/ui/SessionDetailScreen.kt`, updated `presentation/MovementNavState.kt` (sealed destinations), `presentation/MovementFormat.kt` (pace + elevation), `history/ui/MovementHistoryScreen.kt` (clickable rows), `tracker/ui/TrackerScreen.kt`, `feature/main/ui/MainScreen.kt`
  - shared/androidMain: `feature/movement/ui/MovementMap.android.kt` (path framing via `newLatLngBounds`, posted after layout with a center+zoom fallback); deleted `tracker/ui/TrackerMap.android.kt`
  - shared/{iosMain,jvmMain,jsMain}: `MovementMap` placeholder actuals (old `TrackerMap` actuals deleted)
  - shared/commonTest: `feature/movement/detail/presentation/SessionDetailViewModelTest.kt`, expanded `MovementFormatTest.kt` + `MovementNavStateTest.kt`
  - shared/jvmTest: `feature/movement/detail/SessionDetailIntegrationTest.kt`
  - features: `movement_session_detail.feature`
- **Key commands:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :core:check :app:desktopApp:check :app:webApp:check`
- **Validation results:** `:app:shared:jvmTest` BUILD SUCCESSFUL — 55 tests, 0 failures (new: `SessionDetailIntegrationTest` 3, `SessionDetailViewModelTest` 4, plus `MovementFormatTest` 9 and `MovementNavStateTest` 7). `:core` BUILD SUCCESSFUL — 13 tests, 0 failures (new: `GeoBoundsTest` 5, `GetSessionDetailUseCaseTest` 4). `:app:androidApp:assembleDebug`, `:app:desktopApp:check` and `:app:webApp:check` BUILD SUCCESSFUL.
- **Deviations:** none in scope. Two judgment calls worth recording: the detail reads the session from the full list (no new query, no migration), and the camera fit is posted to the `MapView` because MapLibre needs the rendered size to fit bounds — with a center+zoom fallback when the view has no size yet or the path is a single coordinate.
- **Follow-ups:** on-device check of the framing on a real recorded route (the map layer stays untested on the host — Compose UI/Robolectric/instrumented tests are still the open gap); satellite toggle using `MapConstants.SATELLITE_STYLE_URL`; delete/rename a session; real SQLDelight migrations.

**Recap:** A recorded session can now be opened from the history and reviewed: its stored route is drawn on the map framed to the path's bounds, with distance, time, average pace and elevation gain, and a clear message when the session stored no route — with the framing rule, the detail use case, the ViewModel and the formatting all unit-tested, and the whole read path covered by an integration test over SQLDelight.

**Próximos pasos (opciones):** (1) on-device validation of the route framing, then satellite toggle; (2) delete/rename sessions from the history or detail; (3) real SQLDelight migrations (replace the `ensureNewTablesExist` hack); (4) Compose UI test setup for the movement screens.

---

## Corrección — El mapa no se renderizaba hasta un evento externo (afecta a los slices 1, 3 y 6)

- **Síntoma:** al abrir el detalle de una sesión (y en realidad en cualquier pantalla con mapa) no se veía nada; bloqueando y desbloqueando el teléfono aparecía el mapa con la ruta. Con diagnóstico en pantalla se confirmó que el estilo ni siquiera empezaba a descargarse hasta ese momento, y que la cámara seguía en zoom 0.
- **Causa raíz:** MapLibre dibuja por defecto en un `SurfaceView`, que tiene su propia ventana y solo recibe superficie cuando el sistema vuelve a disponer la ventana. Dentro de un `AndroidView` de Compose eso no ocurría al componer la pantalla, así que el motor nativo no arrancaba: sin superficie no hay bucle de render, y sin bucle de render no se procesa ni la carga del estilo.
- **Por qué la referencia no falla:** su `MapLibreOfflineRepository` llama a `OfflineManager.getInstance(context)`, que activa el `FileSource` a nivel de aplicación; nuestro proyecto no tiene esa pieza.
- **Verificación (no hipótesis):** se extrajo `android-sdk-11.5.1.aar` del caché de Gradle y se leyó el bytecode de `MapView` con `javap`. Hallazgos que corrigieron dos suposiciones equivocadas: `onCreate()` **no** inicializa nada (solo guarda `savedInstanceState`; la superficie se crea en el constructor vía `initialize()` → `initializeDrawingSurface`), y `onStart()` es quien llama a `ConnectivityReceiver.activate()` y **`FileSource.activate()`**, que habilita las descargas de estilo y tiles.
- **Arreglo:**
  - `MapLibreMapOptions.createFromAttributes(context).textureMode(true)` — render dentro de la jerarquía de vistas en vez de un `SurfaceView` con ventana propia.
  - Restaurado el enlace de ciclo de vida (`onCreate`/`onStart`/`onResume`/`onPause`/`onStop`/`onDestroy`) con puesta al día del estado actual, ahora sabiendo qué hace cada llamada.
  - Dibujado y encuadre movidos al bloque `update` del `AndroidView` (que corre con la vista ya adjunta y medida) **y** repetidos en el callback del estilo con `rememberUpdatedState`, porque ese callback puede llegar después de la última pasada de `update` y dejaba la cámara en la vista mundial.
- **Intentos fallidos (registrados a propósito):** (1) mover `onStart`/`onResume` a `OnAttachStateChangeListener` — no cambió nada, la vista ya estaba adjunta; (2) fijar la cámara inicial en el centro de la ruta antes de cargar el estilo — empeoró el síntoma y se revirtió; (3) eliminar todas las llamadas de ciclo de vida imitando a la referencia — dejó el `FileSource` sin activar.
- **Archivos tocados:** `app/shared/src/androidMain/.../feature/movement/ui/MovementMap.android.kt`.
- **Validación:** `.\gradlew.bat :app:androidApp:assembleDebug` BUILD SUCCESSFUL; `.\gradlew.bat :app:shared:jvmTest :core:jvmTest` BUILD SUCCESSFUL (68 tests). Comprobación en dispositivo: mapa y ruta visibles al abrir el detalle, sin bloquear/desbloquear.
- **Lección:** el mapa del slice 1 se dio por bueno con `assembleDebug` y nunca se validó en dispositivo; el fallo estuvo latente tres slices y salió a la luz en el detalle porque el tracker lo enmascaraba (su diálogo de permisos provocaba el pause/resume que despertaba al render). Ninguna prueba de host podía cazarlo: en JVM el mapa es un placeholder. Es deuda de test instrumentado (`connectedDebugAndroidTest`), no de test de host.

**Recap:** El mapa ya renderiza al entrar a la pantalla, con la ruta guardada dibujada y encuadrada, tras identificar por bytecode que el bloqueo era el `SurfaceView` de MapLibre y el `FileSource` sin activar.

**Próximos pasos (opciones):** (1) migrar el resto del tracking desde el proyecto de referencia; (2) test instrumentado que cubra el render del mapa; (3) mapas offline (`OfflineManager`), que además activa el `FileSource` a nivel de app.

---

## Slice 7 (migración A) — Grabación en segundo plano

- **Objetivo:** que la sesión siga grabándose con la app en segundo plano o la pantalla apagada, con notificación persistente, y que al volver al tracker se vea lo grabado. Spec: [`features/movement_background_recording.feature`](../../features/movement_background_recording.feature). Backlog: [`movement-tracking-migration.md`](movement-tracking-migration.md) punto A.
- **Decisiones + razones:**
  - **La grabación deja de vivir en el ViewModel.** Se introduce el contrato `RecordingController` en `core` y `TrackerViewModel` pasa a ser un observador que reenvía iniciar/detener. Salir de la pantalla ya no puede terminar una sesión.
  - **Reglas de grabación en `core` como transiciones puras** (`RecordingState` + `started`/`recorded`/`elapsedAt`/`stopped`), separadas de dónde se ejecutan. Es lo que permite probar en JVM el comportamiento que en dispositivo depende del servicio.
  - **Tiempo transcurrido derivado del reloj, no de contar ticks.** Antes un bucle `delay(1000)` incrementaba un contador; con la pantalla apagada el sistema estrangula esos ticks y la duración salía corta. Ahora se calcula desde `startedAtMillis`, y hay un test que fija justo eso (diez minutos sin un solo tick intermedio).
  - **`SessionRecording`** (en `app/shared`) concentra colectar ubicaciones, mantener el estado y persistir al detener; lo usan tanto el servicio de Android como el controlador en proceso, así que las reglas no se duplican por plataforma.
  - **`expect/actual` del controlador:** Android delega en `MovementRecordingService` (foreground, `foregroundServiceType="location"`); jvm/js/ios usan `InProcessRecordingController`, que conserva el comportamiento actual para que esos targets sigan funcionando.
  - **`MovementRecordingStore`:** estado a nivel de proceso para publicar lo que graba el servicio. Es una excepción consciente a la regla de "no estado mutable global" de `AGENTS.md`: el servicio sobrevive a cualquier pantalla y la alternativa (bindear el servicio desde cada Composable) añade estado de conexión que también hay que gestionar. Está acotado a la grabación en curso y solo lo escribe el servicio.
  - **Permiso de notificaciones** se pide junto al de ubicación en Android 13+, pero **no condiciona la grabación**: si se deniega, el servicio corre igual y solo se queda sin notificación visible.
  - Frente a la referencia (`NavigationService`): allí el estado vive en un `companion object` global y el servicio mezcla navegación por ruta con grabación; aquí el estado es un componente probado de `core` y el servicio queda como adaptador delgado.
- **Archivos tocados:**
  - core: `model/RecordingState.kt`, `repository/RecordingController.kt`
  - core/commonTest: `model/RecordingStateTest.kt`
  - shared/commonMain: `data/movement/SessionRecording.kt`, `InProcessRecordingController.kt`, `RecordingControllerFactory.kt`, `tracker/presentation/TrackerViewModel.kt` (reescrito), `TrackerViewModelFactory.kt`, `tracker/ui/TrackerScreen.kt`
  - shared/androidMain: `data/movement/MovementRecordingService.kt`, `MovementRecordingStore.kt`, `RecordingControllerFactory.android.kt`, `tracker/ui/LocationPermission.android.kt` (pide también `POST_NOTIFICATIONS`)
  - shared/{jvm,js,ios}Main: `RecordingControllerFactory.<target>.kt`
  - shared/commonTest: `data/movement/SessionRecordingTest.kt`, `TrackerViewModelTest.kt` (reescrito contra un controlador falso)
  - androidApp: `AndroidManifest.xml` (permisos `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS` y declaración del servicio)
  - features: `movement_background_recording.feature`
- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :core:check :app:desktopApp:check :app:webApp:check`
- **Resultados de validación:** `:app:shared:jvmTest` BUILD SUCCESSFUL — 62 tests, 0 fallos (nuevos: `SessionRecordingTest` 6, `TrackerViewModelTest` reescrito a 6). `:core` BUILD SUCCESSFUL — 20 tests, 0 fallos (nuevo: `RecordingStateTest` 7). `assembleDebug`, `:app:desktopApp:check` y `:app:webApp:check` BUILD SUCCESSFUL.
- **Desviaciones:** dos tests de `SessionRecording` fallaron primero con `UncompletedCoroutinesError` porque el bucle de tiempo seguía vivo al terminar `runTest`; se corrigieron capturando el estado y deteniendo la grabación antes de las aserciones (mismo tropiezo que en el slice 3, ahora en el componente extraído).
- **Sin cobertura de host (dicho explícitamente):** el servicio en primer plano, la notificación y la entrega de ubicaciones con la pantalla apagada. Por decisión del usuario esto se valida **manualmente en dispositivo** en lugar de con tests instrumentados. Prueba manual acordada: iniciar, bloquear el teléfono, caminar varios minutos, volver y detener; comprobar que la notificación muestra distancia y tiempo crecientes y que la sesión guardada incluye el tramo grabado en segundo plano.

**Recap:** La grabación pasó del ViewModel a un servicio en primer plano en Android (y a un controlador en proceso en el resto de targets), con las reglas de sesión extraídas a `core` como transiciones puras y el tiempo derivado del reloj en vez de contar ticks; la pantalla del tracker ahora solo observa y reenvía iniciar/detener.

**Próximos pasos (opciones):** (1) validar en dispositivo el punto A y luego seguir con B (Kalman + estadísticas) del backlog; (2) resolver la deuda de migraciones SQLDelight antes de las tablas de rutas; (3) mostrar la sesión en curso también fuera del tracker (por ejemplo en el pilar).

---

## Corrección del slice 7 — Reinicio del servicio y decisión sobre el estado de proceso

- **Origen:** al revisar, a petición del usuario, si `MovementRecordingStore` (estado a nivel de proceso) debía eliminarse en un slice propio.
- **Decisión sobre el estado global:** se mantiene, documentado como decisión consciente y no como deuda. Refleja algo que el sistema ya impone como único (hay una grabación porque hay un servicio en primer plano), tiene un único escritor, es `internal` al módulo y no guarda lógica. La alternativa —servicio *bound*— añadiría estado de conexión, ventana de "aún no conectado" en la UI y reconexión, seguiría sin ser testeable en host, y cumpliría la letra de la regla de `AGENTS.md` pero no su intención: el comportamiento testeable ya vive fuera, en `RecordingState` de `core`.
- **Fallo encontrado en esa revisión (introducido en el slice 7):** `onStartCommand` devolvía `START_STICKY`, así que si el sistema mataba el proceso recreaba el servicio con un `Intent` nulo; el `when (intent?.action)` no contemplaba ese caso y quedaba un servicio vivo que no grababa nada, sin notificación coherente y con la sesión perdida en silencio.
- **Arreglo:** el caso sin acción apaga el servicio (`stopIfIdle`, que nunca desmonta una grabación viva) y se devuelve `START_NOT_STICKY`, porque reanudar sin haber persistido la sesión no es posible. El comportamiento ahora es honesto: no finge grabar.
- **Lo que este arreglo NO resuelve:** la sesión en curso se sigue perdiendo si el sistema mata el proceso. Eso requiere persistir la grabación incrementalmente y se registró como punto **G** del backlog, con su encuadre, en lugar de resolverlo aquí a medias.
- **Archivos tocados:** `app/shared/src/androidMain/.../data/movement/MovementRecordingService.kt`; `docs/features/movement-tracking-migration.md` (punto G nuevo).
- **Validación:** `.\gradlew.bat :app:androidApp:assembleDebug` BUILD SUCCESSFUL. Sin cobertura de host: es código del servicio Android. Comprobación manual posible con `adb shell am kill com.hazlosano` durante una grabación.

**Recap:** El estado a nivel de proceso se conserva como decisión razonada y documentada; a cambio, la revisión destapó un reinicio de servicio mal manejado que ya está corregido, y el problema de fondo —perder la sesión si el sistema mata la app— queda encuadrado como punto G del backlog en vez de disimulado.

**Próximos pasos (opciones):** (1) validar A en dispositivo y decidir si G sube de prioridad según lo que pase en uso real; (2) seguir con B (Kalman + estadísticas); (3) deuda de migraciones SQLDelight antes de las tablas de rutas.

---

## Corrección del slice 7 (2) — La ruta terminada seguía pintada, y el guardado podía cancelarse

- **Síntoma reportado:** al detener la sesión, el recorrido seguía dibujado en el mapa del tracker; navegar al historial o a otros pilares y volver no lo quitaba, y solo desaparecía al cerrar y reabrir la app.
- **Causa:** `MovementRecordingStore` conserva el último estado publicado —incluidos sus puntos— y la pantalla dibujaba `recording.traveledPoints` sin distinguir si la sesión seguía viva. Como el store vive a nivel de proceso, el recorrido sobrevivía a cualquier navegación y solo se limpiaba al morir el proceso. Es consecuencia directa de la decisión de publicar el estado en proceso: la decisión sigue siendo válida, pero exigía definir qué ocurre al terminar.
- **Segundo fallo, encontrado al arreglar el primero:** `stop()` lanzaba el guardado y el servicio llamaba a `stopSelf()` acto seguido; `onDestroy` cancela el scope, así que **la escritura de la sesión podía quedar cancelada a medias**. No se manifestó porque la escritura suele ser más rápida que el apagado, pero era una pérdida de datos esperando a ocurrir.
- **Arreglos:**
  - Al detener, el estado en curso vuelve a vacío: una sesión terminada pertenece al historial, no al tracker. El mapa deja de dibujarla.
  - La confirmación pasa de un booleano a `lastSavedSession: StateFlow<RecordingState?>`, que además lleva el resumen final, así que la pantalla muestra "Sesión guardada · 1.25 km · 10:00" en vez de perder el dato al limpiar el estado. La pantalla la descarta tras cinco segundos (`acknowledgeSavedSession`), de modo que no reaparece cada vez que se vuelve al tracker.
  - El guardado se envuelve en `withContext(NonCancellable)` y `stop()` devuelve su `Job`; el servicio espera (`join`) antes de `stopForeground`/`stopSelf`.
- **Archivos tocados:** core `repository/RecordingController.kt`; shared/commonMain `data/movement/SessionRecording.kt`, `InProcessRecordingController.kt`, `tracker/presentation/TrackerViewModel.kt`, `tracker/ui/TrackerScreen.kt`; shared/androidMain `MovementRecordingStore.kt`, `MovementRecordingService.kt`, `RecordingControllerFactory.android.kt`; shared/commonTest `SessionRecordingTest.kt`, `TrackerViewModelTest.kt`.
- **Validación:** `:app:shared:jvmTest` BUILD SUCCESSFUL — 67 tests, 0 fallos (nuevos: la ruta terminada se limpia, la confirmación se ofrece una vez y se olvida al reconocerla, y una nueva sesión descarta la confirmación anterior; en el ViewModel, que el mapa deja de recibir el recorrido terminado). `assembleDebug`, `:core:check`, `:app:desktopApp:check`, `:app:webApp:check` BUILD SUCCESSFUL.
- **Desviación:** el test `reportsTheRealDurationEvenIfNothingTickedInBetween` tuvo que reescribirse: como el estado en curso ahora se limpia al detener, la duración solo es observable en la sesión guardada.
- **Nota:** este fallo era observable únicamente ejecutando la app —ningún test de host lo habría cazado, porque el estado global de proceso es exactamente lo que no se reproduce en un test unitario— y salió de la prueba manual del usuario. Refuerza que la validación en dispositivo es parte del ciclo, no un extra.

**Recap:** Al detener, el tracker vuelve a estado limpio y muestra durante unos segundos el resumen de lo guardado; además el guardado ya no puede cancelarse al apagar el servicio.

**Próximos pasos (opciones):** (1) validar en dispositivo estas dos correcciones; (2) seguir con B (Kalman + estadísticas) del backlog; (3) G (sobrevivir a que el sistema mate el proceso) si en uso real ocurre.
