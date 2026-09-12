Feature: Moverse por el pilar de Movimiento se ve como lo que es

  Context:
  - Problem: ir de Grabar a Rutas se anima como entrar más adentro, con un deslizamiento lateral,
    cuando son hermanos: tienen su propia barra inferior, igual que las cinco pestañas de pilar. La
    transición no pregunta qué clase de viaje es — mira el tamaño de la pila, y `MovementNavState`
    apila los tres sitios igual que los dos detalles.
  - Savings: deja de enseñarse una dirección que no significa nada, y se quita la contradicción entre
    dos componentes que ya tenían la regla escrita y decían lo contrario.
  - Why: el pilar de Movimiento es donde se pasa el tiempo. Moverse por él tiene que sentirse como
    moverse por el app, no como otro app dentro.

  Como alguien que va y viene entre Grabar, Rutas y Salidas
  Quiero que cambiar de sitio se vea como cambiar de pestaña
  Para que la animación no me diga que entro a algún sitio cuando sólo estoy al lado

  Note: **la regla ya estaba escrita en dos sitios y el código no la cumplía.** `MovementBottomBar`
  dice «los tres sitios del pilar; un detalle no está aquí: no es un sitio, es algo que abriste», y
  `HazloTransitions` dice «lo que es hermano se funde, lo que es más profundo entra por el lado […]
  ninguna pantalla elige la suya — la elige de dónde a dónde se va». Esto no inventa un criterio:
  aplica el que ya había.

  @slice-1
  Scenario Outline: La transición la elige qué clase de viaje es
    Given que estoy en <desde>
    When voy a <hacia>
    Then la transición es <transición>

    Examples: entre sitios — se funden, pase lo que pase con la pila
      | desde   | hacia    | transición |
      | Grabar  | Rutas    | fundido    |
      | Rutas   | Salidas  | fundido    |
      | Salidas | Grabar   | fundido    |
      | Rutas   | Grabar   | fundido    |

    Examples: a un detalle — entra por el lado
      | desde   | hacia              | transición    |
      | Rutas   | detalle de ruta    | hacia adentro |
      | Salidas | detalle de salida  | hacia adentro |

    Examples: volviendo de un detalle — sale por donde entró
      | desde              | hacia    | transición  |
      | detalle de ruta    | Rutas    | hacia atrás |
      | detalle de salida  | Salidas  | hacia atrás |

  @slice-1
  Scenario: Ir y volver entre dos sitios se ve igual en los dos sentidos
    Given que voy de Grabar a Rutas
    And que vuelvo de Rutas a Grabar
    Then las dos veces se funde
    And ninguna de las dos enseña una dirección

  Note: eso importa porque **la pila sí distingue los dos sentidos**: ir a Rutas la hace crecer y
  volver a Grabar la corta. Si la transición siguiera preguntándole a la pila, el mismo toque en la
  misma barra se animaría hacia un lado o hacia el otro según por dónde hubieras pasado antes.

  @slice-1
  Scenario: Empezar una salida desde el detalle de una ruta entra, no se funde
    Given el detalle de una ruta guardada
    When elijo seguirla y empieza la grabación
    Then la transición es hacia adentro
    And no un fundido, porque no vengo de un sitio hermano

  # ─────── Slice 2 — el cromo no se anima con el contenido ───────

  Note: las tres pantallas del pilar llevan su `HazloTopAppBar` y su `MovementBottomBar` **dentro**
  del `Column` que el `AnimatedContent` anima, así que al cambiar de sitio parpadean las dos barras
  además del contenido. Las cinco pestañas de pilar hacen lo contrario —su `NavigationBar` está fuera
  del `AnimatedContent`— y por eso ésas sí se sienten bien.

  @slice-2
  Scenario: Cambiar de sitio no mueve las barras
    Given que estoy en «Mis rutas»
    When voy a «Mis salidas»
    Then sólo se funde el cuerpo
    And la barra de arriba y la de abajo se quedan donde están

  @slice-2
  Scenario Outline: Cada sitio dice cómo se llama y cuál está marcado
    Given el pilar abierto en <destino>
    Then la barra de arriba dice <título>
    And abajo está marcado <sitio>

    Examples:
      | destino               | título       | sitio    |
      | Grabar                | Grabar       | Grabar   |
      | Mis rutas             | Mis rutas    | Rutas    |
      | Mis salidas           | Mis salidas  | Salidas  |

  @slice-2
  Scenario Outline: Un detalle no lleva barra de abajo
    Given el pilar abierto en <destino>
    Then no hay barra de abajo
    And la de arriba lleva su flecha de volver

    Examples:
      | destino              |
      | detalle de una ruta  |
      | detalle de una salida|

  Note: **que el cromo no se anime no lo puede afirmar un escenario, y no se finge que sí.** Es una
  garantía de forma: las barras se pintan fuera del `AnimatedContent` que cambia de sitio, así que no
  hay estado en el que puedan animarse. Lo que sostiene esa forma es el compilador — las tres
  pantallas ya no aceptan a dónde ir, así que no pueden volver a dibujarse un cromo — y lo que sí se
  prueba es la tabla de arriba: de qué destino sale qué sitio y qué título.

  Note: componer las tres pantallas en un test no se puede hoy: cada una construye su propio
  ViewModel, que necesita la persistencia real. Es la misma razón por la que `MainScreen` tampoco se
  compone. Queda dicho en vez de escribir una prueba que mire otra cosa y parezca cubrirlo.

  # ─────── Slice 3 — el mapa sobrevive a salir y volver ───────

  Note: `MovementMap.android.kt` guarda la vista con `remember(context) { MapView(...) }` **dentro**
  del composable, así que salir de la composición la descarta y volver la reconstruye: otra vista,
  otra descarga del estilo, otras capas.

  @slice-3 @future
  Scenario: Volver al tracker no reconstruye el mapa

  @slice-3 @future
  Scenario: Y lo que se estaba grabando sigue dibujado al volver

  # ─────── Lo que cambia en el motor, y no se puede afirmar con un escenario ───────

  Note: el fundido de hermanos deja de ser un **relevo** —apagar lo viejo y luego encender lo nuevo—
  y pasa a ser **lo que llega encendiéndose encima de lo que se va**, que aguanta opaco debajo. El
  relevo dejaba un instante sin ninguna de las dos enteras, y sobre fondo oscuro eso parpadea; era lo
  que obligaba a disimularlo con un `scaleIn`, que ahora sobra y se va.

  Note: **el fundido cruzado tampoco**, que es lo que hace Navigation Compose por defecto y por tanto
  el proyecto hermano de Android: pone las dos pantallas a media opacidad a la vez y por debajo se ve
  el fondo del app a través de ambas. La primera versión de `Motion.kt` ya lo había llamado «turbio»
  y tenía razón.

  Note: y la duración de una pantalla entera sale a `DURATION_SCREEN_MILLIS`, 450 ms — entre los 260
  que había, que se leen como un corte, y los 700 del hermano, que son demasiado para algo que se
  hace veinte veces seguidas en una barra inferior. **Es el único sitio donde se toca.**
