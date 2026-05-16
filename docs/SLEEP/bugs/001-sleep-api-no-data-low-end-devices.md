# Bug #001: Google Sleep API no devuelve datos en dispositivos de gama baja con batería reducida

**Fecha:** 2026-05-14
**Severidad:** Media — no es crash, pero la funcionalidad principal del pilar queda inútil.
**Causa raíz:** Factores externos (dispositivo, batería, Google Play Services), no un bug de código.

---

## Síntoma

El `SleepSummaryCard` muestra `0 sesiones` y `Aún sin datos`. La BD SQLDelight está vacía para la ventana de análisis (6 PM de ayer → ahora). No hay error ni crash.

## Diagnóstico

El código de la app **funciona correctamente**:

1. `SleepMonitor.start()` se llama en `MainActivity.onCreate()` con permiso `ACTIVITY_RECOGNITION` concedido.
2. `SleepReceiver.onReceive()` está registrado y los logs muestran `No sleep segment events in intent` — Google Play Services no entregó eventos de sueño.
3. Los cambios recientes (commits `ae84bc5` a `4e28bbc`) son solo visuales (strings, imágenes, extracción de `SleepQualityLabel`) — ninguno toca la capa de datos.

## Factores concurrentes

En el caso reportado coincidieron tres factores que inhiben la detección:

| Factor | Mecanismo |
|---|---|
| **Smartphone gama baja** | Sensores menos precisos. Google Play Services reduce la prioridad de procesamiento de ML en estos dispositivos. |
| **Batería al 30%** | Android activa Doze / ahorro de batería agresivo, restringiendo `BroadcastReceiver` y procesamiento de sensores en segundo plano. |
| **Teléfono bajo la almohada** | El calor acumulado puede activar thermal throttling, degradando aún más el procesamiento. |

## Condiciones generales que inhiben la API

- Batería baja (< ~40%)
- Dispositivo gama baja (sensores económicos)
- Teléfono lejos de la cama (acelerómetro no capta micromovimientos)
- App en segundo plano sin `WorkManager` que mantenga vivo el monitor
- Google Play Services desactualizado
- Permiso `ACTIVITY_RECOGNITION` revocado (falla silenciosa)
- Thermal throttling por calor excesivo

## Verificación con logcat

```
adb logcat -s SleepMonitor:* SleepReceiver:*
```

| Log | Significado |
|---|---|
| `Sleep monitoring started successfully` | Monitor activo |
| `No sleep segment events in intent` | API no devolvió datos esta noche |
| `Permission denied: ACTIVITY_RECOGNITION not granted` | Permiso revocado |
| `Google Play Services error: code=X` | Error de Play Services |

## Plan de mitigación

- [ ] **Indicador visual** — badge en `SleepSummaryCard` que muestre estado del monitor (verde = activo, gris = inactivo).
- [ ] **Entrada manual** — implementar `SleepSource.MANUAL` como fallback para que el usuario registre horas de sueño cuando la API falla.
- [x] **WorkManager** — job periódico que re-suscriba `SleepMonitor` si se cayó (protege contra `force-stop` y Doze profundo). → Ver [Feature #002](../features/002-workmanager-periodic-refresh.md)
- [ ] **Mensaje contextual** — si `SleepMonitor.started == false`, mostrar causa probable en la UI (permiso denegado, Play Services no disponible).
- [ ] **Recordatorio de condiciones óptimas** — notificar al usuario que cargue el teléfono y lo deje cerca de la cama antes de dormir.

---

## Referencias

- `shared/src/androidMain/.../SleepMonitor.kt` — cliente de `ActivityRecognition`
- `shared/src/androidMain/.../SleepReceiver.kt` — `BroadcastReceiver` que persiste `SleepSegmentEvent`
- `shared/src/androidMain/.../BootReceiver.kt` — re-suscripción tras reinicio
- `composeApp/src/androidMain/.../MainActivity.kt` — manejo de permisos e inicio del monitor
- `docs/Sleep.md` — documentación general del flujo de sueño
