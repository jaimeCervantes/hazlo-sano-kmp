## Medición de la Calidad del Sueño en HazloSano

  1. La Métrica Central: Eficiencia

  La calidad del sueño se mide fundamentalmente con un solo valor: la eficiencia, calculada en `shared/.../domain/usecase/GetSleepAnalysisUseCase.kt`.

  Fórmula:

  eficiencia = tiempoTotalDormido / periodoTotalEnCama

  Donde:
  - totalDurationMillis: suma del tiempo real dormido (recortado a la ventana de análisis, típicamente de 6 PM de ayer hasta ahora).
  - sleepPeriodMillis: desde el primer momento que te dormiste hasta el último que despertaste (el lapso total "en cama").

  El resultado se acota entre 0.0 y 1.0.

  Ejemplo concreto

  Si te acostaste a las 10 PM, despertaste a las 6 AM (8h en cama), pero solo dormiste 7h reales (con 1h de despertares), tu eficiencia es 7 / 8 = 0.875 →
  Buen descanso.

  ---
  2. Etiquetas de Calidad (Thresholds)

  La eficiencia se traduce a categorías con estos umbrales (definidos en SleepQualityLabel.kt:44-47):

  ┌────────┬──────────────────────┬─────────────────────┐
  │ Umbral │   Condición extra    │      Etiqueta       │
  ├────────┼──────────────────────┼─────────────────────┤
  │ ≥ 90%  │ Y máximo 2 segmentos │ Excelente descanso  │
  ├────────┼──────────────────────┼─────────────────────┤
  │ ≥ 80%  │ —                    │ Buen descanso       │
  ├────────┼──────────────────────┼─────────────────────┤
  │ ≥ 65%  │ —                    │ Sueño interrumpido  │
  ├────────┼──────────────────────┼─────────────────────┤
  │ > 0%   │ —                    │ Descanso deficiente │
  ├────────┼──────────────────────┼─────────────────────┤
  │ 0%     │ (sin datos)          │ Aún sin datos       │
  └────────┴──────────────────────┴─────────────────────┘

  La restricción de "máximo 2 segmentos" para "Excelente" es clave: aunque tengas 95% de eficiencia, si tu sueño está muy fragmentado (más de 2 bloques), no
   se considera excelente.

  ---
  3. Métricas de Historial (Multi-noche)

  En GetSleepHistoryUseCase.kt se calculan métricas agregadas sobre varias noches:

  Métrica: Horas promedio
  Cálculo: Media de horas dormidas por noche
  ────────────────────────────────────────
  Métrica: Eficiencia promedio
  Cálculo: Media de la eficiencia de cada noche
  ────────────────────────────────────────
  Métrica: Consistencia
  Cálculo: Desviación estándar de la hora de inicio del sueño (en minutos)
  ────────────────────────────────────────
  Métrica: Ventana de sueño
  Cálculo: Tiempo promedio "en cama" por noche
  ────────────────────────────────────────
  Métrica: Deuda de sueño
  Cálculo: Minutos acumulados por debajo de 8h/noche (sum(max(0, 8h - dormido)))
  ────────────────────────────────────────
  Métrica: Tendencia
  Cálculo: Compara la mitad reciente de noches vs. la mitad antigua. Si mejora +30min → "Mejorando ↑", si empeora -30min → "Empeorando ↓", si no → "Estable
    →"

  Las sesiones se agrupan en "noches" usando como frontera las 6 PM: una sesión que empieza ≥ 18:00 pertenece a esa noche; si empieza antes, pertenece a la
  noche anterior.

  ---
  4. Texto de Ánimo y Fondo de Pantalla

  En SleepHistoryScreen.kt hay lógica adicional:

  Texto motivacional — prioriza estos casos en cascada:
  1. Noche mala (eff < 75%, ≥ 3 segmentos) pero mejorando → ánimo por mejora
  2. Noche mala sin mejora → ánimo por noche difícil
  3. Mejorando (> 5% vs noche anterior) → felicitación
  4. Empeorando (> 5%) → ánimo
  5. Semana excelente (eff ≥ 85%, ≥ 4 noches) → felicitación
  6. Buen camino (eff ≥ 80%) → motivación
  7. Deuda de sueño (> 120 min) → alerta
  8. Mensaje genérico

  Fondo de pantalla — selecciona 1 de 8 imágenes de Unsplash según eficiencia promedio + tendencia.

  ---
  5. Flujo de Datos Completo

  Google Play Services (ActivityRecognition API)
    → SleepReceiver (Android)
      → Mapea status → SleepPhase (1=AWAKE, 2=ASLEEP, 3=LIGHT, 4=DEEP, 5=REM)
        → SqlDelightSleepDataSource
          → GetSleepAnalysisUseCase (una noche)
          → GetSleepHistoryUseCase (múltiples noches)
            → SleepViewModel / SleepHistoryViewModel
              → UI (SleepSummaryCard, NightCard, SleepHistoryScreen)

  Nota importante: La fase de sueño (SleepPhase) actualmente no se persiste en la base de datos SQLDelight — solo existe en memoria. Esto significa que el
  desglose por fases (ligero/profundo/REM) se pierde al reiniciar la app.

  ---
  6. Archivos Clave

  ┌──────────────────────────────────────────────────────┬───────────────────────────────────────────┐
  │                       Archivo                        │                    Rol                    │
  ├──────────────────────────────────────────────────────┼───────────────────────────────────────────┤
  │ shared/.../domain/usecase/GetSleepAnalysisUseCase.kt │ Cálculo de eficiencia (métrica)           │
  ├──────────────────────────────────────────────────────┼───────────────────────────────────────────┤
  │ shared/.../domain/usecase/GetSleepHistoryUseCase.kt  │ Métricas de historial, tendencia, deuda   │
  ├──────────────────────────────────────────────────────┼───────────────────────────────────────────┤
  │ shared/.../domain/model/SleepAnalysis.kt             │ Modelo con efficiency, fases, confianza   │
  ├──────────────────────────────────────────────────────┼───────────────────────────────────────────┤
  │ shared/.../domain/model/SleepHistory.kt              │ Modelo con promedios, consistencia, deuda │
  ├──────────────────────────────────────────────────────┼───────────────────────────────────────────┤
  │ composeApp/.../atomic/SleepQualityLabel.kt           │ Umbrales y etiquetas de calidad           │
  ├──────────────────────────────────────────────────────┼───────────────────────────────────────────┤
  │ composeApp/.../atomic/NightCard.kt                   │ Umbrales duplicados + vista por noche     │
  ├──────────────────────────────────────────────────────┼───────────────────────────────────────────┤
  │ composeApp/.../sleep/ui/SleepHistoryScreen.kt        │ Texto motivacional, selección de fondo    │
  └──────────────────────────────────────────────────────┴───────────────────────────────────────────┘

  ---
  En resumen: la calidad del sueño se reduce a eficiencia = tiempo dormido / tiempo en cama, y todo lo demás (etiquetas, tendencias, ánimos, fondos) se
  deriva de ese único número. Es un modelo simple pero efectivo, aunque no considera factores como la distribución de fases REM/profundo o la regularidad
  circadiana más allá de la desviación estándar del onset.

