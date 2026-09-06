# Feature: el pilar de Movimiento tiene puerta

Roadmap de slices. Escrito el **2026-09-05**.
Spec: [`features/puertas_de_movimiento.feature`](../../features/puertas_de_movimiento.feature).

Va **después** de [`sistema-de-diseno.md`](sistema-de-diseno.md), por decisión del usuario
(2026-09-05): estas pantallas se construyen ya con la paleta y la tipografía nuevas, en vez de nacer
con la vieja y repintarse un slice más tarde.

## Problema / Savings / Why

- **Problema:** el pilar con más código construido de los cuatro —16 slices— es el único cuya
  herramienta no se alcanza desde su propia pestaña. `MainScreen.kt:249` manda `BottomTab.Movimiento`
  directo a `PillarCatalogScreen`, y su propio comentario lo admite: *«el tracker se sigue alcanzando
  desde Inicio, que es donde estaba»*. Para abrir una ruta guardada hay que ir a **Inicio → tarjeta →
  Tracker → Historial → Mis rutas**: cuatro toques, y el tercero es una pantalla que no tiene nada
  que ver con rutas. Y una vez dentro, las salidas grabadas se listan como tres renglones de texto:
  no hay forma de reconocer *cuál* fue cada una sin abrirlas de una en una.
- **Savings:** cuatro toques y un desvío por Inicio se convierten en uno. Reconocer una salida pasa
  de abrir cinco pantallas a mirar la lista. Y salir a seguir una ruta deja de exigir que te acuerdes
  de por dónde se llegaba.
- **Why:** «recorrer rutas» es la promesa central del pilar según el propio backlog de migración. Hoy
  el app sabe importar un GPX, guardarlo, dibujarlo y exportarlo — y no sabe llevarte a él.

## Lo que se comprobó antes de planificar

- **La puerta no está escondida: no existe.** `BottomTab.Movimiento` no ofrece ninguna acción del
  pilar. El único acceso al tracker es `onNavigateToTracker` desde `HomeScreen`.
- **La navegación está encadenada al revés.** En `MainScreen`, el *volver* de Historial va al
  Tracker, y el de Rutas va a Historial. Rutas cuelga de Historial, que es una pantalla con la que no
  tiene relación.
- **El preview lleva meses a medio construir.** `MovementSession` ya declara `previewPoints`, y
  `SaveSessionUseCase` se molesta en muestrear la traza a 200 puntos para llenarlo — pero
  `MovementSession.sq` no tiene esa columna, `SqlDelightMovementSessionRepository` no lo guarda, y
  `MovementSessionMapping.toDomain()` lo devuelve siempre vacío. Es un campo que se escribe en
  memoria y muere ahí.
- **`MovementSessionEntity.routeId` guarda `null` en todas las sesiones.** La columna existe y la
  tabla a la que apunta también; nada ata todavía una grabación a la ruta que iba siguiendo.
- **El proyecto hermano ya resolvió la forma.** Su doc `034-panel-detalle-tienda-mapa` fija el patrón
  de casa para un mapa con detalle: *«en desktop, panel lateral flotante; en móvil, hoja inferior
  superpuesta»*, sin empujar el contenido ni secuestrar la navegación. Es exactamente el patrón al
  que han convergido las apps de rutas, y no hace falta inventarlo: ya es nuestro.

## Decisiones tomadas antes de empezar

1. **`PillarCatalogScreen` no se duplica.** Gana un hueco opcional para las acciones del pilar, que
   sólo Movimiento llena. Escribir una segunda pantalla casi igual es el fallo de diseño que
   `AGENTS.md` nombra por su nombre.
2. **El trazado de la lista se dibuja con `Canvas`, no con un mapa.** Veinte instancias de
   `MovementMap` en un `LazyColumn` es una lista inusable, y además necesitaría tiles —o sea, red—
   justo en el pilar que promete funcionar sin ella. La silueta del recorrido basta para reconocer
   una salida, y es lógica pura y multiplataforma.
3. **El preview se guarda, no se deriva.** Contradice en apariencia la regla del pilar («las cifras
   se derivan de los puntos»), pero es el mismo caso que `distanceTraveled`, que ya es la excepción
   declarada y por el mismo motivo: **la lista no puede leer todos los puntos de todas las
   sesiones**. Y un preview no es una cifra: es una silueta, así que congelarla no congela ninguna
   medición.
4. **Seguir una ruta no avisa de desvíos.** El aviso de desvío es C3 del backlog de migración —
   proyección sobre el trazado, distancia mínima, *smart snap*— y es una feature propia. Aquí la ruta
   se carga, se pinta y se graba encima; nada más. Decisión del usuario (2026-09-05).
