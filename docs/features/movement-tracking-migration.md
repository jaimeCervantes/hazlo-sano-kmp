# Migración del pilar Movimiento — backlog

Qué falta por traer del proyecto de referencia (`C:\Users\S2G52\AndroidStudioProjects\HazloSano`) a este proyecto KMP, y con qué encuadre atacar cada pieza.

**Cómo se usa este documento**
- Es el inventario y el encuadre (Problem / Savings / Why) de cada pendiente, no la especificación.
- Cada slice se ejecuta con su propio `features/<nombre>.feature` (escrito y aprobado **en ese momento**, no por adelantado) y su entrada en [`movement-tracker-bitacora.md`](movement-tracker-bitacora.md).
- Al cerrar un slice: marcar aquí su estado y enlazar el `.feature`.
- La referencia es **fuente de APIs y comportamiento, no de calidad**: se re-expresa aplicando Clean Architecture, SOLID y las reglas de `AGENTS.md`, y se cubre con tests. No se copia tal cual.

**Aviso sobre `core`**: varios archivos ya están copiados en `core/src/commonMain/.../feature/movement/` pero **nadie los usa** — `KalmanFilter`, `GpxParser`, `CalculateStatsUseCase`, `TrackNavigationUseCase`, `ImportRouteUseCase`, `GetRoutesUseCase`, `GetRouteDetailUseCase`, y las interfaces `RouteRepository`, `NavigationController`, `OfflineMapRepository`. Que el archivo exista no significa que la funcionalidad esté migrada: falta implementarlos, conectarlos y probarlos.

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

- **Estado:** pendiente. Es el slice más pequeño y el único 100 % en `core` con tests puros.
- **Problem:** la distancia se acumula con haversine sobre puntos **crudos**, así que el ruido del GPS infla los kilómetros, y ese error contamina también las estadísticas derivadas. (`CalculateStatsUseCase` ya se invoca al detener y sus columnas se persisten — lo que falta es filtrar la señal antes de acumular, y mostrar las métricas que ya se guardan.)
- **Savings:** datos en los que se puede confiar sin repetir la medición ni corregirla a mano; evita rehacer el historial más adelante con métricas distintas.
- **Why:** el pilar promete ver progreso; con distancias infladas y estadísticas vacías, el progreso mostrado es ficción.
- **Referencia:** `domain/filter/KalmanFilter.kt` (68 líneas, ruido de proceso base 3 m/s) y `domain/usecase/CalculateStatsUseCase.kt` (105 líneas: altitud máx/mín, ascenso/descenso, pendientes, VAM, ritmo actual y medio, tiempo en movimiento).
- **Alcance propuesto:** filtrar cada ubicación con Kalman antes de acumular el recorrido, revisar y probar `CalculateStatsUseCase` (hoy se usa sin un solo test), y mostrar en el detalle las métricas que ya se persisten.
- **Módulos:** `core` (filtro + caso de uso ya copiados, hay que revisarlos y probarlos), `app/shared` (ViewModel y UI del detalle).
- **Riesgos / notas:** las columnas de stats ya existen en `MovementSessionEntity`, así que no hace falta migración de esquema. Sesiones ya grabadas seguirán con stats vacíos: decidir si se muestran como "—".
- **Cobertura de test:** unitaria en `core/commonTest` con trazas sintéticas (ruido conocido, subida conocida).

## C — Rutas: importar GPX, listar, detalle y navegación guiada

- **Estado:** pendiente. El bloque más grande; conviene partirlo en al menos tres slices.
- **Problem:** no se puede seguir una ruta planificada: no hay forma de traer un GPX, verlo, ni saber si te estás saliendo del trazado.
- **Savings:** evita depender de otra app para seguir rutas y el riesgo de perderse o desviarse sin darse cuenta.
- **Why:** "recorrer rutas" es la promesa central del pilar en la referencia; la sesión libre es solo la mitad.
- **Referencia:** `domain/parser/GpxParser.kt`, `domain/usecase/ImportRouteUseCase.kt`, `TrackNavigationUseCase.kt` (166 líneas: *smart snap* al trazado, distancia mínima a la ruta, proyección de punto sobre segmento, detección de desvío), `data/repository/RouteRepositoryImpl.kt`, `ui/RouteImportScreen.kt`, `ui/RouteDetailScreen.kt`, `ui/RouteViewModel.kt` (160 líneas).
- **Sub-slices sugeridos:** C1 importar un GPX y listarlo · C2 detalle de ruta en el mapa · C3 navegación siguiendo la ruta con aviso de desvío.
- **Módulos:** `core` (parser y casos de uso copiados, sin probar), `app/shared` (tablas `MovementRouteEntity`/`MovementWayPointEntity`, repositorio, UI), `app/androidApp` (selector de archivos).
- **Riesgos / notas:** requiere **tablas nuevas** → primera vez que hará falta resolver la deuda de migraciones SQLDelight (hoy hay un hack `ensureNewTablesExist`). La referencia detecta duplicados por `fingerprint` (distancia-desnivel-primer/último punto). Nuestro `RouteRepository` copiado mezcla rutas y sesiones: segregarlo antes de implementarlo.
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
| Migraciones SQLDelight reales | Sustituir el hack `ensureNewTablesExist` de `DriverFactory.android.kt` por migraciones versionadas | C (tablas nuevas) |
| Tests instrumentados | El render del mapa no tiene cobertura en host; el fallo de render sobrevivió tres slices sin detectarse | A, C, D, E |
| Tests de Compose UI | `runComposeUiTest` no está configurado; hoy solo cubrimos ViewModel/estado/formato | todos |
| Robolectric + `withHostTest` | Los `actual` de Android no se pueden probar en JVM | A, D |
| `SecurityException` de Google Play Services | Visto en logcat (`GoogleApiManager: Unknown calling package name`); afecta al proveedor de ubicación fusionada | A, B |

---

## Orden sugerido

1. **A** — fiabilidad de la grabación (sin esto, lo demás guarda datos rotos).
2. **B** — calidad de los datos grabados.
3. **Deuda: migraciones SQLDelight** — justo antes de necesitar tablas nuevas.
4. **C1 → C2 → C3** — rutas y navegación guiada.
5. **D** — offline (además activa el `FileSource` a nivel de app).
6. **E** — satélite.
7. **F** — dashboard.

**G** (sobrevivir a que el sistema mate el proceso) no tiene posición fija: depende de si en uso real Android está matando la app durante las grabaciones. Si ocurre, sube justo detrás de A; si no, puede esperar.

El orden es una recomendación, no un compromiso: cada slice se aprueba en su momento con su encuadre y su `.feature`.
