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

---

## Slice 8 (migración B1) — Filtrar la ubicación antes de acumular distancia

- **Objetivo:** que la distancia grabada refleje lo recorrido de verdad. Hasta ahora `RecordingState.recorded()` acumulaba haversine sobre el punto **crudo** que entregaba el proveedor fusionado, así que el ruido del GPS sumaba metros que nadie anduvo. Spec: [`movement_location_smoothing.feature`](../../features/movement_location_smoothing.feature) (8 escenarios).
- **Recorte:** el punto B del backlog se partió en B1 (filtro, todo en `core`) y B2 (tests de `CalculateStatsUseCase` + métricas en el detalle). B2 queda pendiente.

- **Decisiones + porqué:**
  - **La bici obligó a rediseñar los umbrales** (pregunta del usuario durante el encuadre, antes de escribir código). La app **nunca sabe qué actividad grabas**: no hay selector y `MovementSession` no lleva tipo. A 2 s de muestreo, caminar avanza ~2,8 m por lectura y una bici ~11-30 m: cualquier constante en metros codifica en silencio una actividad y destroza la otra. Por eso **ningún umbral es una constante métrica**: el descarte por salto se expresa como **velocidad sobre el intervalo real** (25 m/s ≈ 90 km/h) y el suelo de ruido se deriva de **la precisión que reporta la propia lectura**. Sin esa corrección, un límite pensado para caminar habría rechazado todos los puntos de un ciclista y la sesión habría salido en cero.
  - **`KalmanFilter` reescrito como valor inmutable.** La referencia lo tiene mutable; un filtro mutable compartido entre copias de un estado inmutable es un bug esperando. Ahora cada lectura devuelve un filtro nuevo, y "cada grabación empieza de cero" sale gratis en vez de depender de acordarse de llamar a `reset()`.
  - **La velocidad que adapta el filtro se estima descontando la precisión de la lectura.** En la referencia se estima sobre posiciones crudas, así que diez metros de ruido alrededor de alguien parado se leen como 5 m/s y el filtro deja de suavizar justo cuando más falta hace. Es el fallo que hacía inútil el suavizado en reposo.
  - **`LocationFilter` separado de `RecordingState`** (SRP): `RecordingState` responde "qué grabamos", `LocationFilter` responde "esto merece grabarse". Así `RecordingState` sigue siendo una transición pura sin internals del filtro colándose en el estado que observa Compose, y sus 7 tests conservan su significado.
  - **Se graba la posición corregida, no la cruda.** `CalculateStatsUseCase` recalcula la distancia desde los puntos y `SessionDetail` redibuja el trazado: guardar puntos crudos con distancia filtrada habría hecho que trazado, distancia y estadísticas contaran tres historias distintas.
  - **Rechazo sin contaminar:** una lectura con precisión pésima o un salto imposible **no** se le pasa al suavizador (una medida absurda arrastraría la estimación). Una lectura buena pero por debajo del suelo de ruido **sí** refina la estimación aunque no haga crecer el trazado, y la siguiente distancia se mide desde el último punto **aceptado** — por eso el movimiento lento se acumula a lo largo de varias lecturas en vez de descartarse una por una para siempre.
  - **Sin `expect`/`actual` ni dependencias nuevas.** Todo el filtro es Kotlin común en `core`.
  - **`TrackNavigationUseCase` (código muerto de la slice C) usaba la API vieja** y duplicaba a mano kalman + puerta de velocidad con estado mutable. En vez de adaptarlo, se apunta al `LocationFilter`: compila, pierde la duplicación y llega a C ya alineado.

- **Archivos tocados:**
  - core/commonMain: `filter/KalmanFilter.kt` (reescrito), `filter/LocationFilter.kt` (nuevo), `usecase/TrackNavigationUseCase.kt`
  - core/commonTest: `filter/LocationFilterTest.kt` (nuevo, 10 tests), `filter/GpsTraces.kt` (nuevo, generador de trazas sintéticas)
  - shared/commonMain: `data/movement/SessionRecording.kt`
  - shared/commonTest: `data/movement/SessionRecordingTest.kt` (2 tests nuevos + helper de coordenadas)
  - features: `movement_location_smoothing.feature`

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :core:check`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`

- **Resultados de validación:** `:core` 30 tests, 0 fallos (antes 20). `:app:shared:jvmTest` 69 tests, 0 fallos (antes 67). `:core:check`, `assembleDebug`, `:app:desktopApp:check` y `:app:webApp:check` BUILD SUCCESSFUL.

  Medido sobre las trazas sintéticas (recorrido real → grabado):

  | Traza | Real | Grabado | Desvío |
  |---|---|---|---|
  | Parado 2 min, ruido ±10 m | 0 m | **10,2 m** (crudo: **714,3 m**) | — |
  | Caminando 4 min (~5 km/h) | 333,2 m | 333,5 m | 0,1 % |
  | Bici 2 min (~40 km/h) | 1298,0 m | 1305,5 m | 0,6 % |
  | Bajada (~55 km/h) | 887,4 m | 889,0 m | 0,2 % |
  | Bici + parada + bici | 836,0 m | 838,0 m | 0,2 % |
  | Con un salto de 500 m inyectado | 858,0 m | 866,8 m | 1,0 % |

  El caso estático es el titular: **714 m → 10 m**, y el test compara contra el crudo para que no pueda pasar en verde con una traza que no sea ruidosa. Las cotas de los tests (5 % de desvío, 15 m parado) están holgadas respecto a lo medido a propósito, para que afinar constantes no rompa la suite mientras el comportamiento siga siendo cierto.

- **Desviaciones:**
  - La primera aserción de `aSingleWildReadingDoesNotCorruptTheSession` estaba mal planteada (comprobaba que ningún punto pasara de la latitud del salto, pero el recorrido de prueba pasa legítimamente más al norte). Se sustituyó por una más fuerte: la sesión **con** el salto es idéntica punto por punto a la sesión **sin** él.
  - Hubo que tocar `TrackNavigationUseCase`, que no estaba en el alcance: era código muerto que no compilaba con la API nueva del filtro.
  - `:app:androidApp:assembleDebug` falló una vez al lanzarlo en paralelo con `:app:webApp:check` (`copySharedComposeResourcesToAssets`, choque por los mismos recursos compartidos). Por separado pasan los dos; no es del código.

- **Sin cobertura de host (dicho explícitamente):** las constantes están calibradas contra ruido sintético uniforme, no contra un GPS real. El ruido real está correlacionado (no es blanco), así que el comportamiento en reposo en la calle puede diferir. **Prueba manual pendiente:** grabar una sesión dejando el teléfono quieto varios minutos y comprobar que la distancia no crece; y grabar un trayecto conocido, a pie y en bici, comparando el total con otra app o con el recorrido real.

- **Seguimientos:**
  - **B2:** cubrir `CalculateStatsUseCase` con tests y mostrar en el detalle las métricas que ya se persisten.
  - **Muestreo a 2 s y recorte de curvas:** en una bajada con curvas el trazado corta las curvas y la distancia se queda **corta** — error contrario al que arregla esta slice. Se resolvería muestreando más a menudo, a costa de batería. No se toca aquí.
  - **Calibrar constantes en dispositivo:** `MAX_PLAUSIBLE_SPEED_MPS`, `MAX_USABLE_ACCURACY_METERS`, el suelo de ruido y el ruido de proceso mínimo son primeras estimaciones razonadas, no valores medidos en campo.
  - Las sesiones ya grabadas conservan sus distancias infladas; no se recalculan.

**Recap:** Cada ubicación pasa ahora por un filtro en `core` que descarta lecturas imprecisas y saltos imposibles, suaviza el resto con un Kalman adaptativo inmutable y solo deja crecer el trazado cuando el desplazamiento supera la incertidumbre de la propia lectura. Sobre trazas sintéticas, dos minutos parado bajan de 714 m a 10 m mientras caminar y andar en bici conservan su distancia con menos de un 1 % de desvío. La adaptación por velocidad —y que ningún umbral esté en metros fijos— es lo que hace que el mismo filtro sirva para caminar y para la bici sin que la app sepa cuál de las dos estás haciendo.

**Próximos pasos (opciones):** (1) validar en dispositivo (teléfono quieto + trayecto conocido a pie y en bici) antes de seguir; (2) B2: tests de `CalculateStatsUseCase` y métricas en el detalle; (3) deuda de migraciones SQLDelight antes de las tablas de rutas de C.

---

## Corrección del slice 8 — Cubrir los cuatro modos: caminata, trote, carrera y bici

- **Origen:** el usuario señaló que el pilar contempla **caminata, trote, carrera y bici**, y B1 solo se había probado con dos de los cuatro (caminata ~5 km/h y bici ~40 km/h, más una bajada). Trote (~9 km/h) y carrera (~14 km/h) caían justo en el hueco no cubierto — y es un hueco que importa, porque el suelo de ruido se compara contra lo que avanzas entre lecturas y esos dos ritmos están entre 5 y 8 m por lectura.

- **Fallo real encontrado al añadir la cobertura:** con señal mala (precisión 18 m, ruido ±15 m) se **descartaban lecturas buenas como saltos imposibles** en los tres ritmos de bici. La distancia seguía saliendo bien (0,8-3,0 %), así que una prueba que solo mirara la distancia lo habría dado por bueno; lo que se estaba perdiendo era resolución del trazado, y con algo más de ruido se habría empezado a perder recorrido.
  - **Causa:** la puerta de plausibilidad comparaba la lectura **cruda** contra la última posición **aceptada**, que es una estimación suavizada y por tanto retrasada. Sumando el retraso más el ruido de la lectura, una bajada legítima aparentaba más de 25 m/s. El margen entre el caso real más rápido (55 km/h = 15,3 m/s) y el umbral era de solo 1,6x.
  - **Arreglo 1:** descontar la precisión de la lectura antes de calcular la velocidad, igual que ya se hacía para estimar la velocidad que adapta el suavizado. No se puede declarar imposible un desplazamiento cuya mitad es incertidumbre de medida.
  - **Arreglo 2:** `MAX_PLAUSIBLE_SPEED_MPS` de 25 → **40 m/s** (144 km/h). La puerta existe para cazar teletransportes del GPS, que son dos órdenes de magnitud mayores (el test inyecta 500 m/s); 90 km/h dejaba demasiado poco aire sobre una bajada real. El test del salto salvaje sigue cazándolo sin margen de duda.

- **Cobertura nueva:** los ritmos pasan a ser una tabla (`TravelPace`) recorrida por dos tests, en vez de casos sueltos. Los tests reportan **todos** los ritmos que fallan, no solo el primero, así que una regresión se ve entera de una vez.
  - `theDistanceIsTrueAtEveryPaceTheAppIsUsedAt` — señal típica (precisión 8 m), 4 min por ritmo.
  - `aPoorSignalCostsPrecisionNotTheJourney` — fixes bastos (precisión 18 m, ruido ±15 m), 10 min por ritmo. Es el caso donde más sufre el paso corto, porque el suelo de ruido escala con la precisión y la zancada del que camina es la primera en quedar por debajo.

- **Archivos tocados:**
  - core/commonMain: `filter/LocationFilter.kt` (puerta de plausibilidad)
  - core/commonTest: `filter/TravelPace.kt` (nuevo), `filter/LocationFilterTest.kt` (tres tests de ritmo sueltos → dos dirigidos por tabla)
  - features: `movement_location_smoothing.feature` (dos `Scenario Outline` con los cuatro modos)

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`

- **Resultados de validación:** `:core` 29 tests, 0 fallos; `:app:shared:jvmTest` 69 tests, 0 fallos; `assembleDebug`, `:app:desktopApp:check` y `:app:webApp:check` BUILD SUCCESSFUL. (El total de `core` baja de 30 a 29 porque tres tests de ritmo se refunden en dos que cubren seis ritmos cada uno.)

  Recorrido real → grabado, por ritmo:

  | Ritmo | Señal típica (4 min) | Señal mala (10 min) |
  |---|---|---|
  | Caminata (5 km/h) | 330,6 → 333,6 m (0,9 %) | 830,6 → 835,9 m (0,6 %) |
  | Trote (9 km/h) | 595,0 → 603,4 m (1,4 %) | 1495,0 → 1538,6 m (2,9 %) |
  | Carrera (14 km/h) | 925,6 → 938,4 m (1,4 %) | 2325,6 → 2400,5 m (3,2 %) |
  | Bici (25 km/h) | 1652,8 → 1678,0 m (1,5 %) | 4152,8 → 4293,2 m (3,4 %) |
  | Bici rápida (40 km/h) | 2644,4 → 2673,5 m (1,1 %) | 6644,4 → 6852,1 m (3,1 %) |
  | Bajada (55 km/h) | 3636,1 → 3662,6 m (0,7 %) | 9136,1 → 9383,3 m (2,7 %) |

  Ningún ritmo pasa del 1,5 % con señal típica ni del 3,4 % con señal mala, y ninguna lectura legítima se descarta ya como salto. Las cotas de los tests (5 % y 10 %) dejan aire deliberado sobre lo medido.

- **Nota sobre el método:** este fallo salió de ampliar la cobertura a los casos que el usuario nombró, no de una revisión del código. Las dos actividades que faltaban no fallaron —fue la bici con señal mala la que destapó el problema—, pero sin la tabla de ritmos completa nadie lo habría mirado.

- **Sigue pendiente:** validación en dispositivo. El ruido sintético es uniforme e independiente entre lecturas; el real está correlacionado. Los porcentajes de arriba son una cota de confianza sobre el diseño, no una medición de campo.

**Recap:** El filtro está ahora probado en los cuatro modos que contempla el pilar —caminata, trote, carrera y bici, más la bajada rápida— con señal buena y mala, y la ampliación destapó que la puerta de saltos imposibles descartaba lecturas legítimas de ciclista con señal basta. Corregida descontando la incertidumbre de la medida y subiendo el umbral a 144 km/h, ningún ritmo pierde lecturas y el desvío máximo es del 3,4 % en el peor caso.

**Próximos pasos (opciones):** (1) validar en dispositivo a pie y en bici antes de seguir; (2) B2: tests de `CalculateStatsUseCase` y métricas en el detalle — ojo, sus umbrales `dist > 0.5` y `dist > 2.0` son constantes métricas y tienen exactamente el problema que se acaba de evitar aquí; (3) deuda de migraciones SQLDelight antes de las tablas de rutas de C.

---

## Slice 9 (migración B2a) — Que las estadísticas de la sesión sean ciertas

- **Objetivo:** corregir el cálculo de estadísticas antes de mostrar ninguna métrica más. `CalculateStatsUseCase` corría en producción **sin un solo test**, acumulaba desnivel sobre altitud cruda y decidía qué era movimiento con umbrales en metros fijos. Spec: [`movement_session_statistics.feature`](../../features/movement_session_statistics.feature) (7 escenarios). **B2b** (mostrar las métricas en el detalle) queda pendiente.

- **Decisiones + porqué:**
  - **Orden invertido respecto al backlog.** El punto B decía "revisar el caso de uso y mostrar las métricas". Al analizarlo salió que la pantalla de detalle **ya muestra** "Desnivel" = `stats.totalAscent`, acumulado sumando cada diferencia de altitud positiva sobre la altitud cruda del GPS. Es decir, no era una métrica pendiente de enseñar: era una métrica falsa ya enseñada. Mostrar más encima habría multiplicado el problema.
  - **Un fallo que introdujo B1 sin querer.** `movingTime` contaba un tramo como movimiento si `dist > 0.5`. Desde B1 el filtro descarta las lecturas de cuando estás parado, así que una pausa de cinco minutos ya no produce muchos segmentos cortos sino **un único segmento larguísimo** entre el punto anterior y el posterior, que supera de sobra los 0,5 m: la pausa entera contaba como tiempo en movimiento. Ahora el criterio es **velocidad** (≥ 0,5 m/s), no distancia — coherente con la regla de B1 de que ningún umbral sea una constante métrica, y además inmune a que cambie el muestreo.
  - **Suavizado de altitud en el filtro, histéresis en el caso de uso.** Son dos responsabilidades distintas: suavizar es una regla de señal (va con el resto del suavizado, en `KalmanFilter`, para que los puntos guardados lleven la altitud corregida y trazado/distancia/estadísticas sigan contando la misma historia, decisión de B1); acumular es una regla de negocio (va en `Elevation`, un valor propio y probable aislado).
  - **La altitud NO se suaviza de forma adaptativa.** El truco de B1 —estimar la velocidad descontando la precisión— no funciona en vertical: una subida real avanza decenas de centímetros entre lecturas mientras el error vertical se mide en decenas de metros, así que no hay tasa vertical que detectar por lectura. Se usa un ruido de proceso fijo.
  - **Precisión vertical derivada de la horizontal (×2).** El proveedor no entrega una precisión vertical aparte; la geometría satelital sitúa al receptor a un lado del cielo en vez de rodearlo, así que el error vertical de un fix ronda el doble del horizontal.
  - **`Elevation` con histéresis:** se guarda una altitud de referencia y solo cuenta un cambio que supere el umbral, momento en que la referencia se mueve con él. Una subida larga y sostenida suma su altura real; el ruido alrededor de una misma altitud no suma nada. El umbral es **parámetro, no constante**: pertenece a la calidad del fix que reportó la altitud, no al terreno.
  - **Una sesión de un solo punto devuelve `SessionStats()`.** Antes fijaba `maxAltitude`/`minAltitude` a la altitud de ese punto: una sesión que no fue a ninguna parte se reportaba con una altitud real y cero desnivel, indistinguible de una sesión de verdad.
  - **`altitudeDistribution`:** se conserva el cálculo (nadie lo lee, pero quitarlo tocaría el modelo y el mapper) y se corrige su bucketing, que truncaba hacia cero y metía las altitudes negativas en la franja equivocada.

- **Ajuste durante la implementación (dos tests en rojo lo destaparon):** el primer suavizado vertical era **demasiado agresivo**. Con `VERTICAL_DRIFT_MPS = 0.3` la altitud estimada iba ~28 m por detrás de la real en una subida: la cima se medía más baja y, en la ida y vuelta, el descenso arrancaba truncado y perdía el **36 %**. Se aflojó a `1.0` y se documentó el constante por lo que realmente equilibra (retraso contra ruido), no como "tasa máxima de ascenso plausible", que era una justificación falsa. Rechazar el ruido es trabajo de la histéresis, aguas abajo; el suavizado solo quita el filo de cada lectura.

- **Archivos tocados:**
  - core/commonMain: `filter/KalmanFilter.kt` (altitud + `verticalAccuracy()`), `model/Elevation.kt` (nuevo), `usecase/CalculateStatsUseCase.kt` (reescrito)
  - core/commonTest: `usecase/CalculateStatsUseCaseTest.kt` (nuevo, 7 tests), `filter/GpsTraces.kt` (perfil de altitud, ruido vertical y `naiveAscentMeters()`)
  - features: `movement_session_statistics.feature`

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`

