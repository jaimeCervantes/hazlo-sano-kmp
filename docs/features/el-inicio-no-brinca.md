# El Inicio no brinca mientras carga

Spec: [`el_inicio_no_brinca.feature`](../../features/el_inicio_no_brinca.feature)

## Encuadre

- **Problem:** abrir el app son tres segundos de empujones. El resumen de anoche se pinta dentro de
  un `if (sleepAnalysis != null)` sin rama else, así que mientras se lee de la base de datos **no
  existe ni el hueco**, y al llegar empuja hacia abajo los pilares, los campeones y el feed. Lo mismo
  con las publicaciones, que entran de golpe donde sólo había una ruedita.
- **Savings:** dejar de tocar lo que no era. Una lista que se mueve bajo el dedo hace pulsar la
  tarjeta de al lado, y obliga a esperar sin saber cuánto porque nada dice que venga algo.
- **Why:** es lo primero que se ve del app. Una pantalla que se recoloca sola tres veces antes de
  quedarse quieta se lee como algo a medio hacer, por muy terminado que esté lo que enseña.

## Lo que se encontró al mirar, y reencuadra el problema

**Los esqueletos que ya existen cuelgan de la espera equivocada.** `homePillarsSkeleton`,
`pillarHighlightsSkeleton` y `homeFeedSkeleton` se pintan cuando `HomeUiState` es `Loading`. Ese
estado sale de `MockHomeRepository.getHomeContent()`: una función `suspend` sin I/O y sin `delay`,
que se resuelve en un frame. **Prácticamente no se ven nunca.**

Y las dos esperas que sí duran no tienen ninguno:

| Espera real | Qué se ve mientras | Qué pasa al llegar |
|---|---|---|
| El análisis de anoche (SQLDelight) | nada, ni hueco | aparece la tarjeta y empuja todo lo de abajo |
| Las publicaciones (red) | una ruedita de 48 dp al pie | entra la lista entera de golpe |

**Tres hallazgos más, del mismo mirar:**

1. **`null` en el análisis significa una sola cosa.** `GetSleepAnalysisUseCase` devuelve un
   `SleepAnalysis` con ceros cuando no hay sesiones, nunca `null`. El nulo aparece sólo al aplanar
   `SleepUiState` a `SleepAnalysis?` para pasarlo por `MainScreen`, y con él se pierde la diferencia
   entre «todavía no» y «no se pudo». Reservar el sitio es correcto porque la tarjeta siempre acaba
   llegando; lo que hay que recuperar es esa diferencia.
2. **El esqueleto del feed tiene la forma equivocada**, además de la espera equivocada: dibuja dos
   tarjetas de ancho completo de 320 dp, y el feed va en dos columnas desde la segunda pasada del
   slice 1 de publicaciones.
3. **La tarjeta de sueño cambia de alto ella sola.** La fila de horas, la línea de precisión y la
   fila de fases se pintan sólo si el dato las trae — y `App.kt` relee el análisis a los 3 y a los 8
   segundos de abrir, así que puede crecer dos veces con el app ya en pantalla.

**Las pestañas de pilar ya están bien** y no entran: `PillarCatalogScreen` cuelga su esqueleto de
`PillarCatalogUiState.Loading`, que sí es la espera de la red.

## Decisiones tomadas en la puerta de alineación (2026-09-10)

1. **El temporizador de `App.kt` se queda.** Está ahí porque el detector de sueño de Android escribe
   sus segmentos poco después de arrancar; sin las relecturas la tarjeta abriría vacía. Lo que este
   trabajo hace es que su efecto no se note en la posición de las cosas.
2. **La tarjeta de sueño mide siempre lo mismo**, con datos y sin ellos. Reserva el sitio de las tres
   filas opcionales aunque no las tenga. El coste es algo de aire dentro de la tarjeta la primera
   noche; la ganancia es que no empuja nunca, ni al cargar ni al rellenarse.

## Una corrección sobre lo que se dijo la vez anterior

