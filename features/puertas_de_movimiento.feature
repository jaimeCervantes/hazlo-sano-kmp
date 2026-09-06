Feature: El pilar de Movimiento tiene puerta

  Context:
  - Problem: el pilar con más código construido de los cuatro es el único cuya herramienta no se
    alcanza desde su propia pestaña. `MainScreen.kt:249` manda `BottomTab.Movimiento` directo a
    `PillarCatalogScreen`, y su propio comentario lo admite: «el tracker se sigue alcanzando desde
    Inicio, que es donde estaba». Para abrir una ruta guardada hacen falta cuatro toques —Inicio,
    tarjeta, Tracker, Historial, Mis rutas— y el tercero es una pantalla que no tiene nada que ver
    con rutas. Ya dentro, las salidas se listan como tres renglones de texto: no hay forma de
    reconocer cuál fue cada una sin abrirlas de una en una.
  - Savings: cuatro toques y un desvío por Inicio se convierten en uno; reconocer una salida pasa de
    abrir cinco pantallas a mirar la lista; y salir a seguir una ruta deja de exigir acordarse de
    por dónde se llegaba.
  - Why: «recorrer rutas» es la promesa central del pilar según su propio backlog de migración. Hoy
    el app sabe importar un GPX, guardarlo, dibujarlo y exportarlo — y no sabe llevarte a él.

  Como alguien que sale a caminar, correr o pedalear con el teléfono en el bolsillo
  Quiero empezar una salida y encontrar mis rutas desde la pestaña del pilar
  Para no tener que recordar por qué pantalla se llegaba a la que quiero

  Note: el patrón visual no se inventa aquí. El proyecto hermano ya lo fijó en su doc
  `034-panel-detalle-tienda-mapa`: «en desktop, panel lateral flotante; en móvil, hoja inferior
  superpuesta», sin empujar el contenido. Es el mismo patrón al que han convergido las apps de rutas,
  y ya es de la casa.

  Note: seguir una ruta **no** avisa de desvíos. La proyección sobre el trazado y el aviso de salirse
  son C3 del backlog de migración, una feature propia. Aquí la ruta se carga, se pinta y se graba
  encima.

  # ─────────────────── Slice 1 — la pestaña es la puerta del pilar ───────────────────

  @slice-1
  Scenario: Empezar una salida desde la pestaña del pilar
    Given que estoy en la pestaña "Movimiento"
    When toco "Empezar una salida"
    Then se abre el tracker listo para grabar
    And no he pasado por Inicio para llegar

  @slice-1
  Scenario Outline: Las dos puertas del pilar se abren en un toque
    Given que estoy en la pestaña "Movimiento"
    When toco "<puerta>"
    Then se abre "<pantalla>"
    And al volver estoy otra vez en la pestaña "Movimiento", no en el tracker

    Examples:
      | puerta      | pantalla    |
      | Mis rutas   | Mis rutas   |
      | Mis salidas | Mis salidas |

  @slice-1
  Scenario Outline: Sólo Movimiento tiene herramientas que ofrecer
    Given que estoy en la pestaña "<pilar>"
    When miro entre el resumen y los campeones
    Then <ve> las acciones del pilar

    Examples:
      | pilar      | ve       | reason                                              |
      | Movimiento | veo      | es el pilar que tiene herramienta propia            |
      | Nutrición  | no veo   | su pestaña es el catálogo publicado, y no cambia    |
      | Mente      | no veo   | igual que Nutrición                                 |

  @slice-1
  Scenario Outline: El tablero deja de enseñar dos veces lo mismo
    Given un pilar con catálogo
    When abro su tablero
    Then <ve> la sección "<seccion>"

    Examples: se van, porque "Buscar productos y servicios" ya las cubre
      | seccion     | ve     |
      | Servicios   | no veo |
      | Cerca de ti | no veo |

    Examples: se queda, porque tiene fecha y caduca
      | seccion         | ve  |
      | Próximos eventos | veo |

  @slice-1
  Scenario: Las etiquetas nuevas nacen en el catálogo de cadenas
    Given las tres acciones del pilar de Movimiento
    When se busca su texto en el código
    Then ninguna está escrita dentro de un Composable
    And las tres se leen de `Res.string`

  # ─────────────────── Slice 2 — mis salidas, con el trazado a la vista ───────────────────

  @slice-2 @future
  Scenario: Una salida se reconoce sin abrirla
    Given una salida grabada de 7,6 km
    When abro "Mis salidas"
    Then la tarjeta dibuja la silueta del recorrido junto a su distancia y su tiempo
    And puedo distinguirla de otra salida de la misma distancia por otro sitio

  @slice-2 @future
  Scenario: Una salida vieja no finge tener trazado
    Given una salida grabada antes de que se guardara el preview
    When aparece en la lista
    Then enseña su hueco en vez de un recuadro vacío

  # ─────────────────── Slice 3 — empezar desde cero, o siguiendo una ruta ───────────────────

  @slice-3 @future
  Scenario: Empezar sin ruta, como hasta ahora
    Given que estoy en la pantalla de empezar
    When elijo empezar desde cero
    Then la grabación arranca sin ninguna ruta cargada

  @slice-3 @future
  Scenario: Salir siguiendo una ruta guardada
    Given que tengo guardada la ruta "Cañada del Molino"
    When la elijo al empezar una salida
    Then la ruta se pinta en el mapa y mi trazado real se dibuja encima
    And la sesión guardada recuerda que iba siguiendo esa ruta

  @slice-3 @future
  Scenario: Salir siguiendo un GPX que traigo en el momento
    Given un archivo GPX que todavía no está guardado
    When lo elijo al empezar una salida
    Then queda guardado entre mis rutas
    And la salida arranca siguiéndolo

  # ─────────────────── Slice 4 — las rutas al lenguaje del ramo ───────────────────

  @slice-4 @future
  Scenario: El detalle de una ruta es mapa con hoja, no una ficha con un mapa dentro
    Given una ruta guardada
    When la abro
    Then el mapa ocupa la pantalla y sus cifras viajan en una hoja inferior

  @slice-4 @future
  Scenario: Una lista de rutas vacía ofrece la acción
    Given que todavía no tengo ninguna ruta
    When abro "Mis rutas"
    Then la pantalla me ofrece importar un GPX en vez de sólo explicarme que no hay nada