- **Resultados de validación:** `:core` 36 tests, 0 fallos (antes 29). `:app:shared:jvmTest` 69 tests, 0 fallos. `assembleDebug`, `:app:desktopApp:check` y `:app:webApp:check` BUILD SUCCESSFUL.

  | Caso | Real | Antes (sumando cada subida) | Ahora |
  |---|---|---|---|
  | Llano, 10 min, ruido de altitud ±15 m | 0 m | **1450,2 m** de ascenso | **0,0 m** (descenso 13,3 m) |
  | Subida sostenida de 119,6 m | 119,6 m | — | 108,3 m (−9 %) |
  | Ida y vuelta a una loma de 99,5 m | 99,5 ↑ / 99,5 ↓ | — | 103,9 ↑ (+4 %) / 86,5 ↓ (−13 %) |
  | Desnivel entre punto más bajo y más alto | 119,2 m | — | 112,3 m (−6 %) |
  | Tiempo en movimiento con pausa de 5 min | 360 s de 660 s | 660 s (la pausa contaba) | 348 s (−3 %) |

  El titular es el llano: **1450 m de ascenso inventado → 0 m**. El test compara contra la acumulación ingenua para que no pueda pasar en verde con una traza de altitud que no sea ruidosa.

- **Desviaciones:**
  - El test de altitud máxima/mínima estaba mal planteado: comparaba 2119 m contra 2091 m con tolerancia porcentual, y un 1 % de 2119 m son 21 m — la tolerancia no medía nada. Se cambió a comparar el **desnivel entre el punto más bajo y el más alto**, que es la cantidad con significado.
  - `movingTime` no estaba en el alcance del `.feature` original de B1 pero su fallo lo causó B1, así que se arregla aquí.

- **Sesgo conocido y no resuelto:** el desnivel sale **entre un 4 % y un 13 % corto** en subidas reales. Son las dos mitades del mismo compromiso: la histéresis se come el último tramo por debajo del umbral, y el suavizado llega con algo de retraso a los cambios de pendiente. Preferible a inventar: quedarse corto por un 10 % es un error acotado; sumar cada subida daba 1450 m sobre terreno llano.

- **Sin cobertura de host (dicho explícitamente):** el ruido de altitud sintético es uniforme e independiente entre lecturas. El error vertical real del GPS está **fuertemente correlacionado** (deriva lenta con la geometría de los satélites, no salto a salto), y la histéresis se comporta distinto frente a una deriva lenta que frente a ruido blanco. **Este caso es menos representativo que el horizontal de B1**, así que la validación en dispositivo importa más aquí: grabar una ruta de desnivel conocido (una cuesta medida, o comparar con una app de referencia) a pie y en bici.

- **Seguimientos:**
  - **B2b:** mostrar en el detalle tiempo en movimiento, altitud máx/mín y descenso, con "—" para las sesiones viejas. El escenario "una sesión sin nada grabado no tiene estadísticas" está resuelto en el dominio pero **su mitad de pantalla es de B2b**.
  - **Calibrar en dispositivo** `VERTICAL_DRIFT_MPS`, el factor de histéresis y `MIN_TRAVELLING_SPEED_MPS`.
  - `currentPace` y `currentSlope` son métricas de sesión en vivo que se calculan sobre una sesión terminada y se tiran (no se persisten). Restos del caso de uso de navegación de la referencia; candidatos a borrado.
  - `altitudeDistribution` sigue sin consumidor: decidir en B2b si se muestra o se borra del modelo.
  - Las sesiones ya grabadas conservan su desnivel inflado; no se recalculan.

**Recap:** Las estadísticas de una sesión terminada pasan a calcularse sobre altitud suavizada, acumulando desnivel con histéresis contra la calidad del fix y contando como movimiento por velocidad en vez de por distancia. Sobre trazas sintéticas, un paseo llano deja de reportar 1450 m de ascenso inventado y reporta 0, mientras una subida real conserva su altura con un sesgo conocido del 4-13 % por defecto. De paso se corrigió que, desde B1, las pausas contaran enteras como tiempo en movimiento.

**Próximos pasos (opciones):** (1) validar en dispositivo con una ruta de desnivel conocido — importa más aquí que en B1 porque el error vertical real está correlacionado y el ruido sintético no lo está; (2) B2b: mostrar las métricas en el detalle; (3) deuda de migraciones SQLDelight antes de las tablas de rutas de C.

---

## Session handoff — 2026-07-25 (reinicio de la máquina)

Estado al cerrar la sesión, tras entregar los slices 8 (B1) y 9 (B2a). **Nada está commiteado.**

### Estado de git

- Rama: `feat/movement-tracker-screen` (último commit: `f4badb8 feat(movement): record sessions in a foreground service`).
- Un reinicio **no pierde** estos cambios: están escritos en disco, solo sin commitear. Al volver, `git status` debe mostrar exactamente esto:

**Modificados**
- `core/.../filter/KalmanFilter.kt` — reescrito inmutable, suavizado horizontal adaptativo + vertical
- `core/.../usecase/CalculateStatsUseCase.kt` — reescrito
- `core/.../usecase/TrackNavigationUseCase.kt` — apuntado a `LocationFilter` (era código muerto que no compilaba con la API nueva)
- `app/shared/.../data/movement/SessionRecording.kt` — filtra antes de acumular
- `app/shared/.../data/movement/SessionRecordingTest.kt` — 2 tests nuevos + helper de coordenadas
- `docs/features/movement-tracker-bitacora.md`, `docs/features/movement-tracking-migration.md`
- `CLAUDE.md` (reglas de terminal añadidas por el usuario), `.claude/settings.json`

**Nuevos**
- `core/.../filter/LocationFilter.kt`
- `core/.../model/Elevation.kt`
- `core/src/commonTest/.../filter/` (`LocationFilterTest.kt`, `GpsTraces.kt`, `TravelPace.kt`)
- `core/src/commonTest/.../usecase/CalculateStatsUseCaseTest.kt`
- `features/movement_location_smoothing.feature`, `features/movement_session_statistics.feature`

### Validación en el momento de cerrar

Todo en verde:

| Comando | Resultado |
|---|---|
| `.\gradlew.bat :core:jvmTest` | 36 tests, 0 fallos |
| `.\gradlew.bat :app:shared:jvmTest` | 69 tests, 0 fallos |
| `.\gradlew.bat :app:androidApp:assembleDebug` | BUILD SUCCESSFUL |
| `.\gradlew.bat :app:desktopApp:check` | BUILD SUCCESSFUL |
| `.\gradlew.bat :app:webApp:check` | BUILD SUCCESSFUL |

Nota: `assembleDebug` y `:app:webApp:check` **chocan si se lanzan en la misma invocación de Gradle** (`copySharedComposeResourcesToAssets`, conflicto por los mismos recursos compartidos). Lanzarlos por separado.

### Acciones pendientes

**1. Decidir el commit (pendiente del usuario).** No se ha commiteado nada por no haberlo pedido. Son dos slices independientes y conviene que sean dos commits:
   - `feat(movement): filter gps readings before accumulating distance` — slice 8
   - `feat(movement): compute session statistics from smoothed altitude` — slice 9

   `CLAUDE.md` y `.claude/settings.json` son cambios del usuario, ajenos a los slices: van aparte o se dejan fuera.

**2. Validación en dispositivo — es la pendiente de verdad, y bloquea la confianza en ambos slices.** Ninguno está validado contra un GPS real; las constantes se calibraron contra ruido sintético.
   - **B1 (posición):** dejar el teléfono quieto varios minutos y comprobar que la distancia no crece; grabar un trayecto conocido a pie y en bici y comparar el total.
   - **B2a (altitud):** grabar una ruta de **desnivel conocido**, a pie y en bici. **Esta importa más**: el ruido de altitud sintético es uniforme e independiente entre lecturas, mientras que el error vertical real del GPS está fuertemente correlacionado (deriva lenta con la geometría satelital). La histéresis se comporta distinto ante una deriva lenta que ante ruido blanco, así que los números del slice 9 son una cota de confianza sobre el diseño, no una predicción de campo.
   - Constantes a recalibrar con lo que salga: `VERTICAL_DRIFT_MPS`, el factor de histéresis (`ELEVATION_THRESHOLD_FACTOR`), `MIN_TRAVELLING_SPEED_MPS`, `MAX_PLAUSIBLE_SPEED_MPS`, `MAX_USABLE_ACCURACY_METERS`.

**3. Siguiente slice: B2b** — mostrar en el detalle tiempo en movimiento, altitud máx/mín y descenso. Incluye la mitad de pantalla del escenario "una sesión sin nada grabado no tiene estadísticas": el dominio ya devuelve `SessionStats()` vacío, falta que la UI muestre "—" en vez de ceros. Requiere alignment gate y `.feature` propios.

**4. Deuda y limpieza anotadas, sin urgencia:**
   - Sesiones ya grabadas conservan distancia inflada y desnivel inflado; no se recalculan. Decidir si se marcan de algún modo.
   - `currentPace` y `currentSlope` se calculan sobre una sesión terminada y se tiran (no se persisten). Restos del caso de uso de navegación de la referencia; candidatos a borrado.
   - `altitudeDistribution` sigue sin consumidor: decidir en B2b si se muestra o se borra del modelo.
   - Sesgo conocido: el desnivel sale un 4-13 % corto en subidas reales. Aceptado a cambio de no inventar.
   - Muestreo a 2 s: en curvas cerradas a velocidad de bici se cortan las curvas y la distancia se queda corta. Se arreglaría muestreando más, a costa de batería.
   - Deuda de migraciones SQLDelight, pendiente antes de las tablas nuevas de C.

**Recap:** El pilar Movimiento tiene ahora la señal filtrada antes de acumular distancia (slice 8) y las estadísticas calculadas sobre altitud suavizada con histéresis (slice 9), ambos cubiertos por tests en `core` y con toda la validación de host en verde. Lo que falta no es código: es contrastar ambos contra un GPS real, porque el ruido sintético con el que se calibraron no reproduce la correlación del error real — especialmente en vertical.

**Próximos pasos (opciones):** (1) commitear los dos slices por separado; (2) validar en dispositivo, primero el desnivel con una ruta conocida; (3) B2b, mostrar las métricas en el detalle.

---

## Slice 10 — Capturar la traza cruda del GPS para poder calibrar

- **Objetivo:** que una salida real se pueda repetir en frío. Hasta ahora una sesión guardaba **solo las lecturas aceptadas y ya suavizadas**, así que una salida al campo podía decir que la distancia se desvió pero nunca por qué, y ninguna constante se podía reajustar sin volver a salir. Spec: [`movement_trace_capture.feature`](../../features/movement_trace_capture.feature) (9 escenarios).
- **Encuadre:** salió de la pregunta del usuario sobre cómo probar los slices 8 y 9 con un GPS real. Se ofrecieron dos slices —este y B2b (métricas en el detalle)— y se eligió este primero por un motivo asimétrico: **desde una traza cruda se recalcula todo lo de B2b en frío, pero desde la pantalla de B2b no se recupera ni una lectura descartada.** Si solo uno llega listo a la primera salida, tiene que ser este.

- **Decisiones + porqué:**
  - **CSV en disco, no en la base de datos.** El diagnóstico es por sesión, así que lo natural habría sido **columnas nuevas en `MovementSessionEntity`** — y añadir una columna a una tabla existente es exactamente lo que el parche `ensureNewTablesExist` **no** sabe hacer: la guarda es `if (!hasTable)`, se salta la tabla ya creada y la app revienta en tiempo de ejecución solo en instalaciones previas. La otra opción, tabla nueva, funcionaba a costa de un tercer bloque de `CREATE TABLE` duplicado a mano. Un fichero esquiva las dos cosas y además es lo que se puede sacar del teléfono y reproducir, que es para lo que existe. La deuda de migraciones queda anotada, sin tocarla.
  - **Se guarda la lectura CRUDA, no la corregida.** Es lo que hace la traza reproducible: volver a pasarla por el filtro reconstruye el trazado suavizado. Guardar la corregida la suavizaría dos veces en cada reproducción. Hay un test que falla si esa diferencia desaparece.
  - **Se guardan también las lecturas descartadas, y esa es la mitad que importa.** Una lectura rechazada por `WITHIN_NOISE` **sí** alimenta al suavizador, así que quitarla cambia todas las estimaciones posteriores. El trazado persistido de una sesión es precisamente una traza sin ellas: por eso la captura no podía resolverse leyendo la base de datos. Está fijado en `aTraceThatKeptOnlyTheAcceptedReadingsWouldNotReproduceAnything`.
  - **Casado traza↔sesión por el timestamp del primer punto guardado.** `MovementSessionEntity.date` no sirve: `SaveSessionUseCase` lo fija al **detener**, no al iniciar. El primer punto almacenado es la misma lectura que el filtro aceptó primero, y `KalmanFilter` conserva el `timestamp` original al corregir la posición, así que es una clave exacta y no cuesta ni una columna. El fichero se escribe como `partial-<inicio>.csv` y se renombra a `trace-<primerPunto>.csv` al guardar la sesión.
  - **Escritura incremental, con `flush` por lectura.** Una línea corta cada dos segundos no cuesta nada, y es lo único que hace que la traza de una grabación que el sistema mató valga algo — que es justamente la salida con la pantalla apagada. Va por un `Channel` sin límite hacia `Dispatchers.IO`, así que nunca bloquea el hilo que colecta ubicaciones (en Android, el principal del servicio).
  - **El parseo es deliberadamente indulgente.** Una traza cortada termina a media línea; se descarta esa línea y se lee el resto. Un veredicto desconocido (traza escrita por una versión futura) también se salta en vez de adivinarse como aceptado, que inventaría un diagnóstico.
  - **La captura es un argumento de `startRecording(captureTrace)`, no un ajuste.** Pertenece a la grabación que estás iniciando, no a la app. Evita estado global nuevo y viaja al servicio como extra del `Intent`. El interruptor vive en `TrackerViewModel` y solo se ofrece con la grabación parada: un switch que no hace nada a mitad de sesión sería mentira.
  - **Apagado por defecto.** Una app en uso normal no tiene por qué escribir un fichero por sesión. Una sesión sin traza reporta **"sin diagnóstico"**, no contadores a cero — un cero afirmaría que el filtro no rechazó nada.
  - **El diagnóstico se muestra plegado en el detalle** (elección del usuario), como filas ya formateadas. Se construye como lista de filas y no como campos con nombre para que **una razón que nunca disparó simplemente no aparezca**, en vez de una columna de ceros.
  - **La mediana, no la media, para el intervalo real.** Una sesión con un tramo sin cobertura deja un hueco enorme que arrastraría la media lejos del ritmo al que corrió el resto.
  - **Reparto por módulos:** `TraceRecord`, `TraceSummary` y el resumen viven en `core` (son dominio puro y probable); el CSV y el `TraceStore` viven en `app/shared` porque son E/S. Solo Android escribe traza; jvm/js/ios usan `NoOpTraceStore`.