## Sensores

 El proyecto no usa SensorManager, SensorEventListener, ni ningún sensor crudo como acelerómetro, sensor de luz, ritmo cardíaco, etc. Todo se lo delegamos
  a Google Play Services.

  ---
  Lo que realmente usamos: Activity Recognition API

  El único punto de entrada es la API de reconocimiento de actividad de Google Play Services, a través de su funcionalidad específica de detección de sueño:

  // SleepMonitor.kt:42-46
  ActivityRecognition.getClient(context)
      .requestSleepSegmentUpdates(
          pendingIntent,                          // Apunta a SleepReceiver
          SleepSegmentRequest.getDefaultSleepSegmentRequest()  // Configuración por defecto
      )

  ¿Qué sensores usa Google Play Services por debajo?

  Google no documenta públicamente qué combinación exacta de sensores usa el SleepSegmentEvent, pero basado en la documentación oficial y el texto que
  mostramos al usuario, usa:

  - Acelerómetro — detecta movimiento del teléfono (si está quieto en la mesita de noche)
  - Sensor de luz ambiental — detecta si la habitación está oscura
  - Micrófono (posiblemente) — para detección de sonido ambiental (ronquidos, silencio)

  Nuestra app nunca accede a estos sensores directamente. Google Play Services los lee internamente, procesa los datos con sus modelos de machine learning,
  y nos entrega eventos ya procesados (SleepSegmentEvent) a través de un BroadcastReceiver.

  ---
  Flujo Completo

  Sensores del teléfono
    (acelerómetro, luz, etc.)
        ↓ [Google Play Services los lee internamente]
  Modelos de ML de Google Play Services
        ↓ [Procesa y detecta fases de sueño]
  SleepSegmentEvent
    (intent con lista de eventos)
        ↓ [BroadcastReceiver]
  SleepReceiver.onReceive()
        ↓ [Mapea cada evento]
  SleepSession (dominio)
    status → SleepPhase:
      1 = AWAKE
      2 = ASLEEP
      3 = LIGHT
      4 = DEEP
      5 = REM
        ↓ [Persiste vía SQLDelight]
  Base de datos local (SQLite)

  ---
  Archivos Clave del Stack Android

  ┌───────────────────────────────────────────────────┬──────────────────────────────────────────────────────────────┐
  │                      Archivo                      │                             Rol                              │
  ├───────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
  │ shared/src/androidMain/.../SleepMonitor.kt        │ Cliente de ActivityRecognition — inicia/detiene el monitoreo │
  ├───────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
  │ shared/src/androidMain/.../SleepReceiver.kt       │ BroadcastReceiver — recibe SleepSegmentEvent y los persiste  │
  ├───────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
  │ shared/src/androidMain/.../BootReceiver.kt        │ Rearranca el monitoreo tras reiniciar el dispositivo         │
  ├───────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
  │ shared/src/androidMain/.../SleepServiceLocator.kt │ Service locator que mantiene el Context y la DB              │
  ├───────────────────────────────────────────────────┼──────────────────────────────────────────────────────────────┤
  │ composeApp/src/androidMain/.../MainActivity.kt    │ Manejo de permisos ACTIVITY_RECOGNITION e inicio del monitor │
  └───────────────────────────────────────────────────┴──────────────────────────────────────────────────────────────┘

  ---
  Permisos Solicitados

  En AndroidManifest.xml solo hay dos permisos relacionados con sensores/sueño:

  <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION"/>
  <uses-permission android:name="com.google.android.gms.permission.ACTIVITY_RECOGNITION"/>

  No hay permisos de BODY_SENSORS, HIGH_SAMPLING_RATE_SENSORS, ACTIVITY_RECOGNITION_BODY_SENSORS, ni nada similar.

  ---
  Dependencia Única

  Solo una librería de Google Play Services:

  # gradle/libs.versions.toml
  play-services-location = "21.3.0"

  La API de SleepSegmentEvent viene empaquetada dentro del módulo play-services-location, no en un módulo separado. Es una API relativamente nueva (lanzada
  en 2023 como parte de la Actualización de Sueño de la Activity Recognition API).

  ---
  Lo que NO Usamos

  - ❌ Health Connect
  - ❌ Google Fit
  - ❌ SensorManager / sensores crudos
  - ❌ Wearables (aunque el enum SleepSource.WEARABLE existe, no hay implementación)
  - ❌ Entrada manual (SleepSource.MANUAL existe en el enum pero tampoco está implementado)
  - ❌ Micrófono directamente (no hay permiso RECORD_AUDIO)

  ---
  En resumen: delegamos completamente la detección del sueño a Google Play Services. La app solo consume los SleepSegmentEvent ya procesados, los mapea a
  nuestro modelo de dominio, y los persiste. No tocamos un solo sensor directamente.