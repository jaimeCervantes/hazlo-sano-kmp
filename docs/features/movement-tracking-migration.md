# Migración del pilar Movimiento — backlog

Qué falta por traer del proyecto de referencia (`C:\Users\S2G52\AndroidStudioProjects\HazloSano`) a este proyecto KMP, y con qué encuadre atacar cada pieza.

**Cómo se usa este documento**
- Es el inventario y el encuadre (Problem / Savings / Why) de cada pendiente, no la especificación.
- Cada slice se ejecuta con su propio `features/<nombre>.feature` y deja su entrada en [`movement-tracker-bitacora.md`](movement-tracker-bitacora.md).
- **Cambio de cadencia desde `0d45b7c`:** `AGENTS.md` pide ahora escribir **todos** los escenarios de una feature por adelantado, etiquetados por slice (`@slice-1`, `@slice-2`, …) y con `@future` los que aún no se construyen, para que la forma completa sea revisable antes de que exista código. Este documento se escribió bajo la regla anterior —un `.feature` por slice, redactado en su momento— y ningún `.feature` del pilar usa todavía esas etiquetas. Lo que venga de C2 en adelante sigue la regla nueva.
- Al cerrar un slice: marcar aquí su estado y enlazar el `.feature`. **La entrada de bitácora se escribe al cerrar, no semanas después**: el slice 13 se documentó tarde y hubo que reconstruirlo desde los commits, que es más caro y menos fiable.
- La referencia es **fuente de APIs y comportamiento, no de calidad**: se re-expresa aplicando Clean Architecture, SOLID y las reglas de `AGENTS.md`, y se cubre con tests. No se copia tal cual.

**Aviso sobre `core`**: varios archivos se copiaron a `core/src/commonMain/.../feature/movement/` sin que nadie los usara. Que el archivo exista no significa que la funcionalidad esté migrada: falta implementarlos, conectarlos y probarlos. Estado a día de hoy:

- **Ya en uso y probados:** `KalmanFilter` (reescrito en el slice 8), `CalculateStatsUseCase` (probado desde el slice 9, reformulado en el 11), `ImportRouteUseCase`, `RouteRepository` y el lector de GPX — que dejó de ser `GpxParserImpl` en `jvmMain` y pasó a ser `GpxFormat` en `commonMain` (slice 13).
- **Siguen sin un solo consumidor:** `TrackNavigationUseCase`, `GetRoutesUseCase`, `GetRouteDetailUseCase`, `NavigationController`, `OfflineMapRepository`. Los tres primeros se resuelven con C2/C3; `GetRouteDetailUseCase` quedó además redundante con `RouteRepository.getRouteWithPoints`, que es lo que usa el export. Candidatos a borrado si C3 no los necesita tal cual.

---

## Estado actual (lo ya migrado y validado)

| Pieza | Estado |
|---|---|
| Pantalla de tracker con mapa y ubicación en vivo | Hecho (slices 1–2) |
| Grabación de recorrido con distancia y tiempo en vivo | Hecho (slice 3) |
| Persistencia de la sesión en la base multi-pilar | Hecho (slice 4) |
| Historial de sesiones | Hecho (slice 5) |
| Detalle de sesión con la ruta en el mapa | Hecho (slice 6) |
| Render del mapa en dispositivo | Corregido tras el slice 6 (ver bitácora, entrada de corrección) |
| Grabación en segundo plano | Hecho (slice 7, punto A del backlog), validado en dispositivo |
| Filtrado de la señal antes de acumular distancia | Hecho (slice 8, punto B1), probado en caminata, trote, carrera y bici |
| Estadísticas ciertas (desnivel, tiempo en movimiento) | Hecho (slice 9, punto B2a) |
| Captura de la traza cruda para calibrar | Hecho (slice 10). Usada en la primera salida de campo |
| Cifras derivadas del recorrido + primera migración real | Hecho (slice 11, punto B2b). Salda la deuda de migraciones |
| Calidad de altitud que reporta el receptor | Hecho (slice 12). **Pregunta respondida en la segunda calibración de campo, y la respuesta es "no sirve"**: el receptor declara ±1,8 m verticales sobre una altitud congelada seis minutos |
| Rutas: importar GPX, listar, renombrar, exportar, guardar una salida como ruta | Hecho (slice 13, punto C1) |
| Detalle de una ruta guardada en el mapa | Hecho (punto C2). Primera pantalla del pilar con test de host |
| Que el ruido del receptor no se convierta en kilómetros | Hecho (slice 14, punto B3). De 2 832 m a ~165 con el teléfono quieto media hora, sin costarle distancia a las salidas reales |
| El desnivel se calla cuando la altitud se queda pegada | Hecho (B4, slice 1). Una racha rancia de 60 s o más apaga el desnivel de la sesión entera; umbral conservador, sin validar contra una segunda traza |
| Aviso de desvío al seguir una ruta | Hecho (C3). 50 m sostenidos 30 s, y callado mientras estás parado. Los 50 m calibrados contra las cuatro trazas; los 30 s no |