En la bitácora de la búsqueda se escribió que ninguna prueba compone esas pantallas. **Es falso.**
Hay diez clases de prueba de Compose en `app/shared/src/jvmTest/` que usan `runComposeUiTest`, entre
ellas `HomeBoardTest`, que compone `HomeBoard` entero con el estado que quiera. La frase venía
heredada de bitácoras anteriores y no se comprobó. Consecuencia práctica: el slice de la búsqueda se
entregó con menos cobertura de la que el repositorio ya permitía, y estos dos slices sí llevan prueba
de Compose. Lo que falta de aquél queda como slice 3.

## Slices

### Slice 1 — el resumen de anoche reserva su sitio

**Alcance.**

- `MainScreen` deja de recibir `SleepAnalysis?` aplanado y recibe `SleepUiState`, que ya distingue
  las tres situaciones. `App.kt` colecta `uiState` en vez de la proyección `sleepAnalysis`.
- Mientras se lee: un hueco del alto exacto de la tarjeta. Si falló: ni tarjeta ni hueco, como hoy —
  un esqueleto que late para siempre es peor que la ausencia.
- **`SleepSummaryCard` pasa a medir siempre lo mismo.** El alto vive en una constante junto a la
  tarjeta, y el hueco lee esa misma constante: una sola cifra, imposible de desincronizar.
- Ese alto lo fija una prueba, no el ojo: se compone la variante más rica y la más pobre y se afirma
  que miden igual, y que el hueco mide eso mismo.

**Criterios de aceptación.**

- Con la lectura en curso hay un hueco del tamaño de la tarjeta y los pilares ya están en su sitio
  definitivo.
- Al llegar el análisis, nada de lo que hay debajo se mueve.
- La tarjeta mide lo mismo con cero segmentos que con horas, precisión y las tres fases.
- Con la lectura fallida no queda un hueco latiendo.

### Slice 2 — las publicaciones enseñan su hueco

**Alcance.**

- El hueco del feed cuelga de **la espera del feed** (`firstPagePending`), no de la del contenido de
  Inicio.
- Tiene la forma que va a tener: filas de dos, con el alto de una `HazloPublicationCard`.
- Vive dentro de `publicationList`, que es compartida — así el hueco lo enseñan **el feed del Inicio
  y los resultados de la búsqueda** sin escribirlo dos veces.
- El hueco es sólo para la primera página: con publicaciones ya leídas, lo que dice que viene más es
  la ruedita del pie y no un hueco encima.

**Criterios de aceptación.**

- Recién abierto, el feed enseña huecos de dos por fila en vez de una ruedita.
- Al llegar la primera página, las publicaciones ocupan ese mismo sitio.
- Buscar un término enseña los mismos huecos mientras el sitio contesta.
- Pedir la página siguiente no tapa lo ya leído.

### Slice 3 — la cobertura que la búsqueda no llevó

Con `runComposeUiTest` a mano, lo que quedó sin mirar en el slice anterior: que el campo esté en la
barra, que los chips acoten los resultados, y que el Inicio ya no tenga campo. Es deuda reconocida,
no un slice nuevo de producto.

## Preguntas abiertas

1. **¿Qué pasa con una escala de fuente muy grande?** Un alto fijo puede recortar contenido si el
   sistema agranda el texto. La prueba que fija el alto lo detectaría a escala normal, no a las
   demás. Se mira si aparece.

## Estado

| Slice | Estado |
|---|---|
| 1 — el resumen de anoche reserva su sitio | **Hecho** — ver la [bitácora](el-inicio-no-brinca-bitacora.md) |
| 2 — las publicaciones enseñan su hueco | **Hecho** — ver la [bitácora](el-inicio-no-brinca-bitacora.md) |
| 3 — la cobertura que la búsqueda no llevó | **Hecho** — ver la [bitácora](el-inicio-no-brinca-bitacora.md) |

**Pendiente de verlo en un teléfono:** los altos están medidos por prueba, pero que 308 dp de tarjeta
de sueño y dos filas de 228 dp se lean bien en la primera pantalla es cosa de mirarlo.
