# Las transiciones del pilar de Movimiento

Spec: [`transiciones_del_pilar.feature`](../../features/transiciones_del_pilar.feature) · Hermano de:
[`mapas-mas-fluidos.md`](mapas-mas-fluidos.md), que salió de la misma comparación

## Encuadre

- **Problem:** ir de Grabar a Rutas se anima como entrar más adentro, con un deslizamiento lateral,
  cuando son hermanos: tienen su propia barra inferior, igual que las cinco pestañas de pilar. Y el
  fundido que sí se usa entre hermanos parpadea.
- **Savings:** deja de enseñarse una dirección que no significa nada, y se quita la contradicción
  entre dos componentes que ya tenían la regla escrita y hacían lo contrario.
- **Why:** el pilar de Movimiento es donde se pasa el tiempo. Moverse por él tiene que sentirse como
  moverse por el app, no como otro app dentro.

## Lo que se encontró comparando con el proyecto hermano

El usuario dijo que prefiere las transiciones del proyecto hermano de Android
(`~/AndroidStudioProjects/HazloSano`). Al abrirlo apareció que **no declara ninguna**: su `NavHost`
tiene siete `composable()` pelados, sin `enterTransition` ni `exitTransition`. Lo que gusta son los
valores por defecto de Navigation Compose — un fundido cruzado de 700 ms, idéntico para todo.

| | Hermano | Nuestro (antes) |
|---|---|---|
| Duración | 700 ms | 260 ms |
| Forma | fundido cruzado, las dos a la vez | relevo: se apaga lo viejo (150 ms) y **luego** enciende lo nuevo (110 ms) |
| Movimiento | ninguno | deslizamiento en profundidad, y `scaleIn(0.94)` en hermanos |

**No nos faltaba nada: nos sobraba.** Y la diferencia que se nota no es la velocidad, es la forma. El
relevo deja un instante en que ninguna de las dos pantallas está entera, y sobre fondo oscuro eso
parpadea — tanto, que hubo que añadir un `scaleIn` para disimularlo.

## Decisiones

1. **Ni relevo ni fundido cruzado: lo que llega se enciende encima.** El relevo parpadea. El fundido
   cruzado —el del hermano— pone las dos pantallas a media opacidad a la vez y por debajo se ve el
   fondo del app a través de ambas; la primera versión de `Motion.kt` ya lo había llamado «turbio» y
   tenía razón. Encendiendo la que llega **por encima** de la que se va, que aguanta opaca debajo, no
   hay ni hueco ni transparencia. Es lo que el hermano aparenta hacer, hecho de forma que no dependa
   de que el fondo perdone.
2. **Fuera el `scaleIn`.** Estaba para tapar el parpadeo. Sin parpadeo que tapar, es un gesto de más.
3. **450 ms**, en `HazloMotion.DURATION_SCREEN_MILLIS`. Entre los 260 que había, que se leen como un
   corte, y los 700 del hermano, que son demasiado para algo que se hace veinte veces seguidas en una
   barra inferior. **Es el único sitio donde se toca**; si hay que moverlo, se mueve ahí.
4. **Los detalles conservan el deslizamiento.** Sí son más adentro, y es lo que dicen `MovementBottomBar`
   («un detalle no es un sitio, es algo que abriste») y el propio `HazloTransitions` («la elige de
   dónde a dónde se va»). El hermano no distingue; nosotros sí, y esa es la parte donde somos mejores.

## El slice

**Alcance.**

- La transición del pilar la elige **qué clase de viaje es**, no el tamaño de la pila. Sale a
  `movementTransition(from, to, goingDeeper)`, una función pura en `presentation` con su tabla de
  pruebas, como ya hacen `topLayer` y `MovementNavState`.
- `siblings()` pasa de relevo a encenderse encima, y pierde el `scaleIn`.
- La duración de pantalla sale a una constante.

**Criterios de aceptación.**

- Entre Grabar, Rutas y Salidas se funde, **pase lo que pase con la pila** — ir y volver se ven igual.
- Abrir el detalle de una ruta o de una salida entra por el lado; volver sale por donde entró.
- Empezar una salida desde el detalle de una ruta entra, porque el detalle no es un sitio hermano.