**Validado en dispositivo:** solo hasta el slice 7. Los slices 8 y 9 se contrastaron contra tres trazas reales en la primera calibración de campo (ver bitácora), que destapó un bug de truncamiento y el problema de la altitud congelada. El slice 12 se contrastó contra una cuarta traza en la segunda calibración, que respondió su pregunta y destapó B3. Los slices 10, 11, 13 y B4 (slice 1) no se han probado en una salida real.

---

## A — Grabación en segundo plano (foreground service)

- **Estado:** hecho (slice 7), validado manualmente en dispositivo el 2026-07-25. Spec: [`movement_background_recording.feature`](../../features/movement_background_recording.feature). Incluye dos correcciones posteriores (reinicio del servicio; ruta terminada que seguía pintada y guardado cancelable) — ver bitácora. Sigue sin cobertura de host: servicio, notificación y pantalla apagada dependen de prueba manual.
- **Problem:** la grabación vive en `TrackerViewModel`; con la pantalla apagada o la app en segundo plano, Android deja de entregar ubicaciones y la sesión queda incompleta o vacía. Cualquier uso real (teléfono en el bolsillo, contestar un mensaje) rompe el registro.
- **Savings:** evita perder sesiones enteras y tener que repetirlas; elimina la desconfianza de revisar cada grabación para ver si sirve.
- **Why:** sin grabación fiable, historial, detalle, estadísticas y rutas se construyen sobre datos rotos.
- **Referencia:** `feature/movement/tracker/service/NavigationService.kt` (108 líneas): servicio en primer plano, canal de notificación `IMPORTANCE_LOW`, notificación persistente que se actualiza con el estado, expone `NavigationState` por flow.
- **Alcance propuesto:** mover la propiedad de la grabación del ViewModel a un servicio en primer plano; Iniciar/Detener siguen en el tracker; al volver a la pantalla, la UI refleja el estado real del servicio.
- **Módulos:** `core` (contrato de sesión en curso), `app/shared/androidMain` (servicio + adaptador), `app/androidApp` (manifest, permisos).
- **Riesgos / notas:** permisos `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION` y `POST_NOTIFICATIONS` (Android 13+); política de servicios en primer plano de Android 14; el estado deja de ser propiedad del ViewModel, así que hay que decidir la fuente de verdad. La referencia usa `NavigationController` como contrato: evaluar si nos sirve o si conviene uno más estrecho (ISP, como se hizo con `MovementSessionRepository`).
- **Cobertura de test:** la lógica de sesión en curso es testeable en `core`/`commonTest`; el servicio en sí necesita test instrumentado.

## B — Filtro Kalman y estadísticas reales

- **Estado: hecho, con una incógnita abierta que no es de código.** B1 (filtro de posición) en el slice 8 — spec: [`movement_location_smoothing.feature`](../../features/movement_location_smoothing.feature). B2a (estadísticas ciertas) en el slice 9 — spec: [`movement_session_statistics.feature`](../../features/movement_session_statistics.feature). B2b en el slice 11, **replanteado**: en vez de mostrar unas columnas guardadas, las cifras pasan a **derivarse del recorrido cada vez que se abre la sesión** — spec: [`movement_session_metrics.feature`](../../features/movement_session_metrics.feature).
- **Lo que queda de B se partió en dos al medirlo en campo**, y va abajo como B3 y B4. B3 (la distancia que se inventa un teléfono parado) es lo más importante del pilar ahora mismo, por delante de la altitud: la altitud se queda corta, la distancia se infla por cinco.
- **La pregunta que abrió el slice 12 está respondida, y la respuesta cierra una puerta:** el receptor **sí** reporta precisión vertical (las 412 lecturas de la cuarta traza la traen), pero declara ±1,8 m de mediana sobre una altitud congelada **385 s seguidos**. No avisa. Descartar por precisión vertical, que era el camino barato, no existe.
- **Problem (encuadre original, conservado):** la distancia se acumula con haversine sobre puntos **crudos**, así que el ruido del GPS infla los kilómetros, y ese error contamina también las estadísticas derivadas.
- **Savings:** datos en los que se puede confiar sin repetir la medición ni corregirla a mano; evita rehacer el historial más adelante con métricas distintas.
- **Why:** el pilar promete ver progreso; con distancias infladas y estadísticas vacías, el progreso mostrado es ficción.
- **Referencia:** `domain/filter/KalmanFilter.kt` (68 líneas, ruido de proceso base 3 m/s) y `domain/usecase/CalculateStatsUseCase.kt` (105 líneas: altitud máx/mín, ascenso/descenso, pendientes, VAM, ritmo actual y medio, tiempo en movimiento).
- **Alcance:** ~~filtrar cada ubicación con Kalman antes de acumular el recorrido~~ (B1, hecho), ~~revisar y probar `CalculateStatsUseCase`~~ (B2a, hecho), ~~mostrar en el detalle las métricas~~ (B2b, hecho — derivándolas, no leyéndolas).
- **Módulos:** `core` (filtro y caso de uso, ambos probados), `app/shared` (ViewModel y UI del detalle).
- **Riesgos / notas:** resuelto lo que aquí se anotaba. Las cifras ya no son columnas guardadas sino funciones de los puntos, así que **retocar una constante mejora también las salidas anteriores** y no quedan sesiones viejas con números congelados. Lo que sigue sin recalcularse es el resumen de la lista de una sesión que nunca se abra.
- **Aprendido en B1 y B2a (aplica a C y a todo lo que toque puntos):** el pilar contempla **caminata, trote, carrera y bici**, y la app **nunca sabe cuál de ellas estás haciendo** — no hay selector y `MovementSession` no lleva tipo de actividad. A 2 s de muestreo eso son 2,8 / 5,0 / 7,8 / 14-30 m por lectura respectivamente, un orden de magnitud de diferencia, así que **cualquier umbral en metros fijos codifica en silencio una actividad y rompe las otras**. Los umbrales se expresan contra la precisión de la lectura o contra el intervalo real, nunca en metros.
- **Segundo aprendizaje de B2a:** filtrar la señal puede **romper cosas aguas abajo que dependían del ruido**. El umbral `dist > 0.5` de `movingTime` funcionaba por accidente mientras llegaban lecturas de cuando estabas parado; en cuanto B1 dejó de entregarlas, una pausa pasó a ser un único segmento largo y contaba entera como movimiento. Al tocar el filtro, revisar quién consume los puntos.
- **Cobertura de test:** unitaria en `core/commonTest` con trazas sintéticas (ruido conocido, subida conocida).

## B3 — Un teléfono parado no inventa distancia

