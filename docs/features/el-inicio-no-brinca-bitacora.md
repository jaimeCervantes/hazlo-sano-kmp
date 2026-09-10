# Bitácora — El Inicio no brinca mientras carga

Roadmap: [`el-inicio-no-brinca.md`](el-inicio-no-brinca.md) · Spec:
[`el_inicio_no_brinca.feature`](../../features/el_inicio_no_brinca.feature)

---

## Slices 1, 2 y 3 (2026-09-10)

**Lo reportado:** al abrir el app, el espacio del resumen de anoche no existe, no se ve su esqueleto,
y cuando llegan los datos empuja los demás elementos hacia abajo.

### El reporte era un caso de algo más general

Los tres esqueletos que había —pilares, destacados y feed— cuelgan de `HomeUiState.Loading`, y ese
estado sale de `MockHomeRepository.getHomeContent()`: una función `suspend` sin I/O y sin `delay`,
que se resuelve en un frame. **Prácticamente no se veían nunca.** Y las dos esperas que sí duran no
tenían ninguno: la base de datos del sueño no reservaba nada, y la red de las publicaciones enseñaba
una ruedita de 48 dp donde iban a entrar cientos de píxeles de lista.

### `null` significaba dos cosas y por eso no se podía reservar

`GetSleepAnalysisUseCase` devuelve un `SleepAnalysis` con ceros cuando no hay sesiones, **nunca
`null`**. El nulo aparecía sólo al aplanar `SleepUiState` a `SleepAnalysis?` para pasarlo por
`MainScreen`, y con él se perdía la diferencia entre «todavía no ha vuelto» y «no se pudo leer».

Con las dos mezcladas no se podía decidir: reservar siempre habría dejado un hueco latiendo para
siempre tras un fallo, y no reservar nunca es el brinco. Así que `MainScreen` recibe ahora el estado
entero. El `StateFlow` `sleepAnalysis` del ViewModel se queda sin usuarios y se borra.

**Fallar no deja hueco**, a propósito: un esqueleto perpetuo promete algo que ya se sabe que no va a
llegar, y eso es peor que la ausencia.

### Los dos altos los fijó una prueba, no el ojo

Ninguno de los dos números se eligió mirando. Se compuso el componente, se leyó lo que medía y se
puso eso — y la prueba se queda puesta, así que el día que alguien añada una fila que no quepa, lo
dice con el número nuevo antes de que llegue a un teléfono.

| | Medido | Cómo |
|---|---|---|
| `SleepSummaryCardDefaults.height` | **308 dp** | la variante más rica: horas, precisión y las tres fases |
| `PublicationListDefaults.cardHeight` | **228 dp** | una tarjeta con la descripción a dos renglones, su caso más alto |

Y el hueco lee **la misma constante** que el componente al que espera, en los dos casos. No son dos
cifras que haya que acordarse de mover a la vez.

**El primer intento midió 12 dp**, y eso enseñó algo: en un `Column` acotado los hijos posteriores se
quedan sin alto disponible, así que la prueba medía el recorte del contenedor y no la tarjeta. Con
`verticalScroll` los hijos reciben alto sin límite y miden lo suyo.

### `heightIn(min)` y no `height`

Con un alto exacto, una escala de fuente grande recortaría el contenido en silencio. Con mínimo, el
peor caso es que la tarjeta crezca — visible, y con la prueba diciéndolo antes. Fue además lo que
permitió **medir**: al no caber, la prueba reporta el alto natural, que es justo el número que hacía
falta.

### 308 dp es bastante aire la primera noche, y se acepta

Una noche sin registrar enseña tres ceros en un espacio pensado para el doble de información. Se
acepta a sabiendas: el detector de Android escribe sus segmentos a los pocos segundos de abrir el app
—de ahí el `LaunchedEffect` de `App.kt`, que **se queda**—, así que ese estado dura una noche y el
otro dura siempre. Reservar de menos significaría que rellenarse empuja la pantalla, que es
exactamente lo reportado.

### El hueco del feed tenía la forma equivocada, y ahora sólo hay una

