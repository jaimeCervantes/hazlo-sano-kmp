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

## Estado

| Slice | Estado |
|---|---|
| 1 — la transición la elige qué clase de viaje es | **Hecho** — ver la [bitácora](transiciones-del-pilar-bitacora.md) |

**Pendiente de verlo en un teléfono:** si 450 ms sigue pareciendo más seco que el hermano, el número
está en `HazloMotion.DURATION_SCREEN_MILLIS` y el del hermano es 700.