- **Estado: hecho** (slice 14). Spec: [`movement_drifting_signal.feature`](../../features/movement_drifting_signal.feature). Encuadre y medición en la bitácora, entradas "Segunda calibración en campo" y "Slice 14". Resultado sobre las trazas de campo: la media hora parada baja de **2 832 m a ~165**, la caminata y la bici conservan su distancia (−1,7 % y 0 %). El slice 15 añadió lo que se ve durante el minuto de confirmación ("Buscando señal" / "Confirmando…") y el 16 cerró el `@slice-2`: aviso de "llevas X min sin moverte" en el tracker y en la notificación, y la sesión se guarda terminando donde se movió por última vez — distinguiendo quedarse quieto de quedarse sin señal. **B3 queda cerrado.** Lo único pendiente del punto son ~165 m de fantasma que cerraría una puerta de innovación en el Kalman.
- **Problem:** el filtro decide si una lectura es movimiento comparándola solo contra la precisión que esa misma lectura declara. Cuando el receptor pierde cielo sigue entregando fixes que dicen "±6 m" mientras se colocan a 31 m del anterior cada seis segundos. Medido: con el teléfono quieto bajo techo media hora, la app habría grabado **2 832 m y 391 s en movimiento** sobre una salida real de ~480 m en bici.
- **Savings:** una sesión que no hay que borrar ni corregir a mano, y un acumulado en el que la distancia significa algo. Hoy una sola grabación olvidada mete kilómetros que después nadie puede distinguir de los reales, y la única defensa es acordarse de pulsar detener.
- **Why:** la distancia es la promesa del pilar. Un número inflado por cinco es peor que no medir, porque se cree — como pasó con el tiempo en movimiento truncado.
- **El discriminante, medido, no supuesto:** desplazamiento neto en ventanas de 60 s — 6 m con el teléfono parado, 113 m caminando, 137 m en bici. Dos órdenes de magnitud. El movimiento real persiste en una dirección; el ruido se va y vuelve. **La precisión declarada no sirve como discriminante** porque miente justo cuando importa; la puerta de `POOR_ACCURACY` (>50 m) sí funciona y rechazó los 12 fixes verdaderamente malos.
- **Alcance del slice 1:** que el recorrido no crezca mientras la posición no se **queda** lejos de donde estaba, sin perder los metros de un movimiento real una vez confirmado; un motivo de descarte propio en el diagnóstico; y un generador de traza sintética que **deambule como el real** (excursiones de decenas de metros declarándose preciso), porque el ruido sintético actual es honesto sobre su precisión y por eso el escenario "standing still does not add distance" pasa desde el slice 8 mientras falla en la calle.
- **Criterios de aceptación, contra las cuatro trazas reales** (`TraceReplayHarness`, fuera de CI porque `traces/` está en `.gitignore`):

  | Traza | Qué fue | Hoy graba | Debe grabar |
  |---|---|---|---|
  | T1 | quieto en interior, 3 min | 0 m | 0 m (no empeora) |
  | T2 | caminata y trote, 7,6 min | 899 m | 899 m ±5 % |
  | T3 | bici, 5,2 min | 1 190 m | 1 190 m ±5 % |
  | T4 | bici 2 min + 31 min parado | **2 832 m** | ~480 m, y ~0 m aportados por la parte parada |
  | T4 | tiempo en movimiento | **391 s** | el de la parte en bici (~120 s) |

- **Módulos:** `core` (el filtro y su estado; lógica pura, `commonTest`), y revisar aguas abajo quién consume los puntos —`CalculateStatsUseCase` verá ahora un hueco de media hora entre dos puntos aceptados— siguiendo el aprendizaje de B2a.
- **Riesgos / notas:** confirmar el movimiento introduce un retardo de una o dos lecturas en la distancia en vivo; el compromiso es que la distancia se **retrase**, nunca que se pierda. Ningún umbral en metros fijos, como en todo el pilar.
- **Slice 2 (`@future`, sin detallar):** que la grabación avise de que lleva mucho rato sin ir a ninguna parte. Es lo que habría salvado esta salida, pero es una decisión de producto distinta y no hace falta para que la distancia deje de mentir.

## B4 — La altitud rancia

- **Estado: slice 1 hecho** (2026-09-02). Spec:
  [`movement_altitude_staleness.feature`](../../features/movement_altitude_staleness.feature).
  Vía barata (precisión vertical) descartada por la segunda calibración. Sin validar en dispositivo:
  el umbral de 60 s es conservador, no calibrado contra una segunda traza.
- **Problem:** la altitud de este teléfono se congela durante minutos (5 valores distintos en 412
  lecturas; 385 s seguidos en el mismo valor), y **el sistema no lo señala**: declara ±1,8 m
  verticales mientras tanto. El desnivel reportado es ficción por defecto, no por exceso.
- **Savings / Why:** un desnivel que se pueda mirar, o un "—" honesto. Hoy se muestra un número que
  nadie puede contrastar.
