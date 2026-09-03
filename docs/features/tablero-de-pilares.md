# Feature: el tablero de un pilar se arma por secciones

Roadmap de slices. Escrito el **2026-08-30**.
Spec: [`features/tablero_de_pilares.feature`](../../features/tablero_de_pilares.feature).
Referencia de diseño: proyecto Android en `C:\Users\S2G52\AndroidStudioProjects\HazloSano`.

## Problema / Savings / Why

- **Problema:** una sola espera de red apaga la pantalla entera. `PillarCatalogScreen.kt:118` pinta
  un `CircularProgressIndicator` centrado sobre fondo vacío mientras responde el endpoint de Next.js;
  `HomeScreen.kt:89` y `SleepScreen.kt:82` hacen lo mismo con su `LoadingContent`. Lo que ya está en
  memoria se esconde detrás de lo único que falta. Y el tablero de pilar no se parece al del proyecto
  Android: allí el orden es campeones → retos → búsqueda, aquí son métricas y carruseles.
- **Savings:** tiempo percibido de carga y frustración. La estructura aparece de inmediato; con mala
  señal se sigue usando lo que no depende de ella.
- **Why:** la app se está mudando a contenido remoto. Sin estado de carga por sección, cada endpoint
  que se sume vuelve a apagar la pantalla entera.

## Estado del que se parte

| Pantalla | Hoy | Carga |
|---|---|---|
| Nutrición / Movimiento / Mente | `PillarCatalogScreen`: resumen de métricas + carruseles (eventos, servicios, cerca) + rejilla "Todo el catálogo" | ruedita a pantalla completa |
| Sueño | resumen de anoche + campeones + retos + secciones del catálogo | ruedita a pantalla completa; el catálogo ya carga aparte |
| Inicio | resumen de sueño + pilares + campeones + feed | ruedita a pantalla completa |

`HazloChampionsSection`, `HazloChallengesSection` y `HazloExploreProductsSection` ya están portados en
`core/ui/components/sections/`. El componente existe; lo que falta es usarlo en el orden de la
referencia y darle de dónde leer.

## Decisiones tomadas antes de empezar

1. **Campeones y retos entran con contenido de muestra por pilar** (decisión del usuario, 2026-08-30).
   Se toman los valores del proyecto Android —`MovementDashboardScreen.kt`,
   `NutritionDashboardScreen.kt`, `MindDesign.kt`— y los de Sueño que ya existen en
   `MockSleepRepository`. No se inventan datos nuevos.
2. **Una sola fuente para los cuatro pilares.** `MockSleepRepository` deja de tener campeones y retos:
   se **mueven**, no se copian. Dos fuentes para lo mismo es el fallo de diseño que `AGENTS.md` nombra.
3. **La fuente entra por una interfaz de `core`**, con implementación de muestra en `app/shared`. El
   día que el backend los publique se cambia la implementación y ninguna pantalla se entera.
4. **El resumen del pilar se queda arriba.** Es la identidad del pilar y son datos reales del
   catálogo; hace de héroe, como el hero card de la referencia.
5. **Los carruseles del catálogo se quedan**, entre los retos y la búsqueda. Son contenido real que la
   referencia no tenía; la búsqueda sigue siendo lo último, como allí.
6. **La copia visible pasa al catálogo de recursos.** `HazloSectionDefaults` se queda con dimensiones.

## Slices

### Slice 1 — El orden del tablero: campeones, retos, buscar

Escenarios `@slice-1`.

**Alcance:**

- `core`: `PillarHighlights` (modelo), `PillarHighlightsRepository` (interfaz),
  `GetPillarHighlightsUseCase`.
- `app/shared` datos: `SamplePillarHighlightsRepository` con el contenido de muestra de los cuatro
  pilares. `MockSleepRepository` pierde `weeklyChampions` y `activeChallenges`.
- `app/shared` presentación: `PillarHighlightsViewModel` + estado, en `feature/pillar/presentation/`,
  usado por el tablero de pilar y por Sueño.