- **Archivos tocados:**
  - core/commonMain: `filter/TraceRecord.kt` (nuevo), `filter/TraceSummary.kt` (nuevo), `repository/RecordingController.kt` (`startRecording(captureTrace)`)
  - core/commonTest: `filter/TraceSummaryTest.kt` (nuevo, 7), `filter/TraceReplayTest.kt` (nuevo, 5), `filter/GpsTraces.kt` (`tracedThrough()`)
  - shared/commonMain: `data/movement/trace/TraceFormat.kt`, `TraceStore.kt`, `TraceStoreFactory.kt` (nuevos), `data/movement/SessionRecording.kt`, `InProcessRecordingController.kt`, `feature/movement/tracker/presentation/TrackerViewModel.kt`, `tracker/ui/TrackerScreen.kt`, `feature/movement/detail/presentation/SessionDiagnosis.kt` (nuevo), `SessionDetailViewModel.kt`, `detail/ui/SessionDetailScreen.kt`, `presentation/MovementFormat.kt` (`oneDecimal`)
  - shared/androidMain: `data/movement/trace/FileTraceStore.kt`, `TraceStoreFactory.android.kt` (nuevos), `MovementRecordingService.kt`, `RecordingControllerFactory.android.kt`
  - shared/{jvm,js,ios}Main: `data/movement/trace/TraceStoreFactory.<target>.kt`
  - shared/commonTest: `data/movement/trace/FakeTraceStore.kt`, `TraceFormatTest.kt` (nuevos), `SessionRecordingTest.kt` (+5), `feature/movement/detail/presentation/SessionDetailViewModelTest.kt` (+2), `tracker/presentation/TrackerViewModelTest.kt` (+2)
  - features: `movement_trace_capture.feature`

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :core:check`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`

- **Resultados de validación:** `:core` **48 tests, 0 fallos** (antes 36). `:app:shared:jvmTest` **84 tests, 0 fallos** (antes 69). `:core:check`, `assembleDebug`, `:app:desktopApp:check` y `:app:webApp:check` BUILD SUCCESSFUL. Test exterior = `TraceReplayTest` (una traza reproduce su sesión y responde a un cambio de umbral); componentes = `TraceSummaryTest`, `TraceFormatTest`, y los añadidos de `SessionRecordingTest`.

- **Desviaciones:** ninguna respecto al `.feature`. Un cambio de alcance menor: `stop()` ya no puede salir antes de tiempo cuando la sesión no grabó nada, porque la traza hay que cerrarla igual — antes devolvía `null` sin más. El comportamiento visible se conserva (`doesNotPersistASessionWithoutAnyLocation` sigue en verde) y hay un test nuevo que fija que una grabación sin sesión **sí** cierra su traza y **no** la ata a ninguna sesión.

- **Sin cobertura de host (dicho explícitamente):** `FileTraceStore` —el renombrado, el `flush` por lectura, el directorio en almacenamiento externo— no se prueba en JVM: es código Android. Lo que sí está probado es todo lo que decide *qué* se escribe y *cómo* se lee. La prueba de que el fichero sobrevive a que el sistema mate el proceso es manual: `adb shell am kill com.hazlosano` a mitad de una grabación con la captura encendida, y comprobar que el `partial-*.csv` conserva las lecturas.

- **Cómo usarlo en campo:**
  1. Tracker → activar **"Guardar traza de diagnóstico"** → Iniciar.
  2. Al terminar, el detalle de esa sesión muestra **Diagnóstico** (plegado): lecturas recibidas, aceptadas y descartadas con su reparto por motivo, precisión media e intervalo real.
  3. Sacar el fichero: `adb pull /sdcard/Android/data/com.hazlosano/files/traces/ .` — sin `run-as`.
  4. **Sacar las trazas después de cada salida.** Desinstalar la app borra ese directorio, y desinstalar es la salida barata a la deuda de migraciones.

- **Seguimientos:**
  - **B2b:** mostrar en el detalle tiempo en movimiento, altitud máx/mín y descenso. No necesita esquema nuevo: `MovementSessionMapping` ya los lee de vuelta.
  - Reproducir en un test de JVM las trazas reales que salgan de la calibración y ajustar `VERTICAL_DRIFT_MPS`, `ELEVATION_THRESHOLD_FACTOR`, `MIN_TRAVELLING_SPEED_MPS`, `MAX_PLAUSIBLE_SPEED_MPS` y `MAX_USABLE_ACCURACY_METERS`.
  - Los `partial-*.csv` de grabaciones interrumpidas no se limpian solos; se borran a mano.
  - Deuda de migraciones SQLDelight, intacta y anotada: **mientras siga viva, cualquier columna nueva sobre una tabla existente rompe la app en instalaciones previas sin avisar en compilación.**

**Recap:** Una grabación puede ahora guardar cada lectura que entregó el receptor con el veredicto que le dio el filtro, en un CSV que se saca del teléfono sin permisos especiales y se vuelve a pasar por el filtro en un test. La sesión encuentra su propia traza por el timestamp de su primer punto guardado, sin tocar el esquema, y el detalle la resume plegada. Está apagado por defecto y una sesión sin traza lo dice en vez de enseñar ceros. Con esto, calibrar deja de costar una salida por iteración.

**Próximos pasos (opciones):** (1) salir a calibrar — primero teléfono quieto, luego trayecto conocido a pie y en bici, luego cuesta ida y vuelta; (2) B2b, métricas en el detalle; (3) commitear los slices 8, 9 y 10 por separado.

---

## Slice 11 (B2b, replanteado) — Las cifras se derivan del recorrido, y se paga la deuda de migraciones

- **Objetivo:** que mejorar cómo mide la app mejore también las salidas ya grabadas, y que el detalle enseñe todo lo que la sesión mide. Spec: [`movement_session_metrics.feature`](../../features/movement_session_metrics.feature) (9 escenarios).

- **Cómo cambió el encuadre (importa más que el resultado):** el slice empezó siendo "B2b: mostrar tiempo en movimiento, altitud máx/mín y descenso". Al proponer de paso borrar `altitudeDistribution`, `currentPace` y `currentSlope`, el usuario preguntó por qué se borraba algo potencialmente valioso. Al mirarlo de cerca los tres **no eran el mismo caso**, y esa pregunta destapó el problema de fondo:
  - `currentPace` es el ritmo de los **últimos 5 puntos** de una sesión terminada: los ~8 s en los que frenabas para pulsar Detener. `currentSlope` es peor — solo se asigna en el último segmento y solo si pasa la puerta de `isTravelling`, así que **casi siempre valía `0.0`**, que se lee como "llano". No eran métricas sin consumidor: eran métricas **falsas**, restos del caso de uso de navegación en vivo de la referencia.
  - `altitudeDistribution` sí era valioso — pero **no estaba persistido**, así que se calculaba al detener y se tiraba. No podía llegar a pantalla nunca.
  - Y de ahí lo que de verdad importaba: **todas** las columnas de estadísticas (`movingTime`, `avgPace`, `maxAltitude`, `minAltitude`, `totalAscent`, `totalDescent`) y también `distanceTraveled` son **funciones puras de los puntos guardados**. No eran datos: eran una **caché**. Una caché congelada en la versión del algoritmo que la escribió, que es exactamente por qué los slices 8 y 9 tuvieron que anotar "las sesiones ya grabadas conservan sus números inflados; no se recalculan".
  - Al preguntarle si prefería evitar la migración, el usuario respondió que la migración no era obstáculo y que lo importante era que el código quedara flexible **antes de tener usuarios reales**. Eso desbloqueó la opción correcta.

- **Decisiones + porqué:**
  - **Las cifras se derivan al leer.** `GetSessionDetailUseCase` calcula `SessionStats` desde los puntos cada vez que se abre una sesión. Consecuencia inmediata y medible: retocar una constante durante la calibración actualiza **todas** las salidas anteriores, así que se pueden comparar entre sí. Sin esto, cada ronda de ajuste habría dejado obsoletos los datos de campo del día anterior — y esa fase empieza ahora.
  - **Y consecuencia a futuro:** una métrica nueva (pendiente media, VAM, histograma de altitud) deja de costar una migración. Se añade una función y funciona sobre todo el historial.
  - **`elapsedTime` y `date` siguen guardados.** El tiempo transcurrido es reloj de pared de Iniciar a Detener e incluye los tramos sin cobertura antes del primer fix y después del último: **no es derivable del recorrido**, y derivarlo habría perdido información.
  - **`distanceTraveled` se queda como resumen, y se cura al abrir.** El historial no puede leer todos los puntos de todas las sesiones para pintar una lista — con usuarios reales son decenas de miles de filas por apertura. Así que la lista lee un resumen guardado, y `RefreshSessionSummaryUseCase` lo reescribe cuando el detalle mide algo distinto. Abrir una sesión la cura. Límite asumido y anotado: una sesión que nunca abras conserva su resumen viejo en la lista.
  - **Guarda explícita contra borrar datos:** curar **no** se aplica a una sesión sin puntos. Medir un recorrido vacío da 0, que es la ausencia de recorrido y no un viaje de longitud cero; escribirlo habría destruido el único registro de cuánto anduvo. Tiene su test.
  - **`null` significa "no medido", nunca cero.** Todos los campos de `SessionStats` son nullable. Una sesión cuyas lecturas no traían altitud no tiene desnivel — reportar 0 afirmaría terreno llano, que es una afirmación que la grabación nunca hizo. La UI muestra "—".
  - **`MovementSession` adelgaza** a lo que el historial necesita (id, nombre, fecha, tiempo, distancia, preview). Las cifras viven en `SessionDetail`.
  - **`SessionRecording` ya no calcula estadísticas al detener** — desaparece su dependencia de `CalculateStatsUseCase`. Guardar es guardar puntos y el resumen.
  - **Rejilla de 4×2 en el detalle.** Ocho cifras en una fila en un móvil no se leen; y el mapa sigue necesitando sitio.

- **La migración (deuda del slice 4, saldada):**
  - Se activan las migraciones de SQLDelight con `1.sqm` (versión 1 → 2) y **se borra `ensureNewTablesExist`**, el parche que creaba tablas a mano al arrancar porque `AndroidSqliteDriver` solo ejecuta `Schema.create()` cuando el fichero no existe. Ese parche solo sabía añadir tablas enteras: cambiar una existente era imposible, y habría roto una instalación previa en tiempo de ejecución sin fallar en compilación.
  - `1.sqm` crea las tablas que el parche creaba (para bases que lo preceden) y reconstruye `MovementSessionEntity` sin las columnas derivadas.
  - **El riesgo real y cómo se cerró:** reconstruir la tabla implica un `DROP TABLE`, y `MovementPointEntity` la referencia con `ON DELETE CASCADE`. Con claves foráneas activas, el `DELETE FROM` implícito de SQLite **se habría llevado todos los puntos grabados** — una pérdida de datos que solo aparecería en un teléfono real, después, con las lecturas ya perdidas. Se comprobó que el proyecto no activa `PRAGMA foreign_keys` en ningún sitio y que ningún driver lo hace por defecto; pero eso no se dejó como razonamiento: `MovementSessionMigrationTest` monta una base v1 con una sesión y su recorrido, aplica la migración y **afirma que los tres puntos siguen ahí**.
  - No se usa `ALTER TABLE ... DROP COLUMN` porque exige SQLite 3.35+ (Android 14+).

- **Archivos tocados:**
  - core/commonMain: `model/SessionStats.kt` (nullable, sin `currentPace`/`currentSlope`/`altitudeDistribution`), `model/MovementSession.kt` (adelgazado), `model/SessionDetail.kt` (lleva `stats` y `distanceMeters`), `usecase/CalculateStatsUseCase.kt`, `usecase/GetSessionDetailUseCase.kt`, `usecase/SaveSessionUseCase.kt`, `usecase/RefreshSessionSummaryUseCase.kt` (nuevo), `repository/MovementSessionRepository.kt`
  - core/commonTest: `usecase/RefreshSessionSummaryUseCaseTest.kt` (nuevo, 4), `CalculateStatsUseCaseTest.kt` (+1), `GetSessionDetailUseCaseTest.kt` (+2)
  - shared/commonMain sqldelight: `MovementSession.sq` (sin columnas derivadas, `updateDistance`), `1.sqm` (nuevo)
  - shared/androidMain: `data/db/DriverFactory.android.kt` (de 125 líneas a 16)
  - shared/commonMain: `data/movement/MovementSessionMapping.kt`, `SqlDelightMovementSessionRepository.kt`, `NoOpMovementSessionRepository.kt`, `SessionRecording.kt`, `feature/movement/detail/presentation/SessionDetailViewModel.kt`, `SessionDetailViewModelFactory.kt`, `detail/ui/SessionDetailScreen.kt`, `presentation/MovementFormat.kt`
  - shared/jvmTest: `data/db/MovementSessionMigrationTest.kt` (nuevo, 4), `data/db/InMemoryHazloSanoDatabase.kt`, `SqlDelightMovementSessionRepositoryTest.kt` (+1), `SessionDetailIntegrationTest.kt` (+1)
  - shared/commonTest: `SessionDetailViewModelTest.kt` (reescrito, +4), más ajustes en los stubs de repositorio
  - features: `movement_session_metrics.feature`

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :core:check`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`

- **Resultados de validación:** `:core` **55 tests, 0 fallos** (antes 48). `:app:shared:jvmTest` **94 tests, 0 fallos** (antes 84). `:core:check`, `assembleDebug`, `:app:desktopApp:check` y `:app:webApp:check` BUILD SUCCESSFUL.

- **Desviaciones:** tres expectativas de test mal calculadas por mí (distancia haversine real 1111,9 m donde asumí 1113,2, y 390 m donde puse 339). Eran errores de aritmética en las aserciones, no del código; corregidas contra el valor medido.

- **Sin cobertura de host (dicho explícitamente):** la migración se prueba sobre SQLite en memoria vía JDBC, no sobre `AndroidSqliteDriver` en un teléfono. Lo que no cubre el test es que el `user_version` del dispositivo sea realmente 1 — si por lo que sea no lo fuera, la migración no se dispararía. **Comprobación manual al instalar:** abrir el historial y verificar que las sesiones anteriores siguen ahí con su recorrido. Si algo saliera mal, la salida es desinstalar y reinstalar (sin usuarios, coste asumido).

- **Seguimientos:**
  - `avgSlope`, `maxSlope` y `vam` se calculan y no se muestran. Ahora **mostrarlos no cuesta migración**: es añadir dos líneas al ViewModel y a la rejilla.
  - `altitudeDistribution` se borró como campo. El histograma tiempo-por-franja-de-altitud se recalcula desde los puntos guardados cuando se quiera, y funcionará **retroactivamente sobre todo el historial**.
  - Una sesión que nunca se abra conserva su resumen viejo en la lista.
  - Las tablas de rutas del punto C ya no tienen deuda de migraciones delante: se añaden con un `2.sqm`.

