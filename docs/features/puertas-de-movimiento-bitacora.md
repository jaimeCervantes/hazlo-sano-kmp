# Bitácora — el pilar de Movimiento tiene puerta

## Slice 1 — La pestaña de Movimiento es la puerta del pilar (2026-09-06)

**Objetivo.** Que el pilar con más código construido de los cuatro deje de ser el único cuya
herramienta no se alcanza desde su propia pestaña. Spec:
[`puertas_de_movimiento.feature`](../../features/puertas_de_movimiento.feature), escenarios
`@slice-1`.

### Decisiones y por qué

1. **Un hueco en `PillarCatalogScreen`, no una segunda pantalla.** De los cuatro pilares sólo
   Movimiento tiene herramienta propia; escribir un tablero casi idéntico para él era el fallo de
   diseño que `AGENTS.md` nombra. El hueco va **debajo del resumen y encima de los campeones**: el
   resumen es la identidad del pilar, y lo que se viene a hacer va antes que lo que hace la
   comunidad. Los otros tres no lo llenan y no cambian en nada.
2. **Una acción manda y dos acompañan.** Empezar una salida va rellena y a lo ancho; «Mis rutas» y
   «Mis salidas» son sitios a los que ir, no cosas que hacer ahora, y van delineadas a media anchura.
