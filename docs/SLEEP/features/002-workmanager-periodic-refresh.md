# Feature #002: WorkManager — Re-suscripción periódica del Sleep Monitor

**Fecha:** 2026-05-15
**Relacionado con:** [Bug #001](../bugs/001-sleep-api-no-data-low-end-devices.md) (ítem de mitigación "WorkManager")

---

## Problema

La suscripción a la Sleep API de Google (`requestSleepSegmentUpdates`) se pierde silenciosamente cuando:

- El usuario hace **force-stop** de la app.
- Android aplica **Doze profundo**, matando el proceso y los `BroadcastReceiver` registrados.
- Google Play Services reinicia su servicio interno de `ActivityRecognition`.

El `BootReceiver` solo re-suscribía tras reinicio completo del dispositivo — no cubría estos casos intermedios. Si la suscripción se caía a las 10 PM, el usuario podía perder toda una noche de datos sin saberlo.

## Solución

Se implementa un **WorkManager** con `PeriodicWorkRequest` que re-ejecuta `SleepMonitor.start()` cada **6 horas**. Esto refresca la suscripción a la Sleep API periódicamente, asegurando que el `PendingIntent` que apunta a `SleepReceiver` esté siempre vigente.

### Arquitectura

```
WorkManager (cada 6h)
  └─ SleepMonitorWorker.doWork()
       └─ SleepMonitor.start(context)
            ├─ schedulePeriodicRefresh()   ← re-enqueue (KEEP policy)
            └─ requestSleepSegmentUpdates() ← re-suscribe a Google Play Services
                 └─ PendingIntent → SleepReceiver
```

`scheduledPeriodicRefresh` usa `ExistingPeriodicWorkPolicy.KEEP`: si el trabajo ya está encolado, no lo duplica.

### Archivos modificados

| Archivo | Cambio |
|---|---|
| `SleepMonitor.kt` | Añade `schedulePeriodicRefresh()` que encola el trabajo periódico. Se invoca al inicio de `start()`. |
| `SleepMonitorWorker.kt` (nuevo) | `CoroutineWorker` que llama a `SleepMonitor.start(applicationContext)`. |

### Por qué cada 6 horas

- Es un intervalo conservador que no afecta la batería (6 horas = 4 ejecuciones al día, cada una ligera: solo re-registra el `PendingIntent`).
- Cubre el caso de uso principal: si el monitor se cae durante el día, estará re-suscrito antes de la noche.
- `ExistingPeriodicWorkPolicy.KEEP` evita encolar trabajo redundante si `start()` se llama desde otros puntos de entrada (`MainActivity`, `BootReceiver`).

## Verificación con logcat

```
adb logcat -s SleepMonitorWorker:* SleepMonitor:*
```

| Log | Significado |
|---|---|
| `Periodic sleep monitor refresh starting` | WorkManager ejecutó el worker |
| `Sleep monitoring started successfully` | Re-suscripción exitosa |
| `Failed to schedule periodic sleep monitor refresh` | Error al encolar el trabajo (raro) |

---

## Referencias

- `shared/src/androidMain/.../SleepMonitor.kt:96-109` — `schedulePeriodicRefresh()`
- `shared/src/androidMain/.../SleepMonitorWorker.kt` — `CoroutineWorker`
- `shared/src/androidMain/.../BootReceiver.kt` — re-suscripción tras reinicio (complementario)
- [Bug #001](../bugs/001-sleep-api-no-data-low-end-devices.md) — plan de mitigación original