- **Caminos que quedan:** (a) detectar la altitud rancia por repetición exacta —una medición real no
  repite el valor al centímetro entre lecturas— y reportar "—" en vez de inventar; (b) barómetro
  (`Sensor.TYPE_PRESSURE`), slice grande y solo Android. El slice 1 ataca (a); (b) queda fuera.
- **Deuda concreta que dejó el slice 12:** al usar la precisión vertical real en vez del 2× horizontal,
  el umbral de acumulación de desnivel cae a **3,0 m, su suelo**, donde antes estaba en 12 m, su techo.
  Sobre una señal cuantizada eso acumula saltos de cuantización como desnivel. Sigue sin revisarse; el
  slice 1 no lo toca porque una racha rancia ya detectada apaga el desnivel entero de la sesión antes
  de que ese umbral entre en juego.

### Slice 1 — El desnivel se calla cuando la altitud se queda pegada

Escenarios `@slice-1`. Ataca el camino (a) del punto anterior.

**Regla, afinada frente al encuadre inicial:** la señal no es *cuántas* lecturas seguidas repiten el
mismo valor (depende del muestreo, que no es constante), sino **cuánto tiempo** seguido no cambia —
es lo que de verdad importa (cuánto terreno pudo cambiar sin que el sensor se enterara) y es
comparable directamente con el dato de campo (385 s). Umbral de partida: **60 s** sin cambiar el
valor exacto, muy por debajo del incidente medido y sin ningún caso real que muestre una repetición
corta y legítima; queda anotado como el número a validar en la próxima salida de campo.

**Alcance:** `CalculateStatsUseCase` detecta rachas de altitud idéntica por duración; una racha que
alcanza el umbral apaga **todas** las cifras derivadas de altitud de la sesión entera (máxima, mínima,
ascenso, descenso, pendiente media y máxima, VAM) — no solo el tramo congelado — porque una sesión que
pasó minutos con el sensor pegado no puede afirmar un desnivel completo y honesto. El resto de la
sesión (distancia, tiempo en movimiento, ritmo) no depende de esto y sigue igual.

**Fuera de alcance:** el barómetro (camino b) y la deuda del umbral de 3 m del slice 12.

## C — Rutas: importar GPX, listar, detalle y navegación guiada

- **Estado: C1 hecho** en el slice 13 — spec: [`movement_routes_gpx.feature`](../../features/movement_routes_gpx.feature). **C2 y C3 pendientes**, y son las que convierten una ruta en algo que se sigue.
  - **Hecho (C1):** `GpxFormat` en `commonMain` (lee y escribe, sin librería XML, tolera lo que no entiende), tablas `RouteEntity`/`RoutePointEntity` con su migración `4.sqm`, pantalla "Mis rutas" para importar, renombrar, exportar y borrar, y guardar una salida del historial como ruta con nombre. Importar y exportar no piden permiso de almacenamiento: van por el Storage Access Framework. Escritorio, iOS y web esconden ambas acciones porque todavía no tienen acceso a archivos.
  - **Hecho (C2):** ver la ruta en el mapa — spec: [`movement_route_detail.feature`](../../features/movement_route_detail.feature). Se toca una ruta de la lista y se abre con su trazado encuadrado y sus tres cifras. Reutiliza `MovementMap` con `fitPathInView`, que ya dibujaba el recorrido de una sesión terminada. **Destapó una mentira heredada:** `calculateStats` acumula un desnivel de 0.0 cuando ningún punto trae altitud, y ese cero se guarda en la fila indistinguible de un llano real; la pantalla lo recupera mirando los puntos y pinta "—". Primera pantalla del pilar con test de host.
  - **Hecho (C3):** avisar cuando te sales del trazado — spec: [`movement_route_deviation.feature`](../../features/movement_route_deviation.feature). Se mide la distancia al **segmento** más cercano, se avisa a partir de **50 m sostenidos 30 s**, y **no se juzga mientras estás parado**. Los 50 m se calibraron replayando las cuatro trazas contra el camino que de verdad recorrieron (`RouteDeviationCalibration`): moviéndose, la señal nunca se aleja más de **19,4 m**; con el teléfono quieto bajo techo llega a **291 m**, que es de donde sale la regla de callarse parado. **Sin «smart snap»**: la referencia movía la posición sobre la ruta, y eso haría que la salida guardada siguiera el trazado aunque te hubieras desviado. El `routeId` ya se guardaba desde el slice 3 de `puertas-de-movimiento`. **Pendiente de campo:** los 30 s no están calibrados y nadie ha medido un desvío real — ver la bitácora.
- **Problem:** no se puede seguir una ruta planificada: no hay forma de verla en el mapa ni de saber si te estás saliendo del trazado.
- **Savings:** evita depender de otra app para seguir rutas y el riesgo de perderse o desviarse sin darse cuenta.
- **Why:** "recorrer rutas" es la promesa central del pilar en la referencia; la sesión libre es solo la mitad.
- **Referencia:** `domain/parser/GpxParser.kt`, `domain/usecase/ImportRouteUseCase.kt`, `TrackNavigationUseCase.kt` (166 líneas: *smart snap* al trazado, distancia mínima a la ruta, proyección de punto sobre segmento, detección de desvío), `data/repository/RouteRepositoryImpl.kt`, `ui/RouteImportScreen.kt`, `ui/RouteDetailScreen.kt`, `ui/RouteViewModel.kt` (160 líneas).
- **Sub-slices:** ~~C1 importar un GPX y listarlo~~ (hecho, y llegó más lejos: exportar, renombrar, borrar y guardar una salida como ruta) · ~~C2 detalle de ruta en el mapa~~ (hecho) · ~~C3 aviso de desvío~~ (hecho). **El punto C queda cerrado.**
- **Módulos:** `core` (`RouteGeometry` y `RouteDeviation`, ambos probados; `TrackNavigationUseCase` se borró al reexpresar su comportamiento), `app/shared` (el aviso vive en el tracker).
- **Riesgos / notas:** las notas de riesgo de C1 quedaron resueltas — la deuda de migraciones se pagó en el slice 11 y `RouteRepository` se segregó al implementarlo. Lo que queda anotado: el `fingerprint` heredado de la referencia (`distancia-desnivel-lat1-lon1-latN-lonN`, distancia truncada a entero) hace colisionar dos rutas distintas con el mismo inicio, fin y total; como el duplicado **se pregunta** en vez de decidirse, una colisión molesta pero no destruye nada.
- **Cobertura de test:** parser y navegación son lógica pura → `core/commonTest`; repositorio con SQLDelight en memoria como en los slices 4–6.

## D — Mapas offline

- **Estado:** pendiente.
- **Problem:** sin cobertura, el mapa se queda en blanco justo donde más se usa (monte, ruta larga).
- **Savings:** evita quedarse sin referencia visual y el consumo de datos en cada salida.
- **Why:** las actividades del pilar ocurren fuera, donde la conectividad es peor.
- **Referencia:** `data/repository/MapLibreOfflineRepository.kt` sobre `OfflineManager` (crear región, progreso, comprobar si ya está descargada).
- **Efecto lateral relevante:** `OfflineManager.getInstance(context)` activa el `FileSource` a nivel de aplicación. Es la razón por la que el mapa de la referencia funciona sin gestionar el ciclo de vida del `MapView` (ver la entrada de corrección en la bitácora).
- **Módulos:** `core` (interfaz ya copiada), `app/shared/androidMain` (implementación), UI de progreso.
- **Riesgos / notas:** tamaño de descarga y límite de tiles; permisos de almacenamiento no hacen falta (caché interna).

## E — Estilo satélite

- **Estado:** pendiente. Slice pequeño.
- **Problem:** el mapa OSM no sirve para reconocer terreno real (senderos, vegetación) en salidas de monte.
- **Savings:** menos dudas sobre por dónde va el trazado sin salir de la app.
- **Why:** complementa el detalle de sesión y la navegación por ruta.
- **Referencia:** `isSatelliteMode` en `RouteMap.kt`/`SessionDetailScreen.kt`; `MapConstants.SATELLITE_STYLE_URL` **ya existe** en nuestro proyecto, sin usar.
- **Notas:** al cambiar de estilo hay que volver a añadir fuentes y capas (`MapLayers.setup`) y redibujar los datos; es el bug clásico de este toggle.

