# Mapas más fluidos

**Estado: planificado, sin empezar.** Escrito el 2026-09-10 al comparar con el proyecto hermano de
Android (`~/AndroidStudioProjects/HazloSano`) porque el usuario notó que allí los mapas se sienten
más fluidos. Lo que sigue son hallazgos leídos en el código de los dos lados, no medidos en
dispositivo.

## Encuadre

- **Problem:** el mapa del pilar de Movimiento salta donde el del proyecto hermano vuela, y durante
  una grabación le quita la cámara al dedo que la está arrastrando. Las tres diferencias son de
  código, no de sensación: se pueden señalar con el número de línea.
- **Savings:** poder mirar hacia adelante durante una salida sin pelearse con la cámara, y que abrir
  una ruta deje de dar un tirón.
- **Why:** el mapa **es** el pilar de Movimiento. Es la pantalla donde se pasa el tiempo, no una
  ilustración.

## Los tres hallazgos

### 1. Teletransportamos la cámara donde ellos la vuelan

En `MovementMap.android.kt`, la función `frame()` encuadra con `moveCamera`:

```kotlin
moveCamera(CameraUpdateFactory.newLatLngZoom(center, PATH_ZOOM))      // sin área
moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, PATH_PADDING_PX))
```

El hermano, en `core/ui/components/RouteMap.kt`, hace lo mismo con `animateCamera`:

```kotlin
map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
```

`fitPathInView = true` lo usan **el detalle de una ruta y el detalle de una salida**, así que esto es
el primer segundo de cada pantalla de mapa que se abre: la nuestra da un tirón hasta el recorrido y
la suya entra volando.

**Cuidado al arreglarlo:** el encuadre ocurre una vez, cuando el recorrido termina de cargar, y la
cámara está entonces donde MapLibre la deja por defecto. Animar desde ahí sería un vuelo largo desde
el punto cero del mundo, que es peor que el tirón. El arreglo tiene dos mitades: **la primera colocación
se mueve, las siguientes se animan** — el mismo patrón que ya usa `centeredOnUser` para el usuario.

### 2. No escuchamos el gesto, así que la cámara le gana al dedo

El hermano registra un `OnMoveListener` y avisa hacia fuera cuando empiezas a arrastrar:

```kotlin
map.addOnMoveListener(object : MapLibreMap.OnMoveListener {
    override fun onMoveBegin(detector: MoveGestureDetector) { onMapMove?.invoke() }
    ...
})
```

Nosotros **no tenemos ninguna escucha**. Mientras grabas, cada posición nueva hace
`animateCamera(newLatLng(target))` y devuelve la cámara aunque la estés arrastrando. Mirar hacia
adelante durante una salida es pelearse con ella.

Esto es lo más cerca de un fallo de las tres: no es que se vea peor, es que no deja hacer algo.

### 3. Centrar en mí es un booleano de una vez, no un evento

El hermano lleva un contador y trata cada pulsación como un evento:

```kotlin
if (centerOnUserTrigger > lastProcessedTrigger && userLocation != null) {
    map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(...), 16.0))
    lastProcessedTrigger = centerOnUserTrigger
}
```

El nuestro usa `centeredOnUser` como bandera de una sola vez: la primera teletransporta con zoom, y
a partir de ahí sólo desplaza sin zoom. No hay forma de decir «vuelve a centrarme» y que se note.

## Lo que este trabajo **no** debe ser

**Copiar `RouteMap.kt`.** Es la fuente de las APIs correctas, no el listón de calidad —
`AGENTS.md` lo dice y este repositorio ya lo aplicó al portar las capas del mapa. En concreto, del
hermano **no** se traen: un `update` que hace de todo (estilo, datos, cámara y ajustes de UI en el
mismo bloque), el estado de la cámara repartido en tres variables sueltas de la función, ni el
encuadre decidido a base de comparar claves de cadena.

Lo que sí se trae es **qué** hace: volar en vez de saltar, soltar la cámara al primer gesto, y tratar
el centrado como un evento.

## Slices propuestos

| # | Slice | Por qué en este orden |
|---|---|---|
| 1 | El gesto suelta la cámara | Es lo único que impide hacer algo, no sólo verlo feo |
| 2 | Encuadrar volando, con la primera colocación instantánea | El primer segundo de cada pantalla de mapa |
| 3 | Centrar en mí como evento, con su botón | Depende del 1: sin soltar la cámara, no hace falta recuperarla |

## Preguntas abiertas

1. **¿Qué reanuda el seguimiento tras un gesto?** En el hermano no se ve: hay un `onMapMove` que
   avisa, pero quien lo recibe decide. Si es un botón, es el slice 3; si es un temporizador, hay que
   elegir cuánto.
2. **¿Los otros targets?** `MovementMap` es `expect`/`actual` y sólo Android pinta un mapa de verdad;
   iOS, JS y JVM son marcadores de posición. Esto es trabajo de `androidMain` y no cambia el contrato
   común — salvo que el slice 3 necesite un parámetro nuevo en el `expect`, que entonces sí lo tocan
   los cuatro.

## Estado

| Slice | Estado |
|---|---|
| 1 — el gesto suelta la cámara | Pendiente |
| 2 — encuadrar volando | Pendiente |
| 3 — centrar en mí como evento | Pendiente |

**Sin `.feature` todavía**, a propósito: los escenarios se escriben cuando el slice empiece, que es
cuando se sabe qué se puede afirmar sin un dispositivo delante.