**Recap:** Las cifras de una sesión pasan a derivarse de su recorrido cada vez que se abre, en vez de leerse de columnas escritas al detener. El detalle muestra ahora ocho métricas —incluidas tiempo en movimiento, descenso y altitud máx/mín— con "—" donde no se midió nada en vez de ceros que afirman terreno llano. La deuda de migraciones queda saldada con la primera migración real del proyecto, cuyo riesgo de borrado en cascada del recorrido está cerrado con un test y no con un razonamiento. Lo que esto compra de inmediato: la calibración que empieza ahora ya no invalida los datos de campo de las salidas anteriores.

**Próximos pasos (opciones):** (1) instalar, comprobar que el historial sobrevive a la migración, y salir a calibrar; (2) commitear el slice; (3) mostrar pendiente media y VAM, que ya no cuestan migración.

---

## Primera calibración en campo — tres trazas reales

Primera salida con la captura de traza activada. Tres CSV en `traces/`, reproducidos por el filtro real con `TraceReplayHarness` y `TraceDiagnosticsHarness` (ambos en `app/shared/src/jvmTest/`, solo reportan).

### Qué fue cada traza (según el usuario)

| | Traza 1 · 21:34 | Traza 2 · 21:38 | Traza 3 · 21:54 |
|---|---|---|---|
| Actividad | Quieto, dentro de casa | Bajó caminando una pendiente, luego trote/carrera | Bici todo el recorrido |
| Pendientes | — | 2 | 3 (una extra, empinada, subida y bajada) |
| Duración | 3m 11s | 7m 37s | 5m 12s |
| Lecturas | 25 | 214 | 139 |
| Intervalo real | 8,8 s | 2,0 s | 2,0 s |
| Precisión media | 20,4 m | 4,2 m | 6,0 m |
| Distancia cruda | 22 m | 1028 m | 1267 m |
| Distancia grabada | **0 m** | 899 m | 1190 m |

**Geometría verificada:** la traza 3 empieza a 16 m de donde acabó la 2 y termina a 3 m de donde empezó la 2 — es el regreso por la misma ruta. Los 291 m de diferencia (1190 − 899) cuadran con la pendiente extra subida y bajada, así que las distancias **no** son contradictorias. La sospecha inicial de que el paso corto medía corto se retiró por falta de evidencia; sigue sin distancia real medida para cerrarlo del todo.

### Hallazgo 1 — `movingTime` se truncaba (bug del slice 9, corregido aquí)

`movingSeconds += seconds.toLong()` truncaba **cada segmento** a segundos enteros. Con segmentos de ~2,7 s eso tira ~0,7 s cientos de veces por sesión.

| | Suma exacta | Truncando | Perdido |
|---|---|---|---|
| Traza 2 | 409 s | 330 s | 79 s |
| Traza 3 | 282 s | **232 s** | 50 s |

Los 232 s son exactamente lo que la app había reportado para la traza 3, lo que confirmó el diagnóstico sin margen de duda. La pantalla presentaba la diferencia como **una pausa que no ocurrió** — y el usuario llegó a racionalizarla ("tal vez paré 80 s"), que es lo que hace peligroso a un número falso: se cree.

- **Arreglo:** acumular en `Double` y redondear una vez al final.
- **Por qué ningún test lo cazó:** las trazas sintéticas colocaban cada lectura en múltiplos exactos de 2000 ms, así que `.toLong()` no perdía nada. **El bug era invisible por construcción.** Se añadió `intervalJitterMillis` a `trace()` (con su propio `Random`, para no desplazar el ruido de las trazas existentes) y un test que lo usa.
- **Comprobado que el test caza el fallo:** revirtiendo el arreglo, reporta 303 s de 403 s (−25 %), el mismo patrón que la bici real (232 de 312, −26 %).
- **Efecto sobre las trazas reales:** traza 2 de 317 → **395 s** de 457; traza 3 de 232 → **282 s** de 312. Lo que queda sin movimiento en la traza 2 (62 s) son dos tramos genuinamente lentos a 0,33 y 0,39 m/s — el arranque caminando cuesta abajo.

### Hallazgo 2 — La altitud del teléfono se congela (sin resolver)

No es un umbral mal calibrado: **el dato no está**.

| | Altitudes distintas | Racha repetida más larga |
|---|---|---|
| Traza 1 | **1** de 25 | 25 lecturas (192 s) |
| Traza 2 | 111 de 214 | 97 lecturas (**207 s**) |
| Traza 3 | **48** de 139 | 86 lecturas (**193 s**) |

En la traza 3 el 65 % de las lecturas repiten altitud, con 3,2 minutos seguidos en el valor exacto 207,1 — en bici a 13,7 km/h, unos 450 m de terreno. El perfil muestra plano congelado de 60 s a 180 s y solo después la bajada. **Las pendientes que el usuario describe ocurrieron dentro de esa ventana**, y por eso el ascenso reportado es de 5,3 m.

El desglose separa dónde se pierde qué (traza 2, donde la altitud sí varió): crudo +72,8 → suavizado +29,8 → con histéresis +19,8, contra ~24 m reales según el perfil. **Ahí el filtro se comporta bien.** El problema es exclusivamente la señal congelada.

- **Pista concreta sin explotar:** Android expone `getVerticalAccuracyMeters()` y `hasAltitude()`, y **no los leemos** — derivamos la precisión vertical como 2× la horizontal, adivinando. Si el sistema informa de que esa altitud no es fiable, deberíamos capturarlo en la traza y en `UserLocation`, y reportar "—" en vez de un desnivel inventado.
- **Alternativa de fondo:** barómetro (`Sensor.TYPE_PRESSURE`), que es lo que usan las apps de montaña. Slice grande.
- Queda pendiente con gate propio.

### Hallazgo 3 — El ruido sintético era mucho más pesimista que el real

La traza 1 dio 22 m de recorrido crudo en 3 minutos; la traza sintética equivalente asumía **714 m en 2 minutos**. Es la correlación del error real que se avisó en el slice 8, ahora medida: el error deriva despacio, no salta. Buena noticia para la confianza, pero **el titular "714 m → 10 m" exageraba el mérito del filtro**.

Además la traza 1 no fue una prueba válida de GPS parado: desde una habitación, con fix de red (precisión 20 m, muestreo cada 8,8 s, altitud congelada). Los 0 m están bien pero el caso era fácil. **Pendiente repetirla a cielo abierto.**