**Lo que no se puede afirmar con un escenario:** que 450 ms sea el número, y que encenderse encima se
vea mejor que relevarse. Eso es de mirarlo en un teléfono.

## Lo que faltaba, visto al probarlo (2026-09-12)

El slice 1 arregló **qué** transición se usa. Seguía sin sentirse fluido, y al mirar la estructura
aparecieron dos causas que ninguna curva arregla.

### El cromo se anima con el contenido

Las tres pantallas del pilar están montadas igual:

```kotlin
Column {
    HazloTopAppBar(...)      // ← se desvanece
    <contenido>
    MovementBottomBar(...)   // ← se desvanece
}
```

y ese `Column` entero vive **dentro** del `AnimatedContent`. Así que ir de «Mis rutas» a «Mis
salidas» no cambia el contenido: **parpadean también las dos barras**.

Las cinco pestañas de pilar, que sí se sienten bien, hacen lo contrario: el `Scaffold` con su
`NavigationBar` está **fuera** del `AnimatedContent` y sólo el cuerpo se funde. La barra no se mueve
ni un píxel. Esa es toda la diferencia.

### Volver al tracker reconstruye el mapa

`MovementMap.android.kt` guarda la vista con `remember(context) { MapView(context, options) }`, **dentro
del composable**. Al salir de la composición se descarta; al volver se crea otra vista, se vuelve a
bajar el estilo y se rehacen las capas. Ir a «Mis rutas» y volver a «Empezar una salida» paga eso
entero.

## Slice 2 — el cromo sale de la animación

**Alcance.**

- Las barras de arriba y de abajo del pilar se pintan **una vez, fuera** del `AnimatedContent`, en la
  rama de Movimiento de `MainScreen`. Las tres pantallas dejan de dibujarlas.
- Qué título lleva la barra y qué sitio está marcado abajo sale de una función pura sobre el
  destino, con su tabla de pruebas — como `movementTransition`.
- La barra de abajo se pinta **sólo en los tres sitios**. Los dos detalles no la llevan, que es lo
  que `MovementBottomBar` ya dice de sí misma.

**Criterios de aceptación.**

- Al cambiar de sitio, sólo se funde el cuerpo: las dos barras no parpadean.
- Ninguna de las tres pantallas dibuja su propio cromo.
- El detalle de una ruta y el de una salida siguen sin barra inferior, y con su flecha de volver.

## Slice 3 — el mapa sobrevive a salir y volver

**Alcance.** Que la `MapView` del tracker no se destruya al navegar dentro del pilar.

**El riesgo está dicho de antemano:** `MovementMap.android.kt` es un archivo con historia — modo
textura, callbacks de estilo, dibujar en el `update` y no en efectos. Cada uno de esos comentarios es
un fallo que costó encontrar. Retener la vista toca justo eso, así que este slice se mide antes y
después en un dispositivo, y si no se puede medir, no se da por bueno.

**Criterios de aceptación.** Volver a «Empezar una salida» desde otro sitio del pilar no vuelve a
bajar el estilo ni a recrear las capas. Se comprueba por el log que el propio archivo ya escribe
(`style loaded, layers=…`): hoy aparece cada vez; después, una sola.

## Estado

| Slice | Estado |
|---|---|
| 1 — la transición la elige qué clase de viaje es | **Hecho** — ver la [bitácora](transiciones-del-pilar-bitacora.md) |
| 2 — el cromo sale de la animación | **Hecho** — ver la [bitácora](transiciones-del-pilar-bitacora.md) |
| 3 — el mapa sobrevive a salir y volver | **Aparte**, por decisión del usuario (2026-09-12): se analiza y se hace con más cuidado, no en esta tanda |

**Pendiente de verlo en un teléfono:** si 450 ms sigue pareciendo más seco que el hermano, el número
está en `HazloMotion.DURATION_SCREEN_MILLIS` y el del hermano es 700.