- `app/shared` UI: `PillarCatalogScreen` reordena a resumen → campeones → retos → carruseles →
  "Buscar productos y servicios"; `SleepScreen` lee sus campeones y retos del mismo sitio.
- Recursos: `section_weekly_champions`, `section_active_challenges`, `section_view_all`,
  `catalog_section_search` (sustituye a "Todo el catálogo"), `section_products_placeholder`,
  `section_products_empty`.

**Criterios de aceptación:**

- Abrir Nutrición, Movimiento o Mente enseña las secciones en el orden de la tabla del escenario 1.
- Cada pilar trae campeones y retos propios, con su acento.
- Sueño conserva el resumen de anoche arriba y no duplica campeones.
- Una sección sin datos no se pinta con el encabezado vacío.
- Ningún título de sección queda escrito dentro de un `@Composable`.

**Tests:** unitarios del caso de uso y del repositorio de muestra (`commonTest`); test de
`PillarHighlightsViewModel`; test de Compose UI en `jvmTest` que afirma el orden por `testTag`.

### Slice 2 — Esqueletos por sección

Escenarios `@slice-2`. `HazloSkeleton` atómico (sin conocimiento de dominio) más los esqueletos de
resumen, carrusel y rejilla. `PillarCatalogScreen` deja de tener estado de carga a pantalla completa:
campeones y retos ya visibles, esqueleto donde falta el catálogo. Reintentar no apaga lo que ya
estaba.

### Slice 3 — Inicio y Sueño cargan por partes

Escenarios `@slice-3`. Desaparecen los `LoadingContent` de `HomeScreen` y `SleepScreen`: pilares,
campeones, feed y resumen de anoche aparecen cada uno cuando está listo, con su propio esqueleto.

### Slice 4 — La pantalla del pilar

Escenarios `@slice-4`. Desde el resumen de cada pilar se abre una pantalla con qué es ese pilar y qué
propone, con el contenido de <https://hazlosano.com/pilares>.

## Seguimientos del cierre (slices 5-8)

Añadidos el **2026-09-02**, tras aprobación del usuario. Son los cuatro seguimientos que la bitácora
del slice 4 dejó anotados. Cada uno es un cambio de comportamiento independiente; comparten rama y
roadmap por ser continuación directa de la misma feature, no porque dependan entre sí.

### Slice 5 — La copia de `HazloTopAppBar` sale del Composable

Escenarios `@slice-5`.

**Problema:** "Volver", "Perfil", "Notificaciones" y "Menú" están escritos dentro de un componente
`core/ui/components/atomic/`, que por regla no puede llevar copia dentro ni leer recursos. Es el
último atómico con texto en duro.

**Alcance:**

- `HazloTopAppBar` cambia sus cuatro `contentDescription` fijos por parámetros
  (`backContentDescription`, `profileContentDescription`, `notificationsContentDescription`,
  `menuContentDescription`), nulos por defecto para no forzar a quien no muestra ese icono.
- Recursos nuevos: `top_app_bar_back`, `top_app_bar_profile`, `top_app_bar_notifications`,
  `top_app_bar_menu`.
- Los siete llamadores (`MainScreen`, `PillarInfoScreen`, `MovementHistoryScreen`, `TrackerScreen`,
  `RoutesScreen`, `SessionDetailScreen`, `RouteDetailScreen`) pasan `backContentDescription` desde el
  catálogo; `MainScreen`, que es el único que enseña perfil/notificaciones/menú, pasa los cuatro.

**Criterios de aceptación:**

- Ningún `contentDescription` de `HazloTopAppBar` queda escrito dentro del archivo.
- El valor que llega a cada icono es exactamente el que trae el parámetro.

**Tests:** Compose UI test de `HazloTopAppBar` en `jvmTest`, uno por icono.

### Slice 6 — `BottomTab` deja de duplicar el nombre y el icono del pilar

Escenarios `@slice-6`.