- **Archivos tocados:** core/commonMain `usecase/CalculateStatsUseCase.kt`; core/commonTest `filter/GpsTraces.kt` (jitter de intervalo), `usecase/CalculateStatsUseCaseTest.kt` (+1); features `movement_session_statistics.feature` (+1 escenario); app/shared/jvmTest `trace/TraceReplayHarness.kt`, `trace/TraceDiagnosticsHarness.kt` (nuevos, solo reportan).
- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`
- **Resultados:** `:core` **56 tests, 0 fallos** (antes 55). `:app:shared:jvmTest` **96 tests, 0 fallos** (antes 94, +2 arneses). `assembleDebug`, desktop y web BUILD SUCCESSFUL.

**Recap:** La primera salida de campo justificó la captura de traza en una tarde: destapó un bug de truncamiento que quitaba una cuarta parte del tiempo en movimiento de toda sesión y que ningún test sintético podía ver, porque las trazas generadas caían en segundos exactos. También mostró que el problema del desnivel no es de calibración sino de señal — la altitud de este teléfono se queda congelada durante minutos — y que el ruido real infla el recorrido mucho menos de lo que modelamos.

**Próximos pasos (opciones):** (1) encuadrar el problema de la altitud, empezando por leer la precisión vertical que Android ya reporta; (2) repetir la prueba de teléfono quieto a cielo abierto y medir la ruta real para cerrar la duda de la distancia caminando; (3) commitear el arreglo del truncamiento.

---

## Slice 12 — Capturar lo que el receptor dice de su propia altitud

- **Objetivo:** dejar de adivinar la calidad de la altitud, para que la siguiente salida decida si una altitud rancia se puede detectar o hay que medirla de otro modo. Spec: [`movement_altitude_quality.feature`](../../features/movement_altitude_quality.feature) (6 escenarios).
- **Alcance deliberadamente corto:** **este slice no descarta nada.** Captura lo que hoy se tira y deja de inventar una precisión vertical que nunca nos dieron. Qué hacer con la altitud congelada se decide con los datos que esto produzca — diseñar el arreglo contra datos que no tenemos es exactamente lo que llevó a calibrar el filtro contra ruido sintético cinco veces más pesimista que el real.

- **Decisiones + porqué:**
  - **`UserLocation.altitude` pasa a `Double?`.** `Location.getAltitude()` de Android devuelve `0.0` **exactamente cuando `hasAltitude()` es false**, y lo leíamos sin preguntar: un fix sin componente vertical se guardaba como una afirmación de estar al nivel del mar. Nullable en vez de un `hasAltitude: Boolean` al lado de un `0.0`, porque el booleano junto al cero es justo la ambigüedad que se quita.
  - **`verticalAccuracy: Float?` nuevo**, leído de `getVerticalAccuracyMeters()` cuando `hasVerticalAccuracy()`. El `usableVerticalAccuracy()` usa el dato real y **cae al 2× horizontal solo como respaldo**, para receptores que no lo reporten. Antes el 2× era la única vía: una suposición razonada, pero suposición.
  - **Una lectura sin altitud no arrastra la estimación vertical** ni recibe la anterior de vuelta. El suavizado deja el estimado intacto y el punto emitido sale sin altitud. Devolverle la última estimación sería vestir una altitud rancia de medición fresca, que es el fallo exacto que este slice existe para dejar de esconder.
  - **`CalculateStatsUseCase` deduce "sin altitud" de la ausencia, no de comparar contra cero.** El efecto lateral bueno: una sesión en la costa deja de reportarse como "sin altitud". Hay un test para esa distinción, que antes era imposible de expresar.
  - **Migración `2.sqm` (versión 2 → 3):** `MovementPointEntity.altitude` pasa a nullable y gana `verticalAccuracy`. Hace falta persistirla porque **el desnivel se deriva de los puntos al abrir la sesión**, y el umbral con el que se acumula sale de la calidad del fix que reportó la altitud; sin guardarla, cada lectura volvería al 2× adivinado y el slice no serviría de nada. Aquí la tabla reconstruida es la **hija** de la clave foránea, así que no hay riesgo de borrado en cascada — al revés que en `1.sqm`.
  - **Las altitudes ya grabadas se dejan como están.** Se capturaron sin preguntar nunca si el fix tenía componente vertical, así que un `0.0` guardado es genuinamente ambiguo. Reinterpretarlo sería inventar historia.
  - **El CSV gana una columna** (`verticalAccuracy`) y la altitud puede ir vacía. El parser acepta 7 columnas (formato viejo) y 8, tratando las viejas como "no dicen nada de su precisión vertical". Verificado sobre las tres trazas de campo: **producen exactamente los mismos números que antes del cambio.**

- **Archivos tocados:**
  - core/commonMain: `model/LocationModels.kt`, `filter/KalmanFilter.kt` (altitud nullable, `usableVerticalAccuracy()`), `usecase/CalculateStatsUseCase.kt`, `usecase/TrackNavigationUseCase.kt`
  - core/commonTest: `filter/GpsTraces.kt` (`withAltitude`), `usecase/CalculateStatsUseCaseTest.kt` (+2)
  - shared/commonMain: `sqldelight/.../MovementSession.sq`, `sqldelight/.../2.sqm` (nuevo), `data/movement/trace/TraceFormat.kt`, `MovementSessionMapping.kt`, `SqlDelightMovementSessionRepository.kt`
  - shared/androidMain: `data/movement/AndroidLocationRepository.kt`
  - shared/commonTest: `data/movement/trace/TraceFormatTest.kt` (+2)
  - shared/jvmTest: `data/db/MovementSessionMigrationTest.kt` (+2), `SqlDelightMovementSessionRepositoryTest.kt`, los dos arneses
  - features: `movement_altitude_quality.feature`

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :core:check`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`
- **Resultados:** `:core` **58 tests, 0 fallos** (antes 56). `:app:shared:jvmTest` **100 tests, 0 fallos** (antes 96). `:core:check`, `assembleDebug`, desktop y web BUILD SUCCESSFUL.

- **Desviación respecto al `.feature`:** salió una migración que el encuadre no mencionaba. No es opcional: sin persistir la precisión vertical, el cálculo derivado la perdería en cada lectura. Se vio al implementar, no al planificar.

- **Sin cobertura de host:** que Android reporte de verdad `hasVerticalAccuracy()` en este teléfono. Es justamente la incógnita que el slice existe para resolver, y solo se responde saliendo con la captura activada.

- **Seguimientos:**
  - **La pregunta abierta:** ¿informa el sistema de que la altitud congelada es mala? Si la precisión vertical se dispara durante esas ventanas, se descarta y es un día de trabajo. Si no, hace falta barómetro (`TYPE_PRESSURE`), que es un slice grande.
  - `onLocationResult` sigue tomando `result.lastLocation` y descartando el resto del lote. Es una pérdida real de lecturas que el sistema ya entregó; anotado, no mezclado aquí.
  - Sigue pendiente repetir la prueba de teléfono quieto a cielo abierto y medir la ruta real.

**Recap:** La app deja de adivinar lo que el receptor sabe de su propia altitud: lee `hasAltitude()` y `getVerticalAccuracyMeters()`, distingue "sin altitud" de "nivel del mar" en todo el recorrido —modelo, filtro, estadísticas, base de datos y traza—, y usa la precisión vertical real cuando la hay, dejando el 2× horizontal como respaldo. No se descarta nada todavía: eso lo decide la próxima salida, que ahora sí traerá el dato para decidirlo.

**Próximos pasos (opciones):** (1) instalar y salir con la captura activada, mirando qué dice la precisión vertical en las ventanas congeladas; (2) repetir la prueba de teléfono quieto a cielo abierto y medir la ruta real; (3) commitear el slice.

---

## Slice 13 (migración C1) — Las rutas dejan de ser una promesa vacía

> **Nota sobre esta entrada:** está escrita **después**, reconstruida desde los tres commits que la
> entregaron (`be68da2`, `28585c9`, `2665585`) y desde el código, no durante el slice. Las decisiones
> y sus porqués salen de los mensajes de commit y del código; los números de validación son de una
> corrida hecha al escribirla, sobre `HEAD`, no de la corrida de cada commit. Se anota porque una
> bitácora que no distingue lo registrado en el momento de lo reconstruido después engaña sobre su
> propia fiabilidad.

- **Objetivo:** que un GPX se pueda abrir y que una salida grabada se pueda repetir. Spec: [`movement_routes_gpx.feature`](../../features/movement_routes_gpx.feature) (7 escenarios).

- **Qué había antes, y por qué esto no era "conectar lo que ya estaba":** `RouteRepository`, `GpxParser` e `ImportRouteUseCase` llevaban en `core` desde que se construyó el pilar **sin una sola implementación conectada**. No había tabla de rutas, así que `MovementSessionEntity.routeId` guardaba `null` en cada sesión jamás grabada, y el único `GpxParserImpl` vivía en `core/src/jvmMain` bajo un paquete que **no coincidía con su propia carpeta** — inalcanzable desde la app en todos los targets, JVM incluido. Importar un GPX y seguirlo nunca fue posible, y nada lo decía. Es exactamente el aviso que el backlog llevaba escrito: que el archivo exista no significa que la funcionalidad esté migrada.

- **Se entregó en tres commits**, no en tres slices: un solo `.feature` los cubre. `be68da2` (dominio, formato GPX y persistencia) · `28585c9` (pantalla de rutas) · `2665585` (guardar una salida del historial como ruta).

- **Decisiones + porqué:**
  - **`GpxFormat` en `commonMain`, sin librería XML.** Un solo lector sirve a Android, iOS, escritorio y web. Entiende **la rebanada de GPX que es una ruta**: el track, su nombre, y de cada punto su posición, elevación y hora. Todo lo demás se **ignora en vez de rechazarse**, porque un archivo exportado por un reloj o por Strava viene lleno de extensiones sobre las que esta app no tiene opinión y aun así tiene que abrirse. Lee etiquetas con namespace, puntos auto-cerrados y `rtept` además de `trkpt`.
  - **Un archivo sin puntos es un fallo que se reporta, no una lista vacía que se devuelve.** Una ruta sin puntos no se puede seguir, ni dibujar, ni medir; descubrirlo tres pantallas más tarde es peor que decirlo al abrir.
  - **Un punto sin elevación no se inventa una.** `WayPoint.altitude` y `timestamp` son nullable y se escriben **sin** su elemento, no con un cero — la misma distinción que el slice 12 hizo para las lecturas del receptor. Una ruta exportada y vuelta a leer dice exactamente lo que decía, y eso es lo que fija el test de ida y vuelta.
  - **El duplicado se pregunta, no se decide.** Importar un track ya guardado devuelve `AlreadyExists` con **las dos rutas** y la pantalla levanta un diálogo con ambos nombres, en vez de quedarse con dos copias en silencio o pisar lo que había. El reconocimiento va por `fingerprint` **primero** (distancia-desnivel-primer/último punto), que es lo que ve el mismo track bajo otro nombre; la comparación por nombre va después y no puede ver ese caso.
  - **El nombre del archivo como respaldo, y decidido dentro del caso de uso.** `fallbackName` sustituyó al override de nombre que no usaba nadie: **solo el caso de uso sabe si el track se nombró a sí mismo**, así que decidirlo fuera obligaba a parsear dos veces o a adivinar. Una ruta llamada "Ruta sin nombre" cuando el archivo decía más que eso no ayuda a nadie a encontrarla otra vez.
  - **Lo único detrás de `expect`/`actual` es abrir y escribir archivos** (`GpxFileAccess`): dame los bytes, toma estos bytes. Es la única parte que ningún target hace igual.
  - **Android va por el Storage Access Framework, así que importar y exportar no necesitan permiso de almacenamiento ninguno:** el usuario elige el archivo y el permiso viaja con la URI que eligió.
  - **El filtro del selector es deliberadamente ancho.** Muchos proveedores de archivos reportan un GPX como `text/xml` o como nada; filtrar estrictamente por `application/gpx+xml` escondería justo los archivos que esto existe para abrir. **Lo que decide si un archivo es una ruta es el parser**, no el tipo MIME que alguien declaró.
  - **Escritorio, iOS y web no tienen acceso a archivos todavía y lo dicen** con `gpxFileAccessAvailable = false`; la pantalla **esconde** ambas acciones en lugar de ofrecer botones que no hacen nada. Grabar sigue siendo solo de Android, así que un build de escritorio tampoco tiene rutas propias que exportar.
  - **`RouteRepository` pierde sus métodos de sesión.** Declaraba rutas **y** sesiones a la vez, lo que obligaba a cada implementación a deber métodos que no le tocaban y duplicaba `MovementSessionRepository` — cuyo propio comentario ya decía que los dos estaban para segregarse. Es el mismo ISP que se aplicó allí.
  - **Guardar una salida como ruta pide nombre, y sin nombre no guarda nada.** "Salida del 9 ago" es lo que pasó ese día, no cómo se llama la ruta; el nombre de la sesión se ofrece como punto de partida y nada más. Menos de dos puntos tampoco se guarda: **un punto es un lugar, no una ruta**.
  - **Sesión y ruta se guardan aparte porque responden preguntas distintas:** una sesión es lo que pasó y pertenece a un día; una ruta es lo que pretendes repetir y **sobrevive a la salida que la produjo**.
  - **La opción se esconde donde las rutas no se pueden guardar en absoluto**, en vez de reportar éxito y no guardar nada: la factory no pasa el caso de uso cuando no hay base de datos, y la pantalla lee `canSaveAsRoute`.

- **La migración (`4.sqm`, versión 4 → 5):**
  - `RouteEntity` y `RoutePointEntity`. **Dos tablas y no una**, igual que las sesiones y por el mismo motivo: la lista de rutas tiene que pintarse **sin leer todos los puntos de todas las rutas**, así que distancia y desnivel se guardan como resumen, y los puntos son lo que la ruta realmente es.
  - **Borrar una ruta borra sus puntos explícitamente**, los puntos primero y dentro de una transacción. No se confía en el `ON DELETE CASCADE` declarado, porque **este proyecto no activa claves foráneas en ningún driver**; confiar en él dejaría huérfanos, para siempre, todos los puntos de cada ruta borrada. La declaración se deja como documentación de la relación, no como mecanismo.
  - **Aquí no se reconstruye ninguna tabla ni se toca una fila existente** — estas tablas no existían antes de esta migración —, así que no hay cascada que razonar, al revés que en `1.sqm`.
  - Nota de numeración: entre el slice 12 y este, el arreglo del pilar de sueño (`9f8ad5b`) se llevó la versión 3 → 4 con su `3.sqm`, así que a las rutas les tocó la 4 → 5.

- **Archivos tocados:**
  - core/commonMain: `parser/GpxFormat.kt` (nuevo, reemplaza a `GpxParserImpl`), `model/Route.kt` (`WayPoint` nullable, `calculateStats`, `calculateFingerprint`), `repository/RouteRepository.kt` (segregado), `usecase/ImportRouteUseCase.kt` (reescrito), `usecase/ExportRouteAsGpxUseCase.kt` (nuevo), `usecase/SaveRouteFromSessionUseCase.kt` (nuevo)
  - core/jvmMain: `parser/GpxParserImpl.kt` (**borrado**, 72 líneas inalcanzables)
  - core/commonTest: `parser/GpxFormatTest.kt` (nuevo, 15), `usecase/SaveRouteFromSessionUseCaseTest.kt` (nuevo, 5), `usecase/ImportRouteUseCaseTest.kt` (adaptado, 4)
  - shared/commonMain sqldelight: `Route.sq` (nuevo), `4.sqm` (nuevo)
  - shared/commonMain: `data/movement/SqlDelightRouteRepository.kt`, `RouteRepositoryProvider.kt` (nuevos, con `NoOpRouteRepository`), `feature/movement/routes/presentation/RoutesViewModel.kt`, `RoutesViewModelFactory.kt`, `routes/ui/RoutesScreen.kt`, `routes/ui/GpxFileAccess.kt` (nuevos), `feature/movement/presentation/MovementNavState.kt` (destino `Routes`), `feature/main/ui/MainScreen.kt`, `feature/movement/history/ui/MovementHistoryScreen.kt` (entrada "Mis rutas"), `feature/movement/detail/presentation/SessionDetailViewModel.kt`, `SessionDetailViewModelFactory.kt`, `detail/ui/SessionDetailScreen.kt`
  - shared/androidMain: `routes/ui/GpxFileAccess.android.kt` (nuevo, SAF)
  - shared/{jvm,js,ios}Main: `routes/ui/GpxFileAccess.<target>.kt` (nuevos, no-op declarado)
  - shared/commonTest: `routes/presentation/RoutesViewModelTest.kt` (nuevo, 9), `detail/presentation/SaveSessionAsRouteTest.kt` (nuevo, 4)
  - shared/jvmTest: `data/movement/SqlDelightRouteRepositoryTest.kt` (nuevo, 10)
  - features: `movement_routes_gpx.feature`

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :core:check`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`

- **Resultados de validación (corrida al escribir esta entrada, sobre `HEAD`):** `:core` **78 tests, 0 fallos** (antes 58, +20). `:app:shared:jvmTest` **128 tests, 0 fallos** (+23 de este slice; los otros 5 respecto a los 100 del slice 12 son del arreglo de sueño `9f8ad5b`, ajeno a este slice). `assembleDebug`, `:app:desktopApp:check` y `:app:webApp:check` BUILD SUCCESSFUL.

- **Lo que esa corrida destapó — `:core:check` llevaba rojo desde este slice:** `compileTestKotlinIosSimulatorArm64` fallaba con `Name contains illegal characters: ","`. **Kotlin/Native no admite comas en un nombre de función entre backticks**, y cuatro tests de `commonTest` escritos en este slice las llevaban. En JVM compilan sin protestar, así que `:core:jvmTest` y `:app:shared:jvmTest` seguían verdes y el fallo pasó inadvertido: **solo lo ve un comando de validación que este slice no llegó a ejecutar.** Arreglado renombrando los cuatro (`GpxFormatTest`, `SaveRouteFromSessionUseCaseTest`, `SaveSessionAsRouteTest`, `RoutesViewModelTest`), sin tocar lo que prueban. La quinta coma vive en `SqlDelightRouteRepositoryTest`, que es `jvmTest` y no compila para iOS: se deja, porque ahí es legal.
  - **La lección, que es de proceso y no de Kotlin:** los comandos de validación por defecto están en `AGENTS.md` precisamente porque cada uno ve algo que los otros no. Correr solo los dos de JVM porque son los rápidos deja fuera el único que compila el código común para los targets nativos.

- **Desviaciones respecto al `.feature`:** ninguna en comportamiento; los 7 escenarios tienen test. Sí hay dos desviaciones respecto a reglas del repositorio, y se anotan aquí en vez de dejarlas calladas: `RoutesScreen` lleva **todo el texto en duro** en lugar de `Res.string.*`, y `RoutesViewModel` expone `message: String` ya redactado en vez de un tipo de mensaje. Ambas reglas se escribieron en el commit **siguiente** (`0d45b7c`), así que no estaban vigentes al escribir esto — pero la deuda es real y `RoutesViewModelTest` la paga ya: sus aserciones son sobre la redacción exacta, que es justo el coste que la regla predice.

- **Sin cobertura de host (dicho explícitamente):**
  - `GpxFileAccess.android.kt` — el selector, la URI, escribir el archivo — es código Android y no se prueba en JVM. Lo que sí está probado es **qué** se lee y **qué** se escribe (`GpxFormatTest`) y todas las decisiones del ViewModel.
  - `RoutesScreen` y su diálogo de duplicado no tienen test: `runComposeUiTest` sigue sin configurarse (deuda transversal ya anotada).
  - `4.sqm` se prueba sobre SQLite en memoria vía JDBC, no sobre `AndroidSqliteDriver`. Hay un test que guarda y lee una ruta **después de migrar** una base preexistente.
  - **Comprobación manual al instalar:** abrir "Mis rutas", importar un GPX de un reloj o de Strava, exportarlo y volver a importarlo.

- **Seguimientos:**
  - **C2 y C3 siguen pendientes, y son lo que hace que una ruta sirva:** verla en el mapa, y seguirla con aviso de desvío. `MovementDestination` no tiene todavía destino de detalle de ruta.
  - **`MovementSessionEntity.routeId` sigue guardando `null` en cada sesión.** Ahora existe la tabla a la que apunta, pero nada ata todavía una grabación a la ruta que iba siguiendo. Eso llega con C3.
  - **Código muerto en `core` que este slice no limpió:** `TrackNavigationUseCase`, `GetRoutesUseCase`, `GetRouteDetailUseCase`, `NavigationController` y `OfflineMapRepository`. `GetRouteDetailUseCase` quedó además **redundante**: `ExportRouteAsGpxUseCase` va directo a `RouteRepository.getRouteWithPoints`.
  - **El fingerprint es heredado de la referencia y no se revisó aquí:** `distancia-desnivel-lat1-lon1-latN-lonN` con la distancia truncada a entero. Dos rutas distintas que empiecen y acaben en el mismo sitio con el mismo total colisionan. Como el duplicado se **pregunta** en vez de decidirse, una colisión molesta pero no destruye nada.
  - i18n de `RoutesScreen` y el mensaje redactado de `RoutesViewModel`, según la regla nueva.

**Recap:** Las rutas dejan de ser tres archivos en `core` que nadie llamaba. Hay un lector y escritor de GPX en código común que abre lo que exporta un reloj o Strava e ignora lo que no entiende, dos tablas nuevas con su migración, y una pantalla desde la que se importa, se renombra, se exporta y se borra. Una salida del historial se guarda como ruta con el nombre que tú elijas, y sale en GPX como cualquier otra. Lo que **no** hay todavía es lo que convierte una ruta en algo que se sigue: verla en el mapa y saber si te estás saliendo.

**Próximos pasos (opciones):** (1) C2 — la ruta en el mapa, que es el destino que falta en `MovementNavState`; (2) barrer el código muerto de `core` que este slice dejó al descubierto; (3) pagar la deuda de i18n de la pantalla de rutas antes de que crezca.

---

## Segunda calibración en campo — la pregunta del slice 12, respondida por una traza que delata algo peor

Cuarta traza real, capturada el 2026-08-29 (`traces/trace-1788047034907.csv`, 33m 30s, 412 lecturas). Es la primera con la columna `verticalAccuracy` que instrumentó el slice 12, y **el receptor sí la reporta**: las 412 lecturas la traen.

**Qué fue la salida (según el usuario):** un rato en bici y después dentro de una casa. **Se olvidó de detener la sesión** hasta que se dio cuenta, media hora más tarde. No es un caso de laboratorio: es lo que pasa de verdad, y por eso vale.

**Fidelidad de la traza:** el replay reproduce **exactamente** los veredictos que el teléfono tomó en vivo (79 aceptadas de 412, mismos recuentos por motivo), así que lo que sigue es lo que la app habría grabado, no una aproximación. Las tres trazas de julio siguen dando los mismos números que en la primera calibración, o sea que el parser de 7 y 8 columnas no rompió nada.

### La traza, frente a las tres de julio

| | T1 · quieto interior | T2 · caminata/trote | T3 · bici | **T4 · bici + media hora quieto** |
|---|---|---|---|---|
| Duración | 3m 11s | 7m 37s | 5m 12s | **33m 30s** |
| Lecturas | 25 | 214 | 139 | **412** |
| Intervalo real (mediana) | 8,8 s | 2,0 s | 2,0 s | **5,9 s** |
| Precisión horizontal (mediana) | 20,0 m | 3,1 m | 4,2 m | **19,2 m** (máx 361) |
| Aceptadas | 1 | 170 | 102 | **79** |
| Distancia grabada | 0 m | 899 m | 1190 m | **2 832 m** |
| Desplazamiento neto | 0 m | 592 m | 590 m | **249 m** |

### Hallazgo 1 — Con el teléfono encima de una mesa, la app habría grabado 2,8 km

Del minuto 2 al 33, el desplazamiento neto desde el inicio se queda clavado entre **244 y 279 m** en todos los tramos de dos minutos: el teléfono estuvo en un punto fijo media hora. La bici fueron los dos primeros minutos (487 m de traza cruda, precisión 4,2 m, 14,6 km/h). **El 84 % del recorrido aceptado se acumula después de que la bici acabara**, y el tiempo en movimiento reportado sería de **391 s** para un teléfono parado.

Por qué pasa el filtro, medido y no supuesto:

- `MAX_NOISE_FLOOR_METERS = 20`: el suelo de ruido es la precisión declarada, con tope de 20 m. Los pasos aceptados después del minuto 2,5 miden **31 m de mediana a 3,02 m/s** — ritmo de trote.
- **Y la precisión que declaran esos saltos es de 6,2 m de mediana.** El receptor afirma ±6 m mientras se coloca a 31 m de donde estaba hace seis segundos, sin moverse. La deriva alrededor del punto donde estuvo: mediana 7 m, p90 **57 m**, máximo **396 m**.
- Un filtro cuyo único modelo de ruido es la precisión declarada **no tiene con qué defenderse**, porque el número en el que se apoya miente justo cuando importa. Los 12 fixes verdaderamente malos (>50 m, hasta 361) sí se rechazaron: la puerta de `POOR_ACCURACY` funciona. La que no existe es la que distingue deambular de viajar.
- El bucle de realimentación lo empeora: `predictedDrift` mide velocidad aparente, la deriva parece 3 m/s, el Kalman **deja de suavizar** por creer que hay movimiento rápido, y el ruido pasa entero al recorrido.

**El discriminante que sí separa los dos casos** (medido sobre las cuatro trazas, ventanas de 60 s, desplazamiento neto dentro de la ventana):

| | T1 quieto | T2 caminata | T3 bici | T4 bici (0-170s) | T4 parado (>170s) |
|---|---|---|---|---|---|
| Neto por ventana (mediana) | 2 m | 113 m | 137 m | 114 m | **6 m** |
| neto/recorrido (mediana) | 0,29 | 0,91 | 0,65 | 0,45 | **0,11** |

Dos órdenes de magnitud de separación. **El movimiento real persiste en una dirección; el ruido se va y vuelve.** Las excursiones sueltas de hasta 396 m ensucian el máximo de la ventana pero no sobreviven a exigir que la posición **se quede** lejos.

**Por qué ningún test lo cazó, otra vez por construcción:** `movement_location_smoothing.feature` tiene desde el slice 8 el escenario "Standing still does not add distance", y pasa. El ruido sintético de `GpsTraces` es pequeño, simétrico y **honesto sobre su propia precisión**. El real es asimétrico, excursiona a decenas de metros y se declara preciso mientras lo hace. Es el mismo patrón que el bug del truncamiento: la traza sintética hacía el fallo invisible. Van dos.

### Hallazgo 2 — La precisión vertical se reporta, y no sirve para nada

La pregunta abierta del slice 12 era: ¿avisa el sistema de que la altitud congelada es mala? **No.**

- `verticalAccuracy` en las 412 lecturas: **mín 1,00 · mediana 1,80 · máx 3,64 m**. Solo 10 lecturas pasan de 3 m.
- La altitud tiene **5 valores distintos en 412 lecturas** y se queda congelada en 206,3 durante **385 s seguidos** — con precisión vertical mediana de **1,67 m** en esa misma ventana y precisión horizontal de 19 m.
- El sistema afirma ±1,7 m verticales sobre un número que lleva seis minutos sin moverse. No se dispara, no sigue a la horizontal, no marca nada.

El plan que esta bitácora dejaba escrito —"si la precisión vertical se dispara en esas ventanas, descartarla es un día de trabajo"— **queda descartado**. Lo que queda es detectar la altitud rancia por repetición exacta (la señal es que el valor no cambia ni un centímetro entre lecturas, que ninguna medición real hace), o barómetro.

**Efecto colateral del slice 12 que conviene ver:** al usar el dato real en vez del 2× horizontal, el umbral de acumulación de desnivel cae a **3,0 m, su suelo**, donde con la estimación anterior habría estado en 12 m, su techo. Sobre una señal cuantizada a cinco valores eso es acumular saltos de cuantización como desnivel. Aquí no hizo daño porque la altitud no se movió: el −10,8 m reportado es un único escalón 217,1 → 206,3 justo al acabar la bici, y **no se puede distinguir** de una bajada real de 11 m en 480 m de recorrido, que también es plausible. El slice 12 no fue en balde —capturó el dato que responde la pregunta— pero su suposición de que el dato serviría era optimista.

### Lo que sigue sin saberse

- **No hay distancia real medida** para la parte de bici, así que los ~480 m son la traza cruda, no una verdad contrastada. Sigue pendiente medir una ruta conocida.
- **Sigue sin haber prueba de teléfono quieto a cielo abierto.** T1 fue en interior con fix de red y T4 fue en interior también: los dos casos "quietos" que tenemos son bajo techo. Lo que T4 añade es que quieto **bajo techo durante media hora** es un caso real que la app hoy falla, y el arreglo se puede calibrar sin salir.

- **Comandos:** `.\gradlew.bat :app:shared:jvmTest --tests "*TraceReplayHarness" --tests "*TraceDiagnosticsHarness"` (BUILD SUCCESSFUL; los arneses solo reportan, la salida se lee en `app/shared/build/test-results/jvmTest/`).
- **Archivos tocados:** ninguno de código. Esta entrada y `features/movement_drifting_signal.feature`, que encuadra el slice que sale de aquí.

**Recap:** La segunda salida cierra la pregunta del slice 12 con un "no" —el receptor declara ±1,8 m verticales sobre una altitud congelada seis minutos— y destapa por el camino algo bastante peor que el desnivel: con el teléfono quieto encima de una mesa media hora, la app habría grabado **2 832 m y 391 s en movimiento**. El motivo no es un umbral mal puesto sino que el filtro se apoya entero en la precisión que declara cada lectura, y ese número miente exactamente cuando el receptor pierde cielo. El discriminante que sí funciona está medido: el desplazamiento neto por ventana de un minuto separa el caso quieto (6 m) del real (113-137 m) por dos órdenes de magnitud.

**Próximos pasos (opciones):** (1) el slice de la distancia fantasma, encuadrado en [`movement_drifting_signal.feature`](../../features/movement_drifting_signal.feature); (2) la altitud rancia por repetición exacta, ahora que se sabe que la precisión vertical no la delata; (3) revisar el umbral de desnivel que el slice 12 dejó en su suelo de 3 m.

---

## Slice 14 (B3) — Un teléfono parado no inventa distancia

- **Objetivo:** que el recorrido deje de crecer cuando el receptor deambula. Spec: [`movement_drifting_signal.feature`](../../features/movement_drifting_signal.feature) (9 escenarios en `@slice-1`, 2 en `@slice-2 @future`).

- **El resultado, sobre las cuatro trazas de campo** (`TraceReplayHarness`, filtro real):

  | Traza | Qué fue | Antes | Ahora |
  |---|---|---|---|
  | T1 | quieto en interior, 3 min | 0 m | **0 m** |
  | T2 | caminata y trote, 7,6 min | 899 m | **884 m** (−1,7 %) |
  | T3 | bici, 5,2 min | 1 190 m | **1 190 m** (0 %) |
  | T4 | bici 2 min + 31 min parado | **2 832 m** | **645 m** |
  | T4 | tiempo en movimiento | 391 s | **192 s** |

  La bici de T4 mide ~480 m de traza cruda, así que la media hora parada aporta ahora ~165 m en vez de ~2 350. De 13 sueltas al recorrido en toda la sesión, 3 son la bici.

- **La regla, y por qué acabó siendo esta.** Se probaron cuatro y se midieron todas contra las trazas antes de escribir el filtro; **tres se descartaron con datos**, y eso es la mitad del trabajo del slice:
  - **Rectitud del tramo (neto/recorrido ≥ 0,6):** descartada. Mata recorridos reales — la bici de T4 tiene una rectitud de 0,45 a 60 s, así que el umbral la borraba entera (485 → 0 m). Una regla que puede no cumplirse nunca además **bloquea la grabación para siempre**.
  - **Confirmar tras *k* lecturas seguidas:** descartada. Ni con k = 10 baja el fantasma (1 496 m): el receptor entrega ráfagas rápidas dentro de las excursiones, así que contar lecturas no mide tiempo.
  - **Comparar la posición de ahora contra la de hace un minuto (dos puntos):** descartada. Un solo pico contamina cualquiera de los dos extremos; sobre T4 dejaba **2 260 m**.
  - **La que quedó:** comparar **dónde ha estado** el receptor en la última media ventana contra dónde estuvo en la anterior, cada una tomada como su **lectura mediana**. Un pico no arrastra una mediana. Es la forma robusta del discriminante que la segunda calibración midió (6 m de desplazamiento neto por minuto parado, 113-137 m caminando o en bici).

- **Decisiones + porqué:**
  - **La ventana es un minuto, y es tiempo, no metros.** Un minuto le pide la misma paciencia a un ciclista que a un caminante; cualquier cifra en metros sería una exigencia distinta para cada uno, que es la regla que este pilar lleva desde el slice 8.
  - **Y el minuto se midió, no se supuso — es también lo que el usuario espera en pantalla.** Con el filtro ya terminado se replayaron las cuatro trazas con ventanas de 20, 30 y 60 s: la media hora parada da **1 433 m a 20 s, 1 613 m a 30 s y 165 m a 60 s**, mientras la caminata y la bici conservan su distancia con las tres. Media espera cuesta un orden de magnitud de mentira, así que la espera se queda. (Aviso de proceso: la primera lectura de esa comparación dio "30 s ≡ 60 s" y era **falsa** — venía de un XML de resultados viejo, porque `:core:jvmTest` había fallado antes en la misma invocación y Gradle nunca llegó a correr el arnés. Leer resultados de test sin comprobar que la corrida es de ahora es exactamente el error que estas trazas existen para no cometer.)
  - **Retener no es descartar.** Una lectura que se aleja del recorrido se guarda; al confirmarse la salida se sueltan **todas**, así que la forma del recorrido y su longitud sobreviven a la espera. Sin esto, quedarse solo con el punto que confirma cuesta un 25 % de la distancia en bici (medido: recortar la traza a un punto cada 30 s deja T3 en 917 m de 1 224).
  - **Y una vez establecido el viaje, se suelta lecturas según llegan.** Solo el primer minuto —y el minuto después de cada parada— llegan de golpe; el resto de la salida tiene la distancia en vivo. Sin esto la pantalla avanzaría a saltos de un minuto durante toda la salida.
  - **Al soltar, el lote se adelgaza al suelo de ruido.** El suelo de ruido es la resolución del recorrido; retener no puede cambiarla. Soltar todas las lecturas retenidas infla la distancia un **14 %** en una caminata con señal gruesa, porque cada punto de más trae su propio error.
  - **El salto imposible pasa a juzgarse como cinemática, no como velocidad.** El fallo real de T4 no era deriva: el receptor **se teletransporta** 170-400 m, se queda ahí un minuto declarando ±3 m de precisión, y vuelve. Por posición no se distingue de un viaje. Lo que lo delata es que **nadie pasa de estar quieto a 60 km/h entre dos lecturas**, así que lo alcanzable se calcula con `v·t + ½at²` a partir de lo que el receptor **venía haciendo** (leído de la traza, no supuesto: sigue sin haber selector de actividad). Un límite de velocidad multiplicado por el hueco deja pasar justo estos casos — 168 m en 9 s son 18 m/s, por debajo del tope de 40.
  - **El umbral del juicio también es robusto.** Compararlo contra la precisión que declara *una* lectura era apoyarse en el número que el campo demostró mentiroso; se usa la **mediana** de las precisiones de la ventana. Durante la media hora parada el receptor soltaba fixes sueltos de 3 m mientras los demás decían 20.
  - **Volver al ancla cancela la salida, pero una sola lectura no cuenta como volver.** Con una señal limpia el suelo de ruido es de 3 m y un caminante entra y sale de él constantemente; reiniciando la espera en cada una, T2 tardaba dos minutos en poder confirmar su primer minuto y perdía 88 m. Se reinicia solo si el receptor **ha estado** alrededor del ancla (otra vez la mediana), no si una lectura lo roza.
  - **La cola de veredictos va en orden de llegada, y esto es un bug que el test cazó.** Una lectura rechazada por precisión o por salto se resolvía al instante mientras otras anteriores seguían retenidas, así que la traza se escribía **fuera de orden** — y una traza fuera de orden **ya no reproduce la sesión de la que salió**, que es lo único para lo que existe. Ahora toda lectura sin veredicto espera su turno en una cola ordenada (`Waiting.Candidate` / `Waiting.TurnedAway`), y `LocationFilterResult.settled` entrega los que ya son definitivos. `TraceReplayTest` lo destapó solo.
  - **Al detener se juzga con lo que haya.** Quien pulsa detener mientras camina perdería el final de su salida; `closing()` aplica el mismo juicio con la ventana que tenga en vez de esperar un minuto que no va a llegar.
  - **Una salida retenida más de una ventana es evidencia caducada.** Si el receptor calla —o solo entrega fixes inservibles— durante un minuto, una posición tomada después no dice nada de dónde estaba antes. Sin esto, T4 soltaba 699 m de un tirón al volver de una excursión de 373 m.

- **El generador sintético, reescrito contra las cifras medidas.** El ruido de `trace()` es pequeño, simétrico y **honesto sobre su propia precisión**; por eso "standing still does not add distance" pasaba desde el slice 8 mientras la app grababa 2 832 m sobre una mesa. `driftingTrace()` reproduce lo medido: deriva base con retorno a la media, excursiones que se van y vuelven, cadencia degradada de 6 s y **precisión declarada de 20 m con fixes sueltos de 5 m**. Un test afirma que el generador cumple las tres estadísticas de campo (p90 de desvío > 40 m, desplazamiento neto por minuto < 20 m, y lecturas que dicen ±5 m desde 30 m de distancia): **el molde también se prueba**, porque van dos bugs de campo que ningún test sintético podía ver.

- **Archivos tocados:**
  - core/commonMain: `filter/LocationFilter.kt` (reescrito: cola ordenada, rastro del último minuto, juicio robusto, cinemática), `usecase/TrackNavigationUseCase.kt` (lote en vez de una posición)
  - core/commonTest: `filter/DriftingSignalTest.kt` (nuevo, 7), `filter/GpsTraces.kt` (`driftingTrace`, veredictos por `settled`), `filter/LocationFilterTest.kt` (1 test adaptado), `filter/TravelPace.kt` (los saltos imposibles se miden como proporción)
  - shared/commonMain: `data/movement/SessionRecording.kt` (suelta lotes, escribe veredictos ya definitivos, cierra el filtro al detener), `feature/movement/detail/presentation/SessionDiagnosis.kt` (motivo nuevo)
  - shared/commonTest: `data/movement/SessionRecordingTest.kt` (1 test adaptado)
  - shared/jvmTest: los dos arneses (lote; el de diagnóstico reporta ahora de dónde sale cada metro)
  - features: `movement_drifting_signal.feature` (nuevo), `movement_location_smoothing.feature` (una línea)

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :core:check`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`
- **Resultados:** `:core` **99 tests, 0 fallos** (antes 92, +7). `:app:shared:jvmTest` **198 tests, 0 fallos**. `:core:check` (compila también para iOS), `assembleDebug`, desktop y web BUILD SUCCESSFUL.
  - Nota: `:app:androidApp:assembleDebug` encadenado con `:app:desktopApp:check` y `:app:webApp:check` en una sola invocación falla en `copySharedComposeResourcesToAssets`; por separado los tres pasan. Es de la tarea de recursos, no del código.

- **Desviaciones respecto a los criterios de aceptación:**
  - **T4 debía quedar en ~480 m y queda en 645.** La media hora parada aporta ~165 m (5 m por minuto) en vez de ~0. Son dos sueltas que sobreviven al juicio: excursiones sostenidas de más de un minuto que por posición son indistinguibles de haber caminado 40 m y haberse quedado ahí. No se siguió afinando para no ajustar constantes contra una sola traza.
  - **T2 y T3 debían mantenerse ±5 % y lo hacen** (−1,7 % y 0 %), pero conviene decir que los 899 y 1 190 m de referencia son lo que grababa el **filtro viejo**, no una distancia medida. Sigue sin haber una ruta real medida contra la que contrastar.
  - **Un escenario del slice 8 cambia de significado:** "none of those readings is treated as an impossible jump" pasa a "the journey is not read as a string of impossible jumps". Con la regla cinemática, un pico de ruido aislado **sí** puede ser imposible —y rechazarlo es correcto—; lo que no puede pasar es que un viaje se lea como una ristra de teletransportes. El test mide ahora la proporción (< 5 %).
  - **Deuda de i18n:** el motivo nuevo entra en `SessionDiagnosis` con el texto en duro, como los otros tres. No se arregla aquí para no mezclar; queda con el resto de la deuda de i18n.

- **El coste en pantalla, dicho sin adornos.** Durante el primer minuto de una grabación —y el minuto siguiente a cada parada real— la **distancia se queda en 0,00 y no se dibuja recorrido**; al confirmarse aparecen los dos de golpe. El mapa y el cronómetro **sí** van en vivo desde el segundo cero: el punto azul sale de `TrackerViewModel.userLocation`, que lee la ubicación cruda del repositorio y no pasa por el filtro. Una parada corta (un semáforo) normalmente no cuesta nada, porque la confianza de "viaje establecido" dura un minuto desde la última confirmación. Esto no se arregla mostrando la distancia retenida: sería volver a poner en pantalla el número que este slice existe para quitar, y encima podría bajar. Lo que sí procede es **decirlo** —"confirmando…" en vez de un 0,00 que parece una app rota—, y eso es el `@slice-2`.

- **Sin cobertura de host (dicho explícitamente):** cómo se siente esa espera en un teléfono real solo se comprueba saliendo. **Comprobación manual al instalar:** empezar a grabar andando y ver que al minuto aparece la distancia de golpe y a partir de ahí avanza sola; y dejar el teléfono quieto diez minutos con la grabación puesta y ver que sigue en 0.

- **Seguimientos:**
  - **El `@slice-2` sigue pendiente, y ahora tiene dos motivos:** que la grabación avise de que lleva mucho sin ir a ninguna parte (lo que habría salvado la salida de la segunda calibración), y que **diga que está confirmando** en vez de enseñar un 0,00 durante el primer minuto. Lo segundo es barato y quita la única cosa de este slice que un usuario nota.
  - Los ~165 m que quedan en T4 se cerrarían con una puerta de innovación en el propio Kalman (rechazar una medición a N sigmas del estimado), que es la técnica estándar y un slice pequeño con estas trazas ya en casa.
  - Sigue pendiente **medir una ruta real** para poder decir cuál de los dos números (884 o 899) es el bueno.
  - La primera lectura de una grabación entra siempre al recorrido sin confirmación. Es correcta —es el punto de partida— pero significa que una sesión de dos lecturas guarda un punto.

**Recap:** El filtro deja de creerse la precisión que declara cada lectura y pasa a preguntar dónde ha estado el receptor: compara la mediana de la última media ventana contra la de la anterior, y hasta que puede responder retiene las lecturas en vez de tirarlas, de modo que confirmar suelta el tramo entero y la distancia se retrasa pero no se pierde. Con eso, y con una puerta de salto que juzga lo alcanzable a partir de lo que el receptor venía haciendo en vez de un límite fijo de velocidad, la media hora con el teléfono en una mesa baja de 2 832 m a ~165, mientras la caminata y la bici reales conservan su distancia (−1,7 % y 0 %). Por el camino, un test destapó que la traza se estaba escribiendo fuera de orden, lo que la habría dejado sin servir para lo único que existe.

**Próximos pasos (opciones):** (1) el aviso de "llevas mucho parado" del `@slice-2`; (2) la puerta de innovación en el Kalman, que cerraría los 165 m que quedan; (3) B4, la altitud rancia; (4) salir a medir una ruta conocida y cerrar de una vez la duda de la distancia caminando.

---

## Slice 15 (B3, `@slice-2` a medias) — La pantalla dice lo que está esperando

- **Objetivo:** que el minuto de espera del slice 14 se entienda en vez de sufrirse. Spec: el `Scenario Outline` `@slice-2` de [`movement_drifting_signal.feature`](../../features/movement_drifting_signal.feature).
- **El problema exacto, que salió al revisar el slice anterior:** un `0,00 km` mientras llevas 80 m andados y un `0,00 km` de un teléfono que no se ha movido **se ven idénticos y dicen cosas opuestas**. El primero se lee como una app rota. La distancia no se puede adelantar sin devolver la mentira a la pantalla, pero decir que se está esperando no cuesta nada.

- **Decisiones + porqué:**
  - **Un tipo sellado, no un texto** (`TrackerDistance`: `WaitingForAFix` / `Confirming` / `Travelled`). La regla de `AGENTS.md` es que la presentación no reparte texto ya redactado; además un test sobre "Confirmando…" sería un test sobre la redacción.
  - **El estado se deriva de lo que ya hay, sin tocar el dominio.** `traveledPoints` vacío es que aún no hay fix; **un solo punto es el sitio donde empezaste, no un viaje** — el filtro está reteniendo lo que ha llegado desde entonces. Dos o más puntos es que hay distancia medida. No hizo falta que el filtro publicara su cola: `RecordingState` ya lo dice.
  - **En reposo se sigue enseñando el cero de siempre**, porque ahí no hay nada pendiente: no hay sesión.
  - **Los dos textos nuevos van a `strings.xml`** (`tracker_distance_waiting_for_fix`, `tracker_distance_confirming`), que es lo que pide la regla de i18n para todo lo que se toque de ahora en adelante. El resto de `TrackerScreen` sigue con el texto en duro: es deuda anterior y no se mezcla aquí.

- **Archivos tocados:** shared/commonMain `feature/movement/tracker/presentation/TrackerDistance.kt` (nuevo), `feature/movement/tracker/ui/TrackerScreen.kt`, `composeResources/values/strings.xml`; shared/commonTest `feature/movement/tracker/presentation/TrackerDistanceTest.kt` (nuevo, 4).
- **Comandos:** `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :core:check`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`
- **Resultados:** `:app:shared:jvmTest` **202 tests, 0 fallos** (antes 198, +4). `:core:check`, `assembleDebug`, desktop y web BUILD SUCCESSFUL.

- **Sin cobertura de host:** `TrackerScreen` no tiene test de UI —necesita el mapa `actual` y la factory del ViewModel—, así que lo probado es el mapeo, como se hizo con `SessionDiagnosis`. **Comprobación manual:** al pulsar Iniciar debe verse "Buscando señal", luego "Confirmando…", y al minuto la distancia.

- **Seguimientos:** queda la otra mitad del `@slice-2`, avisar de que la grabación lleva mucho rato sin ir a ninguna parte — que es lo que habría salvado la salida de la segunda calibración. Y una sesión que se quede quieta desde el principio se queda en "Confirmando…" indefinidamente: es cierto, pero ese aviso es justo el que falta.

**Recap:** El minuto que el filtro necesita para no inventarse kilómetros deja de parecer una app rota: mientras no hay fix la pantalla dice que busca señal, mientras el recorrido es solo el punto de partida dice que confirma, y en cuanto hay distancia medida la enseña. El mapa y el cronómetro seguían yendo en vivo desde el principio; lo único que faltaba era que el número dijera la verdad sobre sí mismo.

**Próximos pasos (opciones):** (1) la otra mitad del `@slice-2`, el aviso de llevar mucho parado; (2) la puerta de innovación en el Kalman; (3) B4, la altitud rancia; (4) instalar y salir a medir una ruta conocida.

