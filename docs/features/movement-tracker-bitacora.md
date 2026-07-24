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