**Problema:** `BottomTab` lleva los cuatro nombres, iconos y colores de pilar repetidos en duro, pese
a que `pillarLabel`, `pillarIcon` y `PillarType.toColor()` ya son la fuente única desde el slice 4.

**Alcance:**

- `BottomTab` pasa a guardar un `PillarType?` (`null` para Inicio) en vez de `label`/`icon`/`color`
  propios.
- La pestaña resuelve su etiqueta, icono y color desde `PillarVisuals` cuando lleva pilar, y desde un
  recurso nuevo (`bottom_tab_home`) y `HazloSanoGreen`/`Icons.Filled.Home` cuando no.

**Criterios de aceptación:**

- El nombre, el icono y el color de cada pestaña de pilar coinciden con los que usa su propio tablero.
- No queda un segundo mapa `PillarType -> nombre/icono` en el archivo.

**Tests:** test de Compose UI en `jvmTest` que compara la pestaña con `pillarLabel`/`pillarIcon`.

### Slice 7 — Reintentar en Inicio y en Sueño cuando fallan

Escenarios `@slice-7`.

**Problema:** `HomeViewModel` no tiene forma de reintentar; `SleepViewModel.refresh()` sólo actúa si
ya había cargado con éxito. Un fallo sólo se dice, nunca se puede reintentar sin salir de la pantalla.

**Alcance:**

- `HomeViewModel` gana `refresh()`, reutilizando la misma carga que corre en `init`.
- `SleepViewModel.refresh()` distingue: si el estado es `Failed`, vuelve a cargar como al abrir la
  pantalla; si es `Success`, conserva el comportamiento actual (actualización silenciosa).
- `HomeBoard` y `SleepDashboardContent` añaden un botón "Reintentar" bajo su aviso de fallo.
- El texto "Reintentar" se **unifica**: `catalog_retry` se renombra a `action_retry` y lo usan los
  tres sitios (catálogo, Inicio, Sueño) en vez de que Inicio y Sueño hereden una tercera copia del
  mismo valor.

**Criterios de aceptación:**

- Un Inicio o un Sueño que fallaron ofrecen un botón que dispara una nueva carga.
- El botón usa el mismo recurso que ya usa el catálogo para "Reintentar".

**Tests:** unitarios de `HomeViewModel`/`SleepViewModel` (el estado pasa de `Failed` a `Success` tras
`refresh()`); Compose UI test del botón en `HomeBoardTest`/`SleepDashboardLoadingTest`.

### Slice 8 — Otra puerta a la explicación del pilar sin catálogo

Escenarios `@slice-8`.

**Problema:** la única puerta a `PillarInfoScreen` vive en la tarjeta de resumen del pilar, y esa
tarjeta no se pinta sin catálogo (`PillarBoardInfoTest`, "a board with no catalogue yet offers no
summary card to open it from"). Sin red, no hay forma de leer qué es ese pilar.

**Alcance:**

- `CatalogMessage` (ya usado por los estados `Unavailable` y `Failed` del catálogo, en el tablero de
  pilar y en Sueño) gana un botón "Qué es este pilar" junto al de reintentar, con el mismo recurso
  (`pillar_info_open`) que ya usa la tarjeta de resumen.
- `pillarCatalogPlaceholder` recibe `onOpenInfo` y lo pasa a `CatalogMessage`; `PillarCatalogContent`
  y `SleepDashboardContent` reutilizan el `onOpenInfo` que ya reciben.

**Criterios de aceptación:**

- Sin catálogo (`Unavailable` o `Failed`), el pilar y Sueño enseñan una puerta a la explicación aunque
  no haya tarjeta de resumen.
- Esa puerta abre la misma `PillarInfoScreen` que la de la tarjeta.

**Tests:** Compose UI test que añade el caso que `PillarBoardInfoTest` dejó marcado como pendiente.

## Fuera de alcance

- "Lugares destacados" de Nutrición (decisión del usuario, 2026-08-30).
- Publicar campeones y retos reales desde el backend: entra cuando exista el endpoint, cambiando la
  implementación del repositorio.