---

## Slice 16 (B3, `@slice-2` completo) — El silencio no es quietud

- **Objetivo:** que una grabación olvidada se note mientras pasa, y que no se guarde como si la espera fuera parte de la salida. Spec: los cuatro escenarios `@slice-2` de [`movement_drifting_signal.feature`](../../features/movement_drifting_signal.feature).
- **De dónde sale:** de la salida real de la segunda calibración. El usuario se bajó de la bici, entró en casa y la grabación siguió media hora. Con el slice 14 esa media hora ya no inventa kilómetros, pero la sesión seguía guardándose como **33 minutos** para 645 m — un ritmo de 55 min/km que no describe nada.

- **La decisión que sostiene todo el slice: el silencio no es quietud.**
  - Una grabación en un túnel y una grabación encima de una mesa se parecen en lo único que el modelo miraba: **el recorrido deja de crecer**. Y son cosas opuestas — en una el usuario puede estar pedaleando.
  - Lo que las separa es si el **receptor sigue hablando**. Así que `RecordingState` gana `lastReadingAtMillis`, que anota **toda** lectura entregada, la acepte el filtro o no, y la quietud se mide como la distancia entre esa marca y el último punto del recorrido.
  - El efecto bonito de medirlo así: si el receptor se calla, la cifra **se congela** en vez de envejecer. Un teléfono sin señal nunca acumula "diez minutos sin moverte", porque nadie ha visto que no se moviera.

- **Decisiones + porqué:**
  - **El umbral es 5 minutos**, lo bastante largo para que no lo dispare un semáforo, una tienda ni una foto, y lo bastante corto para pillar una grabación olvidada con la salida todavía fresca. La que produjo la regla llevaba 31 minutos.
  - **Se avisa en los dos sitios, y el segundo es el que importa.** En el tracker sale una tarjeta ("Llevas 6 min sin moverte. ¿Sigues grabando?"), pero **cuando esto pasa de verdad el teléfono está en un bolsillo y la app no está en pantalla**: por eso el aviso va también en la notificación persistente del servicio en primer plano, que es lo único visible en ese momento.
  - **No se detiene sola.** La app no sabe si estás parado en un mirador o si te olvidaste; informa y deja el botón de Detener donde estaba. Detener por su cuenta convertiría un falso positivo en pérdida de datos.
  - **Al guardar, la sesión termina donde se movió por última vez** — pero solo si la quietud fue *presenciada*. Es la misma distinción de arriba: una sesión que perdió la señal conserva su duración entera, porque recortarla sería afirmar algo que la app no vio. Con esto, la salida de la segunda calibración pasa de 33 min a los ~9 que el filtro llegó a registrar, y su ritmo deja de ser una cifra absurda.
  - **Se recorta la duración, no lo grabado.** Los puntos siguen todos ahí; lo que cambia es hasta cuándo dice la sesión que duró.
  - **Los textos de la pantalla van a `strings.xml`**; el de la notificación se queda en duro **como el resto de ese archivo**, que es anterior a la regla de i18n y no es un `@Composable` (no puede leer el catálogo igual). Anotado en la deuda, no mezclado aquí.