3. **`MovementNavState` gana una pila, que es más de lo que el roadmap pedía.** El roadmap decía «el
   *volver* de esas dos pantallas devuelve al pilar». Implementarlo literalmente —cambiar un destino
   fijo por otro— habría roto el camino que ya existía: quien entra a «Mis salidas` desde el tracker
   espera volver al tracker. Una pila hace que «volver» signifique lo mismo desde cualquier camino y
   que ninguna pantalla tenga que saber quién la abrió. Volver a un sitio donde ya se estuvo **corta**
   la pila en vez de apilar otra copia, para que ir y venir entre una lista y un detalle no deje
   media docena de pasos que deshacer.

### Lo que se quitó, y lo que casi se pierde con ello

Los carruseles **«Servicios»** y **«Cerca de ti»** salen del tablero (decisión del usuario): la
búsqueda que cierra la pantalla ya enseña lo mismo unas pulgadas más abajo. «Cerca de ti» además
reordenaba por distancia algo que el sitio ya devuelve ordenado por distancia. **«Próximos eventos» se
queda**, porque un evento tiene fecha y caduca, y eso la búsqueda no lo ordena.

Al quitarlos, `CatalogSections.services` y `.nearby` se quedaron sin un solo consumidor y se borraron
con ellos, junto a sus dos cadenas y sus pruebas.

**Y ahí saltó algo que no estaba previsto:** el carrusel era el **único sitio** donde se veía la
línea de encima de una tarjeta —cuánto dura un servicio, cuándo ocurre un evento—. La rejilla de
búsqueda no la pintaba, así que quitar la sección habría hecho desaparecer del app la duración de un
servicio. Lo destapó un test que llevaba tiempo verde (`a service shows how long it lasts`).

En vez de aceptar la pérdida, `HazloExploreProductsSection` gana un parámetro `overlineLabel`: la
búsqueda pasa a enseñar esa línea, que es lo que hacía cierto que «ya cumple con eso». El parámetro
es una función y no un dato porque decidir qué dice esa línea es conocimiento del catálogo, y esa
sección sólo sabe pintar una rejilla.

### El test intermitente, otra vez — y por qué el arreglo anterior estaba mal

`SessionDetailIntegrationTest.openingASessionBringsTheHistorySummaryInLineWithItsRoute` volvió a
fallar, ahora colgándose hasta el límite de un minuto de `runTest`.

**El arreglo del 2026-09-06 por la mañana era incorrecto.** Cambió el test para esperar a que el flujo
del historial volviera a emitir; pero el driver en memoria de los tests **stubbea `addListener`**, así
que `asFlow()` emite **una sola vez** y no vuelve a emitir nunca cuando la tabla cambia. Esa espera no
podía terminar. Pasó tres veces seguidas sólo porque la escritura ganaba la carrera y llegaba antes de
la primera emisión — o sea que el arreglo no quitó la carrera, le dio la vuelta y cambió un fallo
rápido por un cuelgue de un minuto.

**Las tres pasadas limpias que reporté como prueba de que estaba arreglado no probaban eso.** Probaban
que la carrera se seguía ganando.

El arreglo de ahora relee la fila en vez de escuchar el flujo, y espera **en tiempo real sobre un
dispatcher real**: el reloj virtual de `runTest` no gobierna `Dispatchers.Default`, así que un `delay`
en el dispatcher de prueba se saltaría sin dejar avanzar la escritura. La condición es «dejó de valer
lo que valía» y no «vale 390», para que el número esperado viva sólo en la aserción.

### Archivos tocados

- **Catálogo:** `CatalogSections.kt` (fuera `services` y `nearby`), `PillarCatalogScreen.kt` (fuera
  los dos carruseles y sus etiquetas; entra el hueco `pillarActions`; la rejilla pasa a pintar la
  línea de encima), `HazloExploreProductsSection.kt` (`overlineLabel`).
- **Movimiento:** `ui/MovementPillarActions.kt` (nuevo),
  `presentation/MovementNavState.kt` (pila de navegación).
- **Navegación:** `MainScreen.kt` (las cinco pantallas vuelven con `back()`; la pestaña de Movimiento
  pasa el hueco lleno).
- **Recursos:** `movement_start_outing` en los dos idiomas; fuera `catalog_section_services` y
  `catalog_section_nearby` de los dos. Los catálogos quedan en **199 y 199**.
- **Tests:** `MovementNavStateTest` (nuevo, 9), `MovementPillarActionsTest` (nuevo, 4),
  `CatalogSectionsTest` (−4), `PillarCatalogScreenTest` (dos pruebas de ausencia fusionadas en una
  que afirma que las secciones no vuelven), `SessionDetailIntegrationTest` (la espera, reescrita).

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **134 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **330 pruebas, 0 fallos**, verificado con cuatro pasadas
  `--rerun-tasks` **en serie** por lo del test intermitente.
- `.\gradlew.bat :core:check` y `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check` →
  BUILD SUCCESSFUL los tres.

### Sin cobertura de host (dicho explícitamente)

- **Que las tres puertas lleven a donde dicen no lo comprueba ningún test.** Se comprueba que cada una
  avisa al tocarse y que la pila vuelve bien; que `MainScreen` conecte cada aviso con el destino
  correcto se ve abriendo la app.
- **La pila no está probada contra el botón «atrás» del sistema**, que en Android no pasa por
  `MovementNavState`. Es deuda que ya existía y que esta pila hace más visible: hoy el atrás del
  sistema sale del app desde cualquier pantalla del pilar.
- **La disposición de las tres acciones en pantallas estrechas** no la mide nada. Las dos puertas se
  reparten el ancho a medias y su rótulo va a un renglón con elipsis, que es lo que aprendimos de la
  barra inferior; pero en inglés «My outings» y «My routes» son más largas y nadie lo ha mirado.

### Seguimientos

- **El botón «atrás» del sistema** debería recorrer esta pila en Android.
- La pantalla de licencias, pendiente desde el slice 2 del sistema de diseño.

**Recap.** La pestaña de Movimiento deja de ser sólo lo publicado: ofrece empezar una salida, «Mis
rutas» y «Mis salidas», las tres en un toque y sin pasar por Inicio. El tablero no se duplicó — ganó
un hueco que sólo este pilar llena. La navegación pasa a tener memoria, así que volver significa lo
mismo desde cualquier camino. Y el tablero deja de enseñar dos veces lo mismo: se van «Servicios» y
«Cerca de ti», con el cuidado de mudar a la búsqueda la línea que sólo ellos pintaban.

**Próximos pasos (opciones).** (1) El slice 2, «Mis salidas» con el trazado a la vista, que termina de
cablear el `previewPoints` que lleva meses muerto; (2) el botón atrás del sistema, que esta pila deja
a un paso; (3) mirar en el teléfono lo entregado hasta aquí antes de seguir.

---

## Slice 2 — Mis salidas, con el trazado a la vista (2026-09-06)

**Objetivo.** Que una salida se reconozca sin abrirla. La lista eran tres renglones de texto: dos
salidas de la misma distancia por sitios distintos se leían igual. Spec: escenarios `@slice-2`.

### El campo que llevaba meses muriendo en memoria

`MovementSession.previewPoints` existía desde el slice 11 y `SaveSessionUseCase` se molestaba en
muestrear la traza a 200 puntos para llenarlo — pero **no había columna donde guardarlo** y
`toDomain()` lo devolvía siempre vacío. Se calculaba en cada guardado y moría ahí. Este slice cierra
ese circuito.

### Decisiones y por qué

1. **La silueta se guarda, no se deriva**, y eso contradice en apariencia la regla del pilar («las
   cifras se calculan de los puntos cada vez»). Es el mismo caso que `distanceTraveled`, que ya es la
   excepción declarada y por idéntico motivo: **la lista no puede leer todos los puntos de todas las
   salidas** para dibujarse. Y una silueta no es una cifra — congelarla no congela ninguna medición,
   porque no afirma nada medible.
2. **Un `Canvas`, no un mapa.** Veinte `MovementMap` en un `LazyColumn` es una lista inusable, y
   cada uno pediría tiles —o sea red— justo en el pilar que promete funcionar sin ella.
3. **La geometría vive en `presentation`, separada del `Canvas`.** Proyectar, encuadrar y corregir la
   longitud son decisiones que se pueden equivocar; dentro de un `Canvas` sólo se verían mirando la
   pantalla con los ojos entrecerrados. Fuera, se prueban.
4. **La proporción se conserva.** Estirar la silueta hasta llenar el recuadro haría que un ida y
   vuelta en línea recta se viera como un circuito. La forma es lo único que esta silueta comunica,
   así que deformarla es mentir; el recorrido se centra en el eje que le sobra.
5. **La longitud se corrige por el coseno de la latitud media.** Sin eso, un recorrido cuadrado sobre
   el terreno saldría más ancho que alto — a 19.43° el factor es ~0.943, que se nota.
6. **La migración no rellena lo que ya existe.** Las salidas anteriores tienen sus puntos en
   `MovementPointEntity` y se podrían muestrear; no se hace, porque eso es exactamente el «leer todos
   los puntos de todas las salidas» que la columna viene a evitar, sólo que una vez. Enseñan su
   hueco, que es honesto: no se sabe su forma sin abrirlas.
7. **El formato es texto simple** —pares separados por `;`— y no una polilínea codificada. Con 200
   puntos son ~3 KB por salida, que en una tabla que ya guarda cada lectura del GPS no es nada, y a
   cambio la columna se lee a ojo cuando algo va mal. El decodificador **se salta lo que no entiende**
   en vez de estallar: la silueta es decoración y una columna a medio escribir no puede costar el
   historial.

### Archivos tocados

- **Base:** `MovementSession.sq` (columna `previewPoints`), `8.sqm` (nuevo, versión 8 → 9).
- **Datos:** `TrackPreviewFormat.kt` (nuevo), `MovementSessionMapping.kt`,
  `SqlDelightMovementSessionRepository.kt`.
- **Presentación:** `movement/presentation/TrackSilhouette.kt` (nuevo, la geometría),
  `MovementHistoryViewModel` (`SessionListItem` gana su silueta).
- **UI:** `movement/ui/TrackSilhouetteView.kt` (nuevo, el `Canvas` y el hueco),
  `MovementHistoryScreen.kt` (la tarjeta pasa a fila: silueta a la izquierda, datos a la derecha;
  `SessionList` se expone como `OutingList` para poder componerla en un test).
- **Tests:** `TrackSilhouetteTest` (nuevo, 10 — la geometría), `TrackPreviewFormatTest` (nuevo, 5),
  `SessionPreviewMigrationTest` (nuevo, 4), `OutingSilhouetteTest` (nuevo, 3);
  `MovementSessionMigrationTest` y `MovementHistoryViewModelTest` actualizados.

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **134 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **352 pruebas, 0 fallos** (venían 330).
- `.\gradlew.bat :core:check` y `:app:shared:check` → BUILD SUCCESSFUL, incluido
  `verifyCommonMainHazloSanoDatabaseMigration`.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check` →
  BUILD SUCCESSFUL los tres.

