# Bitácora — Las transiciones del pilar de Movimiento

Roadmap: [`transiciones-del-pilar.md`](transiciones-del-pilar.md) · Spec:
[`transiciones_del_pilar.feature`](../../features/transiciones_del_pilar.feature)

---

## Slice 1 — La transición la elige qué clase de viaje es (2026-09-10)

**Lo reportado:** las transiciones del pilar de Movimiento no gustaban, y las del proyecto hermano de
Android sí.

### La regla ya estaba escrita en dos sitios, y el código no la cumplía

`MovementBottomBar` dice, en su propio KDoc:

> Los tres sitios del pilar. Un detalle no está aquí: no es un sitio, es algo que abriste.

Y `HazloTransitions`:

> lo que es hermano se funde, lo que es más profundo entra por el lado […] ninguna pantalla elige la
> suya — la elige de dónde a dónde se va.

Pero la transición del pilar **no preguntaba de dónde a dónde se iba: miraba el tamaño de la pila**.
Y `MovementNavState` apila los tres sitios igual que los dos detalles, así que ir de Grabar a Rutas
la hacía crecer y se animaba como entrar. Este slice no inventa un criterio: aplica el que ya había.

Lo que se siente distinto del cambio de pilar **era el mismo gesto animado con la regla equivocada**.

### Abrir el proyecto hermano dio la vuelta a la pregunta

No declara ninguna transición: su `NavHost` tiene siete `composable()` pelados, sin `enterTransition`
ni `exitTransition`. Lo que gustaba eran los valores por defecto de Navigation Compose.

| | Hermano | Nuestro (antes) |
|---|---|---|
| Duración | 700 ms | 260 ms |
| Forma | fundido cruzado, las dos a la vez | relevo: apagar lo viejo (150 ms) y **luego** encender lo nuevo (110 ms) |
| Movimiento | ninguno | deslizamiento en profundidad, `scaleIn(0.94)` en hermanos |

**No nos faltaba nada: nos sobraba.** Y lo que se nota no es la velocidad sino la forma — el relevo
deja un instante en que ninguna de las dos pantallas está entera, y sobre fondo oscuro eso parpadea.
Tanto, que la versión anterior había añadido un `scaleIn` para disimularlo.

### Ni relevo ni fundido cruzado

Las dos alternativas obvias estaban descartadas, cada una por su motivo:

- **El relevo** parpadea, que es el problema.
- **El fundido cruzado** —el del hermano— pone las dos pantallas a media opacidad a la vez, y por
  debajo se ve el fondo del app a través de ambas. La primera versión de `Motion.kt` ya lo había
  llamado «turbio», y tenía razón.

Lo que se hizo es la tercera: **la que llega se enciende por encima de la que se va, que aguanta
opaca debajo** (`targetContentZIndex`, y la salida con un `snap` al final en vez de un `fadeOut`).
No hay hueco ni transparencia. Es lo que el hermano aparenta hacer, hecho de forma que no dependa de
que el fondo perdone.

Y con eso el `scaleIn` sobra, porque estaba para tapar el parpadeo. Se va.

### Lo que no se copió del hermano

**Su uniformidad.** Anima igual ir a un hermano que entrar a un detalle, porque no distingue. El
nuestro sí: los detalles conservan el deslizamiento, que es lo que dicen los dos componentes citados
arriba. Ahí somos mejores y no había razón para renunciar.

Y **sus 700 ms**. Es lo que el usuario reconoció como más fluido, pero 700 ms por cada toque de una
barra inferior es demasiado para algo que se hace veinte veces seguidas. Quedan 450 en
`HazloMotion.DURATION_SCREEN_MILLIS`, el único sitio donde se toca.

### El criterio sale a una función pura

`movementTransition(from, to, goingDeeper)` vive en `presentation` con su tabla de pruebas, como
`topLayer` y `MovementNavState`. `goingDeeper` **sólo decide el sentido del deslizamiento cuando hay
deslizamiento**: entre dos sitios no lo hay, pase lo que pase con la pila.

Eso último tiene su prueba con las seis combinaciones de sitios **por los dos valores de
`goingDeeper`**, doce en total, porque es justo lo que estaba mal: la pila crece al ir a Rutas y
mengua al volver a Grabar, así que el mismo toque en la misma barra se animaba hacia un lado o hacia
el otro según por dónde hubieras pasado antes.

### Archivos tocados

- **Motion:** `Motion.kt` (`DURATION_SCREEN_MILLIS`, `siblings()` reescrito, fuera `scaleIn`).
- **Presentación:** `MovementTransition.kt` (nuevo).
- **UI:** `MainScreen.kt` (la transición del pilar pregunta por el viaje).
- **Tests:** `MovementTransitionTest` (nuevo, 5 pruebas, 20 viajes afirmados).

### Comandos y resultados

- `.\gradlew.bat :app:shared:jvmTest` → **479 pruebas, 0 fallos** (venían 474).
- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Sin comprobar

- **Nada de esto se ha visto en movimiento.** Lo que se prueba es el criterio, que es lo único que
  puede estar mal sin mirar; que 450 ms sea el número y que encenderse encima se vea mejor que
  relevarse son cosas de un teléfono.
- **El cambio de `siblings()` afecta también a las cinco pestañas de pilar**, que ya usaban esta
  transición. Es lo pretendido —era el mismo parpadeo— pero conviene mirarlo ahí también.

**Recap.** Moverse entre los tres sitios del pilar se funde, como cambiar de pestaña, porque la
transición pasó a preguntar qué clase de viaje es en vez de cuánto mide la pila — que es la regla que
`MovementBottomBar` y `HazloTransitions` ya tenían escrita. Y el fundido dejó de ser un relevo que
parpadeaba sin adoptar el cruzado que enturbia: lo que llega se enciende encima.

**Próximos pasos (opciones).** (1) Verlo en el teléfono y mover los 450 ms si hace falta; (2)
[`mapas-mas-fluidos.md`](mapas-mas-fluidos.md), que salió de la misma comparación y tiene tres
hallazgos con más peso que éste; (3) lo que quedaba pendiente de la búsqueda.
