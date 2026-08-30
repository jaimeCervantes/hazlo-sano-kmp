# Bitácora — el tablero de un pilar se arma por secciones

Append-only. Una entrada por slice.

## Slice 1 — El orden del tablero: campeones, retos, buscar (2026-08-30)

**Objetivo.** Que las cuatro pestañas de pilar se lean en el orden del proyecto Android de
referencia: identidad del pilar arriba, comunidad en medio —campeones semanales y retos activos— y
el catálogo al final, cerrando con "Buscar productos y servicios".

**Decisiones y por qué.**

1. **Una sola fuente para los cuatro pilares.** `PillarHighlightsRepository` en `core` con
   `SamplePillarHighlightsRepository` en `app/shared`. Los campeones de Sueño estaban escritos dentro
   de `MockSleepRepository`; se **mudaron**, no se copiaron. Dos fuentes para el mismo contenido es
   el fallo de diseño que `AGENTS.md` nombra por su nombre.
2. **Contenido de muestra, no inventado.** Los valores salen del proyecto Android de referencia
   (`MovementDashboardScreen.kt`, `NutritionDashboardScreen.kt`, `MindDesign.kt`) y de lo que Sueño
   ya enseñaba. Decisión explícita del usuario: el tablero se ve completo hoy y la implementación se
   sustituye cuando el backend publique los suyos.
3. **ViewModel aparte del catálogo.** `PillarHighlightsViewModel` no comparte estado con
   `PillarCatalogViewModel` porque no dependen de la misma fuente ni tardan lo mismo. Tenerlos
   separados es lo que hará posible el slice 2: que una espera de red no arrastre a lo que ya está.
4. **Las secciones compartidas viven en `core/ui/components/sections/`.** `pillarHighlightsSections`
   lo usan el tablero de pilar y Sueño; dejarlo dentro de una de las dos pantallas habría obligado a
   la otra a importar de una feature ajena.
5. **El espacio va antes de cada sección**, como en los carruseles del catálogo, para que una sección
   ausente no deje un hueco doble.
6. **Se borró el camino muerto de `SleepContent`.** Al salir campeones y retos, sus campos de hero no
   los pintaba nadie: `SleepContent`, `SleepRepository`, `GetSleepContentUseCase` y
   `MockSleepRepository` quedaban sin un solo lector. Se eliminaron los cuatro y `SleepUiState.Success`
   se quedó con el análisis, que es lo único que esa pantalla carga por su cuenta.
7. **La copia visible salió de los Composables.** `HazloSectionDefaults` se quedó sólo con
   dimensiones; los títulos, el "Ver Todos", el "Logro" de la tarjeta de campeón y el "Nuevo" y
   "Progreso" de la de reto pasaron a `strings.xml`. `SectionHeader` es atómico y ya no lleva un
   `"Ver Todos ->"` escrito dentro como valor por defecto.

**Archivos tocados.**

- `core` — nuevos: `domain/model/PillarHighlights.kt`, `domain/repository/PillarHighlightsRepository.kt`,
  `domain/usecase/GetPillarHighlightsUseCase.kt`. Borrados: `domain/model/SleepContent.kt`,
  `domain/repository/SleepRepository.kt`, `domain/usecase/GetSleepContentUseCase.kt`.
- `app/shared` datos — nuevos: `data/repository/SamplePillarHighlights.kt`,
  `data/repository/SamplePillarHighlightsRepository.kt`. Borrado:
  `data/repository/MockSleepRepository.kt`.
- `app/shared` presentación — nuevos: `feature/pillar/presentation/` (estado, ViewModel y factoría).
  Modificados: `feature/sleep/presentation/SleepUiState.kt`, `SleepViewModel.kt`.
- `app/shared` UI — nuevo: `core/ui/components/sections/PillarHighlightsSections.kt`. Modificados:
  `feature/catalog/ui/PillarCatalogScreen.kt`, `feature/sleep/ui/SleepScreen.kt`,
  `feature/main/ui/MainScreen.kt`, `App.kt`, `core/ui/components/atomic/SectionHeader.kt`,
  `core/ui/components/cards/HazloChampionCard.kt`, `HazloChallengeCard.kt`,
  `core/ui/components/sections/HazloChampionsSection.kt`, `HazloChallengesSection.kt`,
  `HazloExploreProductsSection.kt`, `HazloSectionDefaults.kt`,
  `composeResources/values/strings.xml`.
- Tests — nuevos: `core/.../GetPillarHighlightsUseCaseTest.kt`,
  `app/shared/commonTest/.../SamplePillarHighlightsRepositoryTest.kt`,
  `.../PillarHighlightsViewModelTest.kt`, `app/shared/jvmTest/.../PillarBoardOrderTest.kt`.
  Actualizados: `PillarCatalogScreenTest.kt`, `SleepScreenCatalogTest.kt`.
- Especificación: `features/tablero_de_pilares.feature`, `docs/features/tablero-de-pilares.md`.

**Comandos clave.**

```
.\gradlew.bat :core:check :app:shared:jvmTest --console=plain
```

**Resultados de validación.** `BUILD SUCCESSFUL`. `core` 118 pruebas en JVM y 118 en el navegador,
0 fallos; `app/shared` 242 pruebas en JVM, 0 fallos. La compilación de iOS
(`compileTestKotlinIosSimulatorArm64`) también pasa; la ejecución de esas pruebas queda `SKIPPED`
porque hace falta macOS. Primer intento fallido y por qué: Kotlin/Native no admite comas dentro de
un nombre de prueba entre backticks, así que dos nombres del test del caso de uso hubo que
reescribirlos.

**Desviaciones.** El borrado del camino de `SleepContent` no estaba en el roadmap; salió de aplicar
la decisión 1 hasta el final. Se hizo y se reporta, como manda el modo autónomo de `AGENTS.md`.
También se movieron a recursos tres cadenas de las tarjetas de campeón y de reto que no estaban en el
alcance escrito, por la regla de que la copia sale de los Composables en todo lo que se toca.

**Cambios visibles no pedidos, pero derivados.** "Retos Activos" ya no muestra el enlace muerto "Ver
Todos ->" (lo heredaba del valor por defecto de `SectionHeader`). La rejilla del catálogo pasó de
"Todo el catálogo" a "Buscar productos y servicios".

**Seguimiento.**

- Slice 2: `HazloSkeleton` y carga por secciones, para que un catálogo que tarda no impida ver
  campeones y retos. Hoy el tablero sigue esperando al catálogo para dibujarse entero.
- El enlace "Ver Todos" de campeones sigue sin llevar a ninguna parte.
- `HomeScreen` todavía no usa `pillarHighlightsSections` ni tiene retos; entra en el slice 3.
