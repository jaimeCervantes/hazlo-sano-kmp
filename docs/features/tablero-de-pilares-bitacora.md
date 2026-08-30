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

## Slice 2 — Cada sección responde de su propia espera (2026-08-30)

**Objetivo.** Que una espera de red deje de apagar la pantalla entera: lo que ya está se enseña, y
sólo lo que falta muestra su hueco.

**Decisiones y por qué.**

1. **Se fue el `when` que elegía entre ruedita y tablero.** `PillarCatalogContent` es ahora un solo
   `LazyColumn` en el que el resumen, la comunidad y el catálogo deciden por separado si se pintan o
   enseñan su hueco. Ese `when` era la causa del problema: con mala señal escondía también lo que no
   dependía de la red.
2. **El esqueleto late.** Un rectángulo gris inmóvil se lee como un hueco roto; el pulso es lo que
   dice "esto viene en camino". `HazloSkeleton` vive en `atomic/` porque no sabe qué está esperando:
   recibe tamaño y forma por el `modifier`.
3. **Las medidas del hueco imitan a lo que llega** (resumen 160dp, tarjeta de producto 228dp,
   rejilla de dos columnas con 16dp entre ellas). Un esqueleto más bajo que su contenido hace saltar
   la pantalla justo cuando la persona empezó a leer, que es peor que no haber puesto nada.
4. **El hueco del catálogo pinta un solo carrusel**, aunque el tablero pueda traer tres. Reservar
   sitio para secciones que quizá no existan —un pilar sin eventos no pinta "Próximos eventos"—
   prometería más de lo que va a llegar.
5. **`pillarCatalogPlaceholder` perdió su parámetro `accent`**: sólo lo usaba la ruedita. Y ahora lo
   comparten de verdad las dos pantallas, así que el hueco del catálogo es el mismo en el tablero y
   en Sueño.
6. **El `when` sobre el estado de los campeones se extrajo a `feature/pillar/ui/pillarHighlights`**,
   para no repetirlo en las dos pantallas. Las dos mitades que son puro dibujo —las secciones y su
   esqueleto— se quedan en `core/ui/components/sections/`, sin saber de dónde salen.
7. **Se borró el estado de carga muerto de `HazloExploreItemsSection`:** un `isLoading` con un
   "Buscando productos..." escrito en duro y un `error`, dos parámetros que ningún llamante encendía
   nunca. Era una segunda forma de esperar, peor que el esqueleto y sin traducción posible.

**Archivos tocados.**

- Nuevos: `core/ui/components/atomic/HazloSkeleton.kt`,
  `core/ui/components/sections/PillarBoardSkeleton.kt`, `feature/pillar/ui/PillarHighlightsBoard.kt`,
  `app/shared/src/jvmTest/.../PillarBoardSkeletonTest.kt`.
- Modificados: `feature/catalog/ui/PillarCatalogScreen.kt` (desaparece `PillarBoard`, entra
  `pillarSummary`), `feature/sleep/ui/SleepScreen.kt`,
  `core/ui/components/sections/HazloExploreItemsSection.kt`, `HazloExploreProductsSection.kt`,
  `app/shared/src/jvmTest/.../PillarBoardOrderTest.kt`, `features/tablero_de_pilares.feature`.

**Comandos clave.**

```
.\gradlew.bat :app:shared:jvmTest --console=plain
.\gradlew.bat :core:check --console=plain
```

**Resultados de validación.** `BUILD SUCCESSFUL` en ambos. `app/shared`: 247 pruebas en JVM, 0
fallos (eran 242 al cerrar el slice 1). `core` sin cambios y en verde.

**Desviaciones.** Una prueba que quería comprobar que el hueco mide lo mismo que la tarjeta que lo
sustituye se descartó: habría necesitado dos `setContent` en la misma prueba, y Compose sólo admite
uno. Lo que sí se comprueba es la forma de la rejilla —dos columnas, misma fila, mismo ancho—, que es
la parte que de verdad se puede romper sin darse cuenta.

**Nota de pruebas para el futuro.** El esqueleto late con una animación infinita, y eso deja al reloj
de prueba de Compose sin quedarse quieto nunca. Cualquier prueba que componga un hueco tiene que
empezar con `mainClock.autoAdvance = false`, o la espera de inactividad no termina.

**Seguimiento.**

- Slice 3: `HomeScreen` y `SleepScreen` todavía se apagan enteras con su `LoadingContent` mientras
  cargan lo suyo. El mecanismo ya está construido; falta aplicarlo ahí.
- Al reintentar, el resumen y el catálogo vuelven a su hueco en vez de conservar lo ya leído mientras
  llega lo nuevo. Es lo que pide el escenario, pero conservar lo viejo con un aviso de "actualizando"
  sería mejor cuando el catálogo ya se había leído una vez.

**Recap.** El tablero de pilar ya no tiene un estado de carga a pantalla completa: campeones y retos
—que no dependen de la red— se ven mientras el catálogo llega, y cada hueco tiene la forma de lo que
va a ocupar. Las dos pantallas que pintan catálogo comparten ese hueco.

**Próximos pasos (opciones).**

1. Slice 3: Inicio y Sueño por partes, quitando sus dos `LoadingContent`.
2. Slice 4: la pantalla de información del pilar, con el contenido de hazlosano.com/pilares.
3. Conservar lo ya leído durante un reintento, en vez de volver al hueco.

## Slice 3 — Inicio y Sueño cargan por partes (2026-08-30)

**Objetivo.** Quitar los dos últimos estados de carga a pantalla completa: los `LoadingContent` de
`HomeScreen` y `SleepScreen`.

**Decisiones y por qué.**

1. **El resumen de anoche va fuera del estado de Inicio.** No lo carga esa pantalla: llega ya
   calculado desde el pilar de sueño. Que Inicio esté esperando lo suyo no es razón para esconderlo.
2. **Los huecos de Inicio viven en su pantalla** (`feature/home/ui/HomeSkeleton.kt`): la rejilla de
   pilares y el feed son de Inicio y de nadie más. El de campeones sí se reusa —es el mismo que el
   del tablero de pilar—, y el hueco de título se promovió a `HazloSectionTitleSkeleton` porque lo
   querían dos pantallas.
3. **Una noche sin datos no deja hueco.** No es que esté cargando: es que todavía no hay noche que
   contar, y un hueco eterno haría creer lo contrario. Por eso la pantalla necesita el estado entero
   y no sólo el análisis nulable.
4. **`Error(message: String)` pasó a ser `Failed`** en Inicio y en Sueño. Un ViewModel no redacta
   copia: el `message` de una excepción no es texto que nadie quiera leer y encima no se traduce. La
   palabra ahora sale de `strings.xml`.
5. **Un fallo ya no vacía la pantalla:** se dice en una fila, y lo que no dependía de ese fallo
   —la tarjeta de anoche, los campeones, el catálogo— sigue en su sitio.

**Archivos tocados.** Nuevos: `feature/home/ui/HomeSkeleton.kt`,
`app/shared/src/jvmTest/.../HomeBoardTest.kt`, `.../SleepDashboardLoadingTest.kt`. Modificados:
`feature/home/ui/HomeScreen.kt` (entra `HomeBoard`, se va `HomeContent` privada y la `HeaderSection`
muerta), `feature/home/presentation/HomeUiState.kt`, `HomeViewModel.kt`,
`feature/sleep/ui/SleepScreen.kt`, `feature/sleep/presentation/SleepUiState.kt`, `SleepViewModel.kt`,
`core/ui/components/sections/PillarBoardSkeleton.kt`, `composeResources/values/strings.xml`,
`app/shared/src/jvmTest/.../SleepScreenCatalogTest.kt`, `features/tablero_de_pilares.feature`.

**Comandos clave y validación.** `.\gradlew.bat :app:shared:jvmTest --console=plain` →
`BUILD SUCCESSFUL`. Este slice suma 8 pruebas nuevas (4 de Inicio y 4 de Sueño) a las 247 con las que
cerró el slice 2.

**Desviaciones.** Se borró `HeaderSection`, una composable privada de Inicio que no llamaba nadie.

**Seguimiento.** Ni Inicio ni Sueño ofrecen reintentar cuando fallan: sólo lo dicen. `HomeViewModel`
no tiene `refresh()`, y el de sueño sólo recarga si ya había cargado bien.

## Slice 4 — La pantalla del pilar (2026-08-30)

**Objetivo.** Que desde cada pilar se pueda leer qué es ese pilar y qué propone, con el contenido de
<https://hazlosano.com/pilares>.

**Decisiones y por qué.**

1. **Sin repositorio, sin ViewModel y sin modelo de dominio.** Es contenido editorial fijo: no hay
   nada que ir a buscar. Vive en `strings.xml`, que además es lo único traducible —un modelo en
   `core` habría alejado la copia del único sitio donde una traducción la alcanza—. La pantalla mapea
   pilar → recursos en un solo `when`, como ya hacía `pillarLabel`.
2. **La puerta está en la tarjeta de resumen, junto a actualizar.** Es la pregunta que se hace quien
   abre la pestaña por primera vez; escondida en el título se habría quedado sin encontrar. Los
   cuatro pilares la tienen: `PillarSummaryCard` para tres, `SleepSummaryCard` para Sueño.
3. **Se unificó cómo se ve un pilar.** Al querer el nombre y el icono una tercera pantalla salieron
   dos duplicados que `AGENTS.md` llama fallo de diseño: `pillarColor(pillar)` era lo mismo que
   `PillarType.toColor()`, y `pillarIcon` estaba dos veces, una de ellas privada dentro de
   `HomeScreen`. Los tres —nombre, color e icono— se fusionaron en
   `core/ui/model/PillarVisuals.kt`, que es lo que la propia nota de `PillarText.kt` decía que había
   que hacer en cuanto una segunda pantalla los quisiera. Fue mover y fusionar, sin dejar copia.

**Archivos tocados.** Nuevos: `feature/pillar/ui/PillarInfoScreen.kt`,
`core/ui/model/PillarVisuals.kt`, `app/shared/src/jvmTest/.../PillarInfoScreenTest.kt`,
`.../PillarBoardInfoTest.kt`. Borrados: `core/ui/model/PillarColor.kt`,
`feature/catalog/ui/PillarText.kt`. Modificados: `feature/catalog/ui/PillarSummaryCard.kt`,
`PillarCatalogScreen.kt`, `core/ui/components/atomic/SleepSummaryCard.kt`,
`feature/sleep/ui/SleepScreen.kt`, `feature/main/ui/MainScreen.kt`, `feature/home/ui/HomeScreen.kt`,
`composeResources/values/strings.xml`, `features/tablero_de_pilares.feature`.

**Comandos clave y validación.**

```
.\gradlew.bat :app:shared:jvmTest :core:check --console=plain
```

`BUILD SUCCESSFUL`. `app/shared`: 263 pruebas en JVM, 0 fallos. `core`: 118 en JVM y 118 en el
navegador, 0 fallos.

**Desviaciones.** La fusión de nombre/color/icono del pilar no estaba en el roadmap; salió de que el
slice pedía esos tres en una tercera pantalla y de la regla de no dejar un segundo componente casi
idéntico.

**Nota sobre el historial.** Los slices 3 y 4 van en un solo commit de código. Al fusionar
`PillarVisuals` y al añadir `onOpenInfo` a las dos pantallas de pilar, los archivos de los dos slices
quedaron encadenados: separarlos habría dejado un commit intermedio que no compila, que es peor que
un commit que hace dos cosas.

**Recap.** El diseño del tablero está completo de punta a punta: los cuatro pilares se leen en el
orden del proyecto de referencia, ninguna espera apaga una pantalla entera —cada sección enseña su
hueco— y desde cada pilar se llega a qué es ese pilar, con el texto del sitio.

**Próximos pasos (opciones).**

1. Dar otra puerta a la explicación del pilar cuando no hay catálogo que dibuje la tarjeta.
2. Sacar la copia de `HazloTopAppBar` al catálogo de recursos: es el último atómico con texto dentro.
3. Unificar `BottomTab` con `pillarLabel`, ahora que vive en `core/ui/model/`.
4. Ofrecer reintentar en Inicio y en Sueño cuando fallan, no sólo decirlo.

**Seguimiento.**

- Sin catálogo no hay tarjeta de resumen, y con ella se va también la puerta a la explicación del
  pilar. Falta otra entrada para cuando no hay red.
- `HazloTopAppBar` lleva "Volver", "Perfil", "Notificaciones" y "Menú" escritos dentro, y es un
  componente atómico: esa copia debería entrar por parámetro. Es la deuda de i18n que queda viva.
- `BottomTab` sigue con los cuatro nombres de pilar en duro; ahora que `pillarLabel` está en `core`,
  unificarlo es un paso corto.
