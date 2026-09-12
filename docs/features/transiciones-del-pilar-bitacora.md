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

---

## Slice 2 — El cromo sale de la animación (2026-09-12)

**El usuario volvió a pedir que las pantallas del pilar fueran más fluidas** —«al abrir Mis rutas,
Mis salidas, Empezar una salida»— después del slice 1. No consta si llegó a verlo compilado; lo que
sí se puede afirmar es lo que el código decía, y decía que el slice 1 había arreglado **qué**
transición se usa sin tocar **qué** se estaba animando.

### Las barras se animaban con el contenido

Las tres pantallas del pilar estaban montadas así:

```kotlin
Column {
    HazloTopAppBar(...)      // ← se desvanecía
    <contenido>
    MovementBottomBar(...)   // ← se desvanecía
}
```

y ese `Column` entero vivía dentro del `AnimatedContent`. Ir de «Mis rutas» a «Mis salidas»
parpadeaba las dos barras además del cuerpo.

Las cinco pestañas de pilar —las que sí se sentían bien— hacen lo contrario: su `NavigationBar` vive
fuera del `AnimatedContent` de `MainScreen`. **Ésa era toda la diferencia**, y ninguna curva la
arregla.

### Dos niveles, y la regla pasa a estar en la forma

El arreglo no es mover las barras: es que el pilar tenga **dos animaciones anidadas**.

- **Fuera**, `MovementSection` distingue «los tres sitios» de «un detalle». Sólo cambia al abrir o
  cerrar un detalle, y eso sí es entrar y salir: se desliza.
- **Dentro de los sitios**, el cromo se pinta una vez y sólo se funde el cuerpo.

Y de ahí sale lo mejor del slice: **`movementTransition` sobra**. La función que el slice 1 añadió
para decidir «hermanos o profundidad» contestaba una pregunta que la estructura ya no hace —entre
hermanos no hay nada que animar salvo el cuerpo, porque los hermanos viven dentro del mismo cromo—.
Se borra ella y sus cinco pruebas. El conocimiento que sí hacía falta sobrevive en mejor sitio:
`movementPlaceOf`, que es **una sola definición** de qué cuenta como sitio y de la que salen las tres
cosas que dependen de eso.

### Un enum que estaba encerrado

`MovementPlace` vivía dentro de `MovementBottomBar`, que es sólo uno de los dos que necesitan la
distinción. El otro es la navegación. Se mueve a `presentation` — movimiento, no copia.

### El pilar sale de `MainScreen`

Eran ochenta líneas que obligaban a `MainScreen` a conocer los cinco destinos y sus argumentos.
Ahora es `MovementPillar(nav, routesViewModel, onOpenSettings)`, y con él se fueron seis imports de
pantallas que la principal no tiene por qué conocer.

### Dos fallos que me habría colado, vistos releyendo

**El primero habría anulado el slice entero.** El estado del nivel de fuera era
`MovementStep(section, depth)` — copiado de lo que había—, y **la profundidad de la pila cambia al
pasar de un sitio a otro**: ir a «Mis rutas» la hace crecer, volver a «Grabar» la corta. Con la
profundidad dentro, el nivel de fuera habría cambiado también entre hermanos y habría vuelto a animar
el cromo. Fuera la profundidad: `Places` es un `data object`, así que cambiar de sitio no lo mueve y
ahí no ocurre nada. La dirección tampoco sale ya de la pila — hacia un detalle se entra, hacia los
sitios se vuelve, y eso lo dice el propio destino.

**El segundo, el panel en blanco.**

`Places` leía `nav.lastPlace`. Al abrir un detalle, `lastPlace` **ya es ese detalle**, así que la
mitad que se desliza hacia fuera se habría quedado **pintando nada** mientras se va. Es exactamente
el problema que `lastPlace` existía para resolver un nivel más afuera, reaparecido un nivel más
adentro al partir la animación en dos.

De ahí `lastVisitedPlace`: la última de las tres pantallas que son un sitio, que sobrevive a abrir un
detalle. Son dos memorias porque son dos niveles, y tiene sus cuatro pruebas.

### Lo que **no** se pudo probar, dicho en vez de fingido

Que el cromo no se anime no lo puede afirmar un escenario: es una garantía de forma. Lo que la
sostiene es el compilador — las tres pantallas ya no aceptan a dónde ir, así que no pueden volver a
dibujarse un cromo.

Y componerlas en un test sigue sin poder ser: cada una construye su propio ViewModel, que necesita la
persistencia real. Es la misma razón por la que `MainScreen` tampoco se compone. Se dejó escrito en
la spec en lugar de escribir una prueba que mire otra cosa y parezca cubrirlo.

### Una decisión pequeña que se conserva a propósito

Arriba, el tracker dice «Movimiento» y abajo dice «Grabar». Al juntar los tres títulos en una tabla
la tentación es unificarlos; se dejó como estaba, con su prueba, porque es la clase de detalle que un
refactor cambia creyendo que arregla una inconsistencia.

### Archivos tocados

- **Presentación:** `MovementPlace.kt` (nuevo: el enum movido y `movementPlaceOf`),
  `MovementNavState.kt` (`lastVisitedPlace`), `MovementTransition.kt` (borrado).
- **UI:** `MovementPillar.kt` (nuevo: los dos niveles y el cromo), `MovementBottomBar.kt` (pierde el
  enum), las tres pantallas (pierden cromo y los callbacks de navegación), `MainScreen.kt` (delega).
- **Tests:** `MovementPlaceTest` (nuevo), `MovementPillarTitleTest` (nuevo),
  `MovementNavStateTest` (+4), `MovementTransitionTest` (borrado).

### Comandos y resultados

- `.\gradlew.bat :app:shared:jvmTest` → **484 pruebas, 0 fallos** (venían 479: +10 nuevas, −5 las de
  `MovementTransitionTest` al borrarse).
- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Dos incidencias del entorno, no del código

- **El daemon de Kotlin se cayó** a mitad de una tanda («Backend Internal error: Exception during IR
  lowering», y detrás una cascada de referencias sin resolver en `PublicationPagerTest`, que no se
  había tocado). Con `--stop` y repetir, BUILD SUCCESSFUL.
- **`PublicationPagerTest.starting another list while one is in flight asks anyway` falló una vez en
  `js, browser`** y pasó al repetir. Es **intermitente**, y queda anotado como tal: ese archivo no se
  tocó hoy y el `check` completo había pasado dos veces esta semana. Vale la pena mirarlo cuando se
  vuelva al buscador — una prueba que falla una de cada tres veces acaba enseñando a ignorar el rojo.

**Recap.** El cromo del pilar se pinta fuera de la animación que cambia de sitio, así que al ir de
«Mis rutas» a «Mis salidas» sólo se funde el cuerpo. Lo que lo hizo posible fue partir el pilar en
dos niveles —los sitios y los detalles—, y de ahí salió que la función que el slice 1 había añadido
para decidir la transición sobraba: la estructura ya lo dice. Dos fallos propios se cazaron
releyendo, y uno de ellos habría anulado el slice entero.

**Próximos pasos (opciones).** (1) Verlo en el teléfono, que es lo único que falta; (2) el slice 3
—que el mapa sobreviva a salir y volver—, dejado aparte a propósito para analizarlo con cuidado; (3)
`mapas-mas-fluidos.md`, con el gesto que suelta la cámara como primero.