Dibujaba dos tarjetas de ancho completo de 320 dp mientras el feed llevaba desde la segunda pasada
del slice 1 yendo de dos en dos. **Un hueco con otra forma que la lista que espera no quita el
brinco: lo parte en dos.**

Ahora lo pone `publicationListSkeleton`, dentro de `publicationList`, que es compartida — así lo
enseñan el feed del Inicio **y los resultados de la búsqueda** sin escribirlo dos veces, y
`homeFeedSkeleton` se queda sólo con su encabezado y delega las filas.

La condición para pintarlo es `publications.isEmpty() && loading`, y no `firstPagePending`: vale
igual para la primera página del feed, la de una búsqueda y la de un filtro recién cambiado, que son
el mismo momento visto tres veces. Con publicaciones ya leídas no se pinta —lo que dice que viene más
es la ruedita del pie— porque tapar lo leído sería esconder lo que ya se podía leer.

### La corrección: sí había pruebas de Compose, y lo dije al revés

La bitácora del slice de la búsqueda decía que ninguna prueba componía esas pantallas. **Es falso.**
Hay diez clases en `app/shared/src/jvmTest/` que usan `runComposeUiTest`, entre ellas `HomeBoardTest`,
que compone `HomeBoard` entero. La frase venía heredada de bitácoras anteriores y no se comprobó, y
la consecuencia práctica fue entregar aquel slice con menos cobertura de la que el repositorio ya
permitía. El slice 3 la salda: `SearchResultsScreenTest` mira los chips, el vacío y el hueco, y
`HomeBoardTest` afirma que el Inicio ya no tiene campo.

### Dos pruebas que se colgaron, y por qué

`performScrollToNode` con `mainClock.autoAdvance = false` no termina nunca: el scroll necesita que el
reloj corra para asentarse. La prueba que baja hasta los campeones deja el reloj andando —no compone
ningún hueco, así que no hay animación infinita que la atrape— y las que sí componen huecos lo paran,
como ya hacía el resto de la clase.

Y esos campeones **cayeron por debajo del pliegue** al crecer el resumen de anoche. Se baja hasta
ellos en vez de aflojar la afirmación, que habría sido dejar de comprobar que la sección existe.

### Archivos tocados

- **Presentación:** `App.kt` (colecta `uiState`), `SleepViewModel.kt` (fuera el `StateFlow`
  aplanado), `MainScreen.kt` (`sleepState`).
- **UI:** `SleepSummaryCard.kt` (`SleepSummaryCardDefaults.height`, `heightIn`), `HomeScreen.kt`
  (`sleepSummary`, etiquetas `BOARD` y `SLEEP`), `HomeSkeleton.kt` (`sleepSummarySkeleton`, y el
  hueco del feed delegado), `PublicationList.kt` (`publicationListSkeleton`,
  `PublicationListDefaults`).
- **Tests:** `SleepSummaryCardHeightTest` (nuevo, 2), `PublicationListSkeletonTest` (nuevo, 4),
  `SearchResultsScreenTest` (nuevo, 4), `HomeBoardTest` (+4 y ajustes).

### Comandos y resultados

- `.\gradlew.bat :app:shared:jvmTest` → **473 pruebas, 0 fallos** (venían 459).
- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Sin comprobar

- **No se ha visto en un teléfono.** Los altos están medidos por prueba, pero que 308 dp de tarjeta
  de sueño y dos filas de 228 dp se lean bien en la primera pantalla de un móvil es cosa de mirarlo.
- **Escalas de fuente grandes:** la prueba mide a la escala normal. Con `heightIn(min)` el contenido
  no se recorta, pero la tarjeta puede crecer y volver a empujar. Queda anotado como pregunta
  abierta del roadmap.
- **El frame de `HomeUiState.Loading`** enseña el hueco del feed sin la fila de chips, que sí está en
  `Success`. Dura un frame porque el repositorio de mentira no espera; el día que el Inicio venga de
  la red, eso es un brinco de 40 dp que habrá que reservar también.