### Sin cobertura de host (dicho explícitamente)

- **Nadie ha visto una silueta dibujada.** Un `Canvas` no expone nodos que un test de Compose pueda
  mirar: lo que se comprueba es que cada salida tiene su sitio y que la geometría proyecta bien. Que
  el trazo se vea reconocible a 64 dp se ve en el teléfono, y es lo primero que conviene mirar.
- **El muestreo a 200 puntos no se ha probado contra una traza real larga.** Una salida de dos horas
  son ~3.600 lecturas; que 200 basten para reconocerla es una apuesta razonable, no una medición.
- **La columna crece con cada salida** y nadie ha medido la tabla en un teléfono con meses de uso.

### Un detalle de proceso

Un test que escribí para la tolerancia del decodificador salió mal —lo monté contra la base de datos
con andamiaje que no hacía nada— y se retiró antes de commitear. Esa propiedad se prueba mejor sobre
el formato directamente, que es donde vive: `TrackPreviewFormatTest`.

**Recap.** «Mis salidas» deja de ser tres renglones de texto: cada salida enseña la forma de su
recorrido a la izquierda, dibujada con `Canvas` y sin pedirle nada a la red. El `previewPoints` que
llevaba desde el slice 11 calculándose y muriendo en memoria tiene por fin columna, y las salidas
anteriores enseñan su hueco en vez de un trazado inventado.

**Próximos pasos (opciones).** (1) El slice 3, empezar una salida desde cero o siguiendo una ruta, que
es la otra mitad de lo que pediste; (2) mirar en el teléfono que las siluetas se reconozcan a 64 dp;
(3) el botón atrás del sistema, que sigue anotado desde el slice 1.