5. **"Historial" pasa a llamarse "Mis salidas".** Decisión del usuario (2026-09-05). Emparejado con
   "Mis rutas", que ya existe: **rutas** es lo que puedes seguir, **salidas** es lo que ya hiciste.
6. **Los carruseles "Servicios" y "Cerca de ti" desaparecen** del tablero de pilar. Decisión del
   usuario (2026-09-05): la sección "Buscar productos y servicios" que cierra la pantalla ya cubre
   las dos, así que son dos formas más de enseñar lo mismo unas pulgadas más arriba. Se quitan en el
   slice 1, que es cuando `PillarCatalogScreen` se toca para abrirle hueco a las acciones del pilar.
   **"Próximos eventos" se queda**: un evento tiene fecha y caduca, y eso la búsqueda no lo ordena.

## Slices

### Slice 1 — La pestaña de Movimiento es la puerta del pilar

Escenarios `@slice-1`.

**Alcance:**

- `PillarCatalogScreen` gana un parámetro opcional para las acciones del pilar, colocado entre el
  resumen y los campeones. Nutrición y Mente no lo pasan y no cambian.
- `feature/movement/ui/MovementPillarActions.kt` (nuevo): la acción primaria —empezar una salida— y
  las dos puertas secundarias, mis rutas y mis salidas.
- `MainScreen`: `BottomTab.Movimiento` pasa el hueco lleno; `MovementNavState` gana `close()` hacia
  el pilar, y el *volver* de Mis salidas y de Mis rutas vuelve **al pilar**, no al tracker.
- Recursos nuevos: `movement_start_outing`, `movement_my_routes`, `movement_my_outings`.
- Se borran los carruseles "Servicios" (`PillarCatalogTags.SERVICES`) y "Cerca de ti"
  (`PillarCatalogTags.NEARBY`) de `pillarCatalogSections`, con sus cadenas y sus pruebas. "Próximos
  eventos" se queda.

**Criterios de aceptación:**

- Desde la pestaña de Movimiento se empieza una salida en un toque, sin pasar por Inicio.
- Desde la pestaña de Movimiento se abren "Mis rutas" y "Mis salidas" en un toque.
- El *volver* de esas dos pantallas devuelve a la pestaña del pilar.
- Nutrición y Mente conservan su tablero; lo único que cambia en ellas es que pierden los dos
  carruseles redundantes.
- Ningún tablero de pilar enseña ya "Servicios" ni "Cerca de ti"; "Próximos eventos" sigue.
- Ninguna de las etiquetas nuevas queda escrita dentro de un `@Composable`.

**Tests:** Compose UI test en `jvmTest` afirmando por `testTag` que el tablero de Movimiento trae las
tres acciones y el de Nutrición ninguna; test de `MovementNavState` para el retorno al pilar.

### Slice 2 — Mis salidas, con el trazado a la vista

Escenarios `@slice-2`. `previewPoints` deja de ser un campo muerto: migración `7.sqm` que añade la
columna, el repositorio la escribe y la lee, y la tarjeta de cada salida dibuja la silueta del
recorrido con `Canvas`. La pantalla pasa a llamarse "Mis salidas". Una salida sin puntos guardados
—las grabadas antes de esta migración— enseña su hueco en vez de un recuadro vacío.

### Slice 3 — Empezar una salida: desde cero, o siguiendo una ruta

Escenarios `@slice-3`. La pantalla de empezar adopta el patrón de casa: mapa a pantalla completa con
una hoja inferior. Dos puertas — empezar desde cero, o elegir una ruta guardada (o importar un GPX en
el momento). La ruta elegida se pinta bajo tu trazado real mientras grabas, y
`MovementSessionEntity.routeId` deja por fin de guardar `null`. Sin avisos de desvío: eso es C3.

### Slice 4 — Las rutas se ven como en las apps del ramo

Escenarios `@slice-4`. `RoutesScreen` y `RouteDetailScreen` pasan al mismo patrón: el detalle es mapa
con hoja inferior, la lista enseña la silueta de cada ruta junto a sus cifras, y el estado vacío
ofrece la acción en vez de sólo explicarla. Aquí cae también la deuda de i18n de `RoutesScreen`, si
el slice 4 del sistema de diseño no la ha cerrado ya.

## Fuera de alcance

- **C3, la navegación guiada con aviso de desvío.** Sigue en
  [`movement-tracking-migration.md`](movement-tracking-migration.md) como el punto grande del pilar.
- **Mapas offline (D) y estilo satélite (E).** Se notan más en estas pantallas, pero son puntos
  propios del backlog.
- **El dashboard de Movimiento (F).** Este roadmap le da al pilar su portada, que es lo que F
  pretendía; al cerrarlo habrá que releer F y ver qué le queda.