**Recap.** El resumen de anoche reserva su sitio desde el primer frame y mide siempre lo mismo, así
que ni aparecer ni rellenarse mueven la pantalla; y el hueco de las publicaciones tiene por fin la
forma de las publicaciones y cuelga de la espera que de verdad dura. Lo que hizo falta para
conseguirlo fue recuperar una distinción que se perdía al aplanar el estado del sueño, y medir dos
altos en vez de estimarlos.

**Próximos pasos (opciones).** (1) Verlo en el teléfono, que es lo único que falta; (2) el slice 2 de
`busqueda-en-la-barra.md`, el campo de las pestañas de pilar; (3) el slice 4 de
`publicaciones-en-el-home.md`, lo que el buscador dejó a medias.

---

## Slice 1, segunda pasada — 308 dp eran un tercio de aire (2026-09-10)

**Probado en el teléfono.** La tarjeta quedaba con el último tercio vacío: el sitio reservado para
dos filas que no llegan nunca.

### Las dos filas opcionales están muertas hoy, y se puede demostrar

- **La precisión** sólo se pinta con `averageConfidence < 1`, y `SleepReceiver` guarda
  `confidence = 1.0f` **fijo**, escrito en el código.
- **Las fases** sólo se pintan con LIGHT, DEEP o REM, que `mapStatusToPhase` saca de
  `STATUS_LIGHT = 3`, `STATUS_DEEP = 4` y `STATUS_REM = 5` — valores que el Sleep API de Android no
  devuelve: `SleepSegmentEvent` reporta si la detección fue exitosa, no en qué etapa de sueño se
  estaba.

Así que reservar 308 dp era reservar 68 para dos filas imposibles.

### Los cuatro números, medidos

Poniendo la constante a 1 dp —con `heightIn(min)`, nada queda forzado— la prueba reporta el alto
natural de las cuatro variantes de una vez:

| Variante | Alto |
|---|---|
| Sin registrar (cero segmentos) | 192 dp |
| Con horas de dormirse y despertar | **240 dp** |
| Con precisión | 260 dp |
| Con las tres fases | 308 dp |

**240 dp es lo que el app produce hoy**, y es el nuevo alto reservado. La tarjeta baja un 22%.

### Lo que se pierde, dicho

Deja de estar garantizado que la tarjeta no crezca **si alguien arregla el mapeo de fases**. Por eso
la prueba afirma también los otros dos números en vez de olvidarlos: quien lo arregle verá fallar
`the rows that cannot happen today would make it grow` con el número nuevo, y decidirá a sabiendas si
sube la reserva o acepta el crecimiento. Es la diferencia entre una decisión y un descubrimiento.

### Un hallazgo que no se buscaba, y que no se arregla aquí

`mapStatusToPhase` no coincide con el API: los estados reales de `SleepSegmentEvent` son
`STATUS_SUCCESSFUL`, `STATUS_MISSING_DATA` y `STATUS_NOT_DETECTED`, y aquí el 1 se lee como AWAKE y
el 2 como ASLEEP. Con eso, **un segmento detectado con éxito se guarda como `UNKNOWN`** —que es lo
que explica el desglose vacío de la captura— y `confidence` es una constante en vez del dato del
sensor. No se toca en este slice: es el pilar de sueño y merece su propio encuadre. Queda dicho.

### Archivos tocados

- **UI:** `SleepSummaryCard.kt` (240 dp, y por qué).
- **Tests:** `SleepSummaryCardHeightTest` (la tabla se parte en dos: lo que el app produce y lo que
  costaría lo que no).
- **Spec:** `el_inicio_no_brinca.feature` (dos filas fuera de la tabla, con la razón).

### Comandos y resultados

- `.\gradlew.bat :app:shared:jvmTest` → **474 pruebas, 0 fallos**.

**Recap.** El alto reservado pasa de 308 a 240 dp porque las dos filas que faltaban por reservar no
pueden llegar con el detector que hay. El aire desaparece y la garantía se mantiene entera para todo
lo que el app produce de verdad; para lo que no, la prueba se queda de guardia.