## G — La grabación sobrevive a que el sistema mate el proceso

- **Estado:** pendiente. Surgido del slice 7, no de la referencia (allí tampoco está resuelto).
- **Problem:** hoy la sesión en curso solo existe en memoria del proceso. Si Android mata la app por presión de memoria a mitad de una salida, lo grabado hasta ese momento se pierde entero y sin aviso: al volver, el usuario encuentra el tracker en reposo como si nunca hubiera empezado.
- **Savings:** evita perder una salida completa por una decisión del sistema que el usuario no controla ni percibe; hoy la única defensa es no usar otras apps durante la grabación.
- **Why:** el slice 7 quitó la dependencia de tener la pantalla encendida; esto quita la última condición para que una grabación larga sea confiable, que es justo cuando más probable es que el sistema recorte memoria.
- **Alcance propuesto:** persistir la sesión en curso mientras se graba (arranque y puntos, de forma incremental) y recuperarla al recrear el servicio, de modo que un reinicio continúe la sesión en vez de descartarla.
- **Módulos:** `core` (reglas de recuperación: qué se considera sesión reanudable y qué se descarta), `app/shared` (persistencia incremental, probable reuso de `MovementSessionRepository` o una tabla de sesión en curso), `androidMain` (servicio: volver a `START_STICKY` o `START_REDELIVER_INTENT` una vez haya algo que reanudar).
- **Estado actual del comportamiento (mitigación aplicada en el slice 7):** el servicio devuelve `START_NOT_STICKY` y, si el sistema lo recrea sin acción, se apaga en vez de quedarse como servicio zombi que no graba. Es honesto —no finge grabar— pero la sesión se sigue perdiendo.
- **Riesgos / notas:** decidir la frecuencia de escritura (cada punto es demasiado; por lotes o por tiempo); definir qué pasa con una sesión reanudable que quedó huérfana (¿ofrecerla al volver, descartarla tras X horas?).
- **Cobertura de test:** las reglas de recuperación son lógica pura → `core/commonTest`; la persistencia incremental, integración con SQLDelight en memoria. Que el sistema mate el proceso se prueba a mano (`adb shell am kill`) o con test instrumentado.

## F — Dashboard de movimiento

- **Estado:** pendiente. Valor cosmético frente al resto.
- **Problem:** hoy el pilar se entra por una tarjeta y va directo al tracker; no hay una portada que ofrezca las actividades.
- **Savings:** poco medible; es descubrimiento, no fiabilidad.
- **Why:** presentación del pilar alineada con el resto de la app.
- **Referencia:** `feature/movement/dashboard/ui/MovementDashboardScreen.kt` (203 líneas).
- **Riesgos / notas:** la referencia carga imágenes remotas de Unsplash por URL; en KMP eso implica decidir librería de imágenes multiplataforma o usar recursos locales. Recomendable dejarlo para el final.

---

## Deuda transversal (no es migración, pero condiciona lo anterior)