- **Archivos tocados:**
  - core/commonMain: `model/RecordingState.kt` (`lastReadingAtMillis`, `observed`, `secondsWithoutMoving`, `goneNowhereMinutes`, y el recorte en `stopped`)
  - core/commonTest: `model/RecordingStateTest.kt` (+5)
  - shared/commonMain: `data/movement/SessionRecording.kt` (anota cada lectura), `feature/movement/tracker/ui/TrackerScreen.kt` (la tarjeta), `composeResources/values/strings.xml`
  - shared/androidMain: `data/movement/MovementRecordingService.kt` (la notificación lo dice)
  - shared/commonTest: `data/movement/SessionRecordingTest.kt` (+1)
  - features: `movement_drifting_signal.feature` (los cuatro escenarios `@slice-2` detallados y sin `@future`)

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :app:shared:jvmTest`, `.\gradlew.bat :core:check`, `.\gradlew.bat :app:androidApp:assembleDebug`, `.\gradlew.bat :app:desktopApp:check`, `.\gradlew.bat :app:webApp:check`
- **Resultados:** `:core` **104 tests, 0 fallos** (antes 99). `:app:shared:jvmTest` **203 tests, 0 fallos** (antes 202). `:core:check`, `assembleDebug`, desktop y web BUILD SUCCESSFUL.

- **Desviación de proceso, que vale más que el arreglo:** un test nuevo se llamaba `a receiver that went quiet reports nothing, because silence is not stillness` y **`:core:jvmTest` pasó en verde**. `:core:check` lo tumbó: `Name contains illegal characters: ","` — Kotlin/Native no admite comas en nombres entre backticks. Es **exactamente** el fallo que el slice 13 dejó anotado, y volvió a colarse por el mismo camino: correr solo los comandos rápidos de JVM. Sigue siendo cierto que cada comando de validación ve algo que los otros no.

- **Sin cobertura de host:** la notificación es código Android y no se prueba en JVM; lo probado es la regla (`goneNowhereMinutes`) y el recorte al guardar. **Comprobación manual:** grabar, dejar el teléfono quieto cinco minutos y ver que la notificación pasa a decir "· 5 min sin moverte" y que la tarjeta sale en el tracker; luego detener y comprobar en el historial que la sesión dura lo que duró la salida, no lo que duró el olvido.

- **Seguimientos:**
  - Una sesión que **nunca** se mueve no tiene último punto que valga, así que ni avisa ni se recorta: se queda en "Confirmando…". Es honesto, pero el aviso de "no has ido a ninguna parte" no la cubre porque no hay recorrido del que medir la distancia. Cubrirla es medir desde el inicio de la grabación en vez de desde el último punto.
  - El recorte usa la marca de tiempo del GPS contra el reloj del sistema. Son ambos epoch UTC, pero si un receptor entregara marcas muy desviadas la duración saldría rara; `elapsedAt` la acota a cero como mínimo.
  - Sigue pendiente la puerta de innovación en el Kalman: los ~165 m de fantasma que quedan son también los que empujan el final de la salida de T4 de ~170 s a ~555 s.

**Recap:** Una grabación olvidada ahora se nota mientras pasa —en el tracker y, sobre todo, en la notificación, que es lo que se ve con el teléfono en el bolsillo— y deja de guardarse como si la espera fuera parte de la salida: la sesión termina donde se movió por última vez. Todo ello apoyado en una distinción que el modelo no hacía y que es la que evita mentir en el otro sentido: quedarse sin señal no es quedarse quieto, así que una sesión que perdió el GPS conserva su duración entera y nunca se le dice al usuario que no se ha movido cuando nadie ha podido verlo.

**Próximos pasos (opciones):** (1) la puerta de innovación en el Kalman, que cierra los ~165 m que quedan; (2) B4, la altitud rancia; (3) instalar y salir: medir una ruta conocida y comprobar los avisos en el teléfono; (4) C2, la ruta en el mapa.

---

## Punto C2 — Una ruta guardada se puede ver antes de salir con ella (2026-08-30)

- **Objetivo:** que una ruta importada deje de ser una fila con dos cifras y se pueda mirar. Spec:
  [`movement_route_detail.feature`](../../features/movement_route_detail.feature) (6 escenarios).

- **Encuadre** (del backlog, sin cambios): **Problem** — importas un GPX y no hay forma de comprobar
  que el archivo es el que creías; un track equivocado se descubre en el monte. **Savings** — dejar
  de depender de otra app para mirar una ruta y no gastar una salida siguiendo lo que no era.
  **Why** — "recorrer rutas" es la promesa central del pilar; sin poder verla, una ruta guardada es
  una fila en una lista.

### Lo que hizo el slice barato

**`MovementMap` ya existía con `fitPathInView`**, escrito en el slice 6 para encuadrar el recorrido
de una sesión terminada. Una ruta y una sesión son cosas distintas, pero dibujarlas es lo mismo, así
que C2 fue conectar lo que había: un destino nuevo, un ViewModel que observa `getRouteWithPoints`
—que también existía— y una pantalla.

### Decisiones + porqué

- **Se observa el repositorio, no se lee una vez.** Si la ruta se borra o se renombra desde la lista
  mientras el detalle la enseña, lo que se ve deja de ser una foto vieja. `Missing` es un estado de
  primera clase por eso, no sólo por un id inválido.
- **Toda la tarjeta de la lista abre la ruta**, con `LeafCard(onClick=)`, que ya lo soportaba. Los
  tres botones de dentro siguen funcionando: en Compose el hijo se queda el toque, así que no hizo
  falta excluirlos a mano.
- **Dos puntos para dibujar, no uno.** `hasPath` exige `size >= 2`: un punto es un lugar, no un
  trazado. Es la misma regla que el slice 13 puso al guardar una salida como ruta.
- **Las cifras se enseñan aunque no haya trazado.** Que no se pueda dibujar no borra lo que la ruta
  sabe de sí misma.
- **La pantalla nace en `Res.string.*`**, no en duro. `RoutesScreen`, que es su vecina, lleva todo el
  texto en el código desde el slice 13 — esa deuda sigue, pero no se amplía.
- **`RouteDetailUiState` no lleva texto redactado.** Los rótulos los pone la pantalla; lo que viaja
  ya convertido son las cifras, que es formato y no copia — la misma distinción que `AGENTS.md` hace
  con los nombres de mes de `MovementFormat`.

### La mentira heredada que esto destapó

`calculateStats` acumula **0.0 de desnivel cuando ningún punto trae altitud**, y ese cero se guarda
en `RouteEntity.elevationGain` **indistinguible de un llano de verdad**. Un GPX sin elevaciones no
describe una ruta plana: no dice nada sobre su desnivel, y pintar «0 m» es afirmar algo que
probablemente es falso.

La pantalla lo recupera mirando los puntos —si ninguno trae altitud, el desnivel es desconocido— y
`MovementFormat.elevation(null)` ya sabía pintar «—» por la misma razón, desde el slice 12. Es la
tercera vez que este pilar tropieza con lo mismo: **un dato ausente guardado como cero**. La primera
fue la altitud de las lecturas (slice 12), la segunda la altitud de los `WayPoint` al escribir GPX
(slice 13), y esta es la tercera. Lo que queda mal es **la columna**, no la pantalla: se arregla
donde se escribe, no donde se lee. Anotado como seguimiento.

### Archivos tocados

- shared/commonMain: `feature/movement/routes/presentation/RouteDetailUiState.kt` (nuevo, con la
  función pura `routeDetail`), `RouteDetailViewModel.kt` (nuevo, con su factory),
  `feature/movement/routes/ui/RouteDetailScreen.kt` (nuevo),
  `feature/movement/presentation/MovementNavState.kt` (destino `RouteDetail`),
  `feature/movement/routes/ui/RoutesScreen.kt` (la fila abre la ruta),
  `feature/main/ui/MainScreen.kt` (cableado)
- `composeResources/values/strings.xml`: 6 cadenas nuevas
- shared/commonTest: `RouteDetailUiStateTest` (10, nuevo)
- shared/jvmTest: `RouteDetailScreenTest` (8, nuevo)
- `features/movement_route_detail.feature` (nuevo)

### Comandos y resultados

`.\gradlew.bat :core:jvmTest` · `:app:shared:jvmTest` · `:core:check` · `:app:shared:check` ·
`:app:androidApp:assembleDebug` · `:app:desktopApp:check` · `:app:webApp:check` · `:server:test`

`:core` **104 tests, 0 fallos** (sin cambio por este slice). `:app:shared:jvmTest` **221 tests, 0
fallos** (+18 de aquí). Todo lo demás BUILD SUCCESSFUL. Los 18 pasaron a la primera.

### Sin cobertura de host (dicho explícitamente)

- **El mapa no se dibuja en la JVM.** `MovementMap` sólo tiene implementación real en Android; en los
  demás targets es un placeholder. Lo que el test afirma es **qué decide la pantalla** —si hay
  trazado, qué cifras salen, qué se dice cuando no hay nada— no que el trazado se pinte bien. Que el
  render funcione sólo lo dice el dispositivo, y este pilar ya sabe lo que cuesta olvidarlo: el
  render del mapa estuvo roto tres slices tras el slice 6.
- **Comprobación manual al instalar:** importar un GPX de un reloj o de Strava, abrirlo desde "Mis
  rutas" y comprobar que el trazado sale entero y encuadrado; abrir una ruta guardada desde una
  salida propia; borrar una ruta desde la lista teniendo su detalle abierto detrás.

### Deuda y seguimientos

- **El cero de desnivel sigue guardándose en la base.** Arreglarlo donde se escribe pide una columna
  nulable y una migración; la pantalla ya no miente, pero la fila sí.
- **C3 sigue pendiente** y es lo que queda para que una ruta se siga: `TrackNavigationUseCase`
  son 164 líneas heredadas, sin consumidor y sin un solo test. Ahí es donde
  `MovementSessionEntity.routeId` deja de guardar `null`.
- **`RoutesScreen` sigue con el texto en duro** y `RoutesViewModel` sigue devolviendo `String` ya
  redactado. Ahora que hay tooling de test de pantalla, esa deuda es más barata de pagar que antes.
- **`GetRouteDetailUseCase` sigue sin consumidor** — este slice fue directo al repositorio, igual que
  el export. Sigue siendo candidato a borrado junto con `GetRoutesUseCase`.

**Recap:** Una ruta guardada se puede abrir y ver. Se toca en la lista y sale su trazado encuadrado
en el mapa con distancia, desnivel y número de puntos; si ya no está, lo dice; si se guardó sin
puntos, lo dice en vez de enseñar un mapa vacío. Reutiliza el mapa que ya existía y el método del
repositorio que llevaba desde el slice 13 sin llamar. De paso destapa que el desnivel de una ruta sin
altitudes se guarda como cero y se pintaba como llano.

**Próximos pasos (opciones):** (1) C3, seguir la ruta con aviso de desvío, que es lo que queda del
punto C y donde `routeId` deja de ser `null`; (2) arreglar el cero de desnivel en la base, que es una
migración pequeña; (3) pagar la deuda de i18n de `RoutesScreen`, ahora más barata con el tooling de
test puesto.

---

## Arreglo — El desnivel desconocido deja de guardarse como cero (2026-08-30)

- **Objetivo:** cerrar lo que C2 destapó y yo había dejado anotado en vez de arreglado. El usuario
  preguntó si no iba primero. Iba.

### Por qué no bastaba con lo que hice en C2

En C2 arreglé **la pantalla de detalle**, derivando el desnivel desconocido de los puntos: si
ninguno traía altitud, «—». Lo que no miré es que **`RoutesScreen` sigue pintando
`route.elevationGain` tal cual**, así que la lista seguía diciendo «0 m» para las mismas rutas. Fixé
la mitad y lo llamé hecho.

Y el truco no se podía extender: `SqlDelightRouteRepository.getAllRoutes()` carga las rutas
**sin sus puntos**, a propósito y con su comentario explicándolo — pintar la lista no puede leer
todos los puntos de todas las rutas. Con sólo la fila en la mano no hay nada con qué distinguir los
dos casos. **La columna resumen es lo que la lista se cree, así que la columna resumen es la que
tiene que ser honesta.** Eso es lo que obliga a que el arreglo sea en el origen y no en una pantalla.

### Decisiones + porqué

- **`calculateStats` devuelve `Double?`.** Nulo cuando **ningún par de puntos consecutivos** trae las
  dos altitudes: sin comparar nada no hay medida. Cero sólo significa cero cuando algo se comparó y
  no subió — un descenso solo, por ejemplo, sí es una medida y da cero.
- **El par, no el punto.** Dos altitudes separadas por un punto sin ella no forman ningún par
  comparable, así que eso también es desconocido. La versión que escribí en C2 miraba «algún punto
  con altitud», que se equivoca en ese caso.
- **`Route.elevationGain` pasa a nulable**, y con él la columna. `MovementFormat.elevation(null)` ya
  sabía pintar «—» desde el slice 12, así que la lista y el detalle quedaron correctos sin tocarles
  una línea de UI.
- **La huella no cambia.** `calculateFingerprint` usa `(elevationGain ?: 0.0).toInt()`, que es
  exactamente lo que se guardaba antes. Si cambiara, las rutas ya importadas dejarían de reconocerse
  y empezarían a aparecer como duplicados nuevos. Hay un test que lo fija.
- **La migración rellena mirando los puntos**, con un `EXISTS` sobre `RoutePointEntity`: el 0.0
  vuelve a NULL sólo donde la ruta no tiene ningún punto con altitud. Es la misma pregunta que hace
  `calculateStats`, hecha sobre lo ya guardado.
- **`GpxFormat.parse` devuelve `elevationGain = null`** en vez de 0.0: ahí todavía no se ha medido
  nada, las cifras las calcula quien importa.

### La migración (`6.sqm`, versión 6 → 7)

SQLite no relaja `NOT NULL`, así que se reconstruye `RouteEntity`. Soltarla es la parte delicada:
`RoutePointEntity` la referencia con `ON DELETE CASCADE`, y con claves foráneas activadas el DELETE
implícito de SQLite **se llevaría todos los puntos de todas las rutas**. Este proyecto no las activa
en ningún driver, y `RouteElevationMigrationTest` lo comprueba en vez de dejarlo razonado — el mismo
cuidado que `1.sqm` tuvo con las sesiones.

### Un test que afirmaba el bug

`SaveRouteFromSessionUseCaseTest` tenía un caso «Sin altitud», con todos los puntos sin elevación,
que afirmaba `assertEquals(0.0, route.elevationGain)`. **El test estaba fijando la mentira**: decía
que una salida de la que no se sabe el desnivel es llana. Ahora afirma `null`. Vale la pena
anotarlo: una suite verde no dice que el comportamiento sea correcto, sólo que es el que alguien
escribió.

### Archivos tocados

- core/commonMain: `model/Route.kt` (`elevationGain` nulable, `calculateStats` devuelve `Double?`,
  huella estable), `parser/GpxFormat.kt` (parse devuelve nulo)
- core/commonTest: `model/RouteStatsTest.kt` (10, nuevo), `usecase/ImportRouteUseCaseTest.kt` y
  `usecase/SaveRouteFromSessionUseCaseTest.kt` (adaptados; uno de ellos afirmaba el bug)
- shared/commonMain sqldelight: `Route.sq` (columna nulable), `6.sqm` (nuevo)
- shared/commonMain: `feature/movement/routes/presentation/RouteDetailUiState.kt` (se simplifica: el
  apaño de C2 desaparece)
- shared/commonTest: `RouteDetailUiStateTest` (los dos casos del apaño se sustituyen por dos de paso
  a través)
- shared/jvmTest: `data/db/RouteElevationMigrationTest.kt` (8, nuevo)

**Sin cambios en la UI:** `RoutesScreen` y `RouteDetailScreen` ya pasaban el valor a
`MovementFormat.elevation`, que acepta nulo desde el slice 12.

### Comandos y resultados

`.\gradlew.bat :core:jvmTest` · `:app:shared:jvmTest` · `:core:check` · `:app:shared:check` ·
`:app:androidApp:assembleDebug` · `:app:desktopApp:check` · `:app:webApp:check` · `:server:test`

`:core` **114 tests, 0 fallos** (antes 104, +10). `:app:shared:jvmTest` **228 tests, 0 fallos**
(antes 221, +7 netos: +8 de migración, y dos del detalle sustituidos por dos más simples). Todo lo
demás BUILD SUCCESSFUL.

**`:core:check` volvió a ganarse el sitio:** falló con `Name contains illegal characters: ","` — un
nombre de test con coma, que Kotlin/Native no admite entre backticks. Es **exactamente** el fallo del
slice 13, que aquella entrada dejó anotado como lección de proceso: los comandos de validación por
defecto están en `AGENTS.md` porque cada uno ve algo que los otros no, y JVM compila esas comas sin
protestar.

### Sin cobertura de host

- La migración se prueba sobre SQLite en memoria vía JDBC, no sobre `AndroidSqliteDriver`.
- **Comprobación manual al instalar sobre una base existente:** abrir "Mis rutas" y comprobar que
  una ruta importada de un GPX sin elevaciones muestra «—» y no «0 m», y que una ruta con desnivel
  medido sigue enseñando su cifra.

### Lo que esto deja dicho para el pilar

Es la **tercera** vez que aparece el mismo error: un dato ausente guardado como cero. La altitud de
una lectura (slice 12), la de un `WayPoint` al escribir GPX (slice 13), y el desnivel de una ruta
(aquí). Las tres veces el síntoma fue el mismo —algo afirmaba «cero» donde no sabía— y las tres el
arreglo fue nulable. Queda por revisar si `MovementSessionEntity` tiene columnas con el mismo vicio;
no se ha mirado en este arreglo.

**Recap:** El desnivel de una ruta ya no miente en ninguna de las dos pantallas. La columna es
nulable, `calculateStats` distingue «no subió» de «no se midió» mirando si llegó a comparar dos
altitudes, y la migración rellena lo ya guardado preguntando a los puntos. La huella no cambia, así
que el reconocimiento de duplicados sigue funcionando sobre lo importado antes. Un test que afirmaba
el comportamiento equivocado quedó corregido.

**Próximos pasos (opciones):** (1) C3, seguir la ruta con aviso de desvío, que es lo que queda del
punto C; (2) revisar si `MovementSessionEntity` guarda ceros donde quiere decir «no se sabe», que es
el mismo vicio por tercera vez; (3) la deuda de i18n de `RoutesScreen`.

---

## Slice — B4, slice 1: el desnivel se calla cuando la altitud se queda pegada

- **Objetivo:** que una sesión con la altitud congelada deje de reportar un desnivel con la misma
  confianza que una con señal sana. Spec: [`movement_altitude_staleness.feature`](../../features/movement_altitude_staleness.feature)
  (5 escenarios). Ataca el camino (a) que dejó abierto el slice 12; el barómetro (camino b) queda
  fuera.

- **Decisiones + porqué:**
  - **La señal es duración, no cuántas lecturas repiten.** El encuadre inicial proponía contar
    repeticiones ("a la tercera lectura idéntica en adelante"); se cambió antes de escribir código
    porque el muestreo no es constante (jitter, señal degradada a 6 s), así que contar lecturas
    respondería una pregunta distinta según la calidad del fix. Medir cuánto tiempo lleva sin cambiar
    es lo que de verdad importa —cuánto terreno pudo cambiar sin que el sensor se enterara— y es
    comparable directamente con los 385 s medidos en campo.
  - **Umbral de partida: 60 s.** Muy por debajo del incidente medido, sin ningún caso real que muestre
    una repetición corta y legítima. Anotado explícitamente como el número a revisar en la próxima
    salida de campo, no como una constante ya calibrada.
  - **Una racha rancia apaga el desnivel de la sesión entera, no solo el tramo congelado.** Una sesión
    que pasó minutos con el sensor pegado no puede afirmar un desnivel completo y honesto aunque el
    resto de su altitud se vea sano; mezclar un tramo fiable con uno inventado seguiría siendo
    ficción, solo que parcial. Distancia, tiempo en movimiento y ritmo no dependen de la altitud y no
    se tocan.
  - **Una racha de dos lecturas ya expresa su span completo.** El detector compara el timestamp de la
    lectura actual contra el de la primera lectura de la racha (no contra el número de lecturas en
    medio), así que dos lecturas separadas por el umbral bastan para probar el límite exacto sin
    depender de cuántas veces se muestreó entre medio.
  - **El umbral de 3 m que dejó el slice 12 no se toca en este slice.** Con la racha ya detectada, el
    desnivel completo de la sesión se apaga antes de que ese umbral entre en juego; queda como deuda
    aparte, tal como decía el roadmap.

- **Lo que salió al escribir los tests, no al planificar:** un test ya existente
  (`aSessionAtSeaLevelIsNotMistakenForOneWithoutAltitude`) construía su traza con
  `altitudeNoiseMeters` en su valor por defecto, `0.0` — una altitud de verdad constante, exactamente
  lo que este slice existe para detectar. Antes de esta regla eso era inofensivo; ahora colisiona con
  ella. Se le añadió ruido de altitud realista (`10.0`), preservando lo que el test siempre quiso
  probar (nivel del mar no se confunde con "sin altitud"), porque un GPS real jamás sostiene un valor
  exacto sin variar. Uno de los tests nuevos también tuvo que simplificarse: comparaba una magnitud
  exacta de ascenso con un `seed` que no la sostenía dentro de la tolerancia — no era un fallo de la
  regla, era ruido de la semilla elegida; se dejó afirmando solo que el ascenso sigue midiéndose (que
  es lo que la escena realmente necesita probar), ya que la magnitud exacta la cubre
  `aRealClimbIsMeasured`.

- **Archivos tocados:**
  - core/commonMain: `usecase/CalculateStatsUseCase.kt` (`hasStaleAltitudeRun()`, nueva constante
    `STALE_ALTITUDE_RUN_MILLIS`, `measuresAltitude` ahora también exige que no haya racha rancia)
  - core/commonTest: `usecase/CalculateStatsUseCaseTest.kt` (+6, uno de ellos corrigiendo la traza de
    un test existente)
  - features: `movement_altitude_staleness.feature` (nuevo)
  - docs: `movement-tracking-migration.md` (B4, slice 1 encuadrado)

- **Comandos:** `.\gradlew.bat :core:jvmTest`, `.\gradlew.bat :core:check`.
- **Resultados:** `:core:jvmTest` **124 tests, 0 fallos** (subiendo de los 118 con los que cerró el
  slice 14 — 6 pruebas nuevas de este slice, más las que se sumaron entre medias). `:core:check`
  BUILD SUCCESSFUL en JVM, JS e iOS (`iosSimulatorArm64Test` se salta en Windows, como siempre).

- **Sin validar en dispositivo:** el umbral de 60 s es una elección conservadora sin contraejemplo
  real que la contradiga, no un número calibrado contra una segunda traza. Sigue pendiente salir a
  medir otra sesión con GPS débil para confirmar que 60 s no es ni demasiado corto (falsos positivos
  en una pausa corta y legítima) ni demasiado largo (deja pasar una racha rancia más breve que también
  merecería silenciarse).

**Recap:** `CalculateStatsUseCase` ya distingue terreno llano de verdad de un receptor que dejó de
actualizar: una racha de altitud idéntica que se sostiene 60 s o más apaga el desnivel de la sesión
entera —máxima, mínima, ascenso, descenso, pendiente y VAM— en vez de mezclarlo con un número que
parece completo sin serlo. Lo que no depende de la altitud sigue igual. El camino (b), barómetro,
queda fuera; la deuda del umbral de 3 m del slice 12 también.

**Próximos pasos (opciones):** (1) salir a medir con GPS débil para validar (o ajustar) el umbral de
60 s; (2) revisar el umbral de 3 m del slice 12 ahora que las rachas largas ya no lo alcanzan a
ensuciar; (3) el barómetro (`TYPE_PRESSURE`) si la detección por repetición no basta en uso real; (4)
C3, seguir una ruta con aviso de desvío, que sigue siendo el punto más grande sin tocar del pilar.