| Deuda | Detalle | Bloquea |
|---|---|---|
| ~~Migraciones SQLDelight reales~~ | **Saldada en el slice 11**: `ensureNewTablesExist` borrado, migraciones versionadas activas. Van ya por `4.sqm` (versión 5) | — |
| Tests instrumentados | El render del mapa no tiene cobertura en host; el fallo de render sobrevivió tres slices sin detectarse | C2, D, E |
| Tests de Compose UI | `runComposeUiTest` no está configurado; hoy solo cubrimos ViewModel/estado/formato. `RoutesScreen` y su diálogo de duplicado entraron sin test de UI | todos |
| Robolectric + `withHostTest` | Los `actual` de Android no se pueden probar en JVM. Ahora también afecta al acceso a archivos por SAF | A, C1, D |
| `SecurityException` de Google Play Services | Visto en logcat (`GoogleApiManager: Unknown calling package name`); afecta al proveedor de ubicación fusionada | A, B |
| i18n de la pantalla de rutas | `RoutesScreen` va con el texto en duro y `RoutesViewModel` expone `message: String` ya redactado; ambas cosas contra las reglas que `AGENTS.md` fijó justo después. Sus tests afirman sobre la redacción | C2, C3 (crece con cada pantalla nueva) |
| ~~Código muerto en `core`~~ | **Casi saldada con C3**: se borraron `TrackNavigationUseCase`, `NavigationController`, `NavigationState`, `GetRoutesUseCase` y `GetRouteDetailUseCase`. Queda `OfflineMapRepository`, que es del punto D | — |
| ~~`:app:shared:check` en rojo~~ | **Resuelto** subiendo SQLDelight 2.0.2 → 2.3.2. `verifyCommonMainHazloSanoDatabaseMigration` no arrancaba en Windows: Gradle lanza el worker de esa tarea con `processIsolation` **y sin heredar el entorno**, así que sin `TEMP` ni `TMP` el `java.io.tmpdir` del worker caía en `C:\WINDOWS` — no escribible —, sqlite-jdbc no podía extraer ahí su `.dll` y la llamada nativa moría con `UnsatisfiedLinkError: _open_utf8`. Nada que ver con el esquema ni con nuestra versión de sqlite-jdbc. Lo arregla [#5215](https://github.com/sqldelight/sqldelight/pull/5215), incluido en 2.3.2 | — |

---

## Orden sugerido

1. ~~**A** — fiabilidad de la grabación (sin esto, lo demás guarda datos rotos).~~ Hecho.
2. ~~**B** — calidad de los datos grabados (B1 filtro, B2a estadísticas, B2b cifras derivadas).~~ Hecho en código.
3. ~~**Deuda: migraciones SQLDelight.**~~ Saldada en el slice 11.
4. ~~**C1** — importar, listar y exportar rutas.~~ Hecho en el slice 13.
5. ~~**Salir con la captura de traza activada** y mirar qué dice la precisión vertical en las ventanas de altitud congelada.~~ Hecho el 2026-08-29 (segunda calibración de campo). Respondió su pregunta —la precisión vertical no delata la altitud congelada— y destapó B3.
6. ~~**B3** — que un teléfono parado no invente distancia.~~ Hecho en el slice 14, calibrado contra las cuatro trazas sin salir a la calle. Queda su `@slice-2`: avisar de que la grabación lleva mucho sin ir a ninguna parte.
7. **B4** — la altitud rancia, por repetición exacta o por barómetro.
8. ~~**C2 → C3** — la ruta en el mapa, y luego seguirla con aviso de desvío.~~ Hechos.
9. **D** — offline (además activa el `FileSource` a nivel de app).
10. **E** — satélite.
11. **F** — dashboard.

**G** (sobrevivir a que el sistema mate el proceso) no tiene posición fija: depende de si en uso real Android está matando la app durante las grabaciones. Si ocurre, sube al principio; si no, puede esperar.

También pendiente de campo, arrastrado desde la primera calibración: repetir la prueba de teléfono quieto **a cielo abierto** (las dos que tenemos, T1 y la parte parada de T4, se hicieron bajo techo) y medir una ruta real para cerrar la duda de la distancia caminando. Ninguna de las dos bloquea B3: la traza T4 ya trae el caso que hay que arreglar.

**Lo que la próxima salida tiene que responder, en un solo sitio.** C3 se construyó antes de salir, a
propósito, calibrando contra las trazas todo lo calibrable. Lo que queda son tres preguntas concretas,
y las tres se contestan con **una sola captura: caminar una ruta conocida y salirse de ella a
propósito**, con la traza encendida:

1. **¿Los 30 s de persistencia del aviso de desvío sobran o faltan?** No están calibrados, igual que
   los 60 s de B4.
2. **¿Se detecta un desvío real a tiempo?** Las cuatro trazas dan el ruido que el umbral debe dejar
   pasar, pero ninguna es de alguien siguiendo una ruta.
3. **¿Los 60 s de `isMoving` callan el aviso en una parada legítima?** Un semáforo largo podría.

Y de paso esa misma salida cierra las dos de arriba —el quieto a cielo abierto y la distancia
caminando— si se hace en un sitio con cielo despejado y una parada larga en medio.

El orden es una recomendación, no un compromiso: cada slice se aprueba en su momento con su encuadre y su `.feature`.
