Feature: El tablero de un pilar se arma por secciones y ninguna espera apaga la pantalla

  Context:
  - Problem: hoy una sola espera de red apaga la pantalla entera. `PillarCatalogScreen` pinta una
    ruedita centrada sobre fondo vacío mientras el endpoint de Next.js responde, y `HomeScreen` y
    `SleepScreen` hacen lo mismo con su `LoadingContent`. Lo que ya está en memoria —los campeones de
    la semana, los retos, el resumen de anoche— se esconde detrás de lo único que falta. Y encima el
    tablero de pilar no se parece al del proyecto Android: allí el orden es campeones, retos y
    búsqueda; aquí son métricas y carruseles, así que la misma app se lee distinta según la pestaña.
  - Savings: tiempo percibido de carga y frustración. La estructura aparece de inmediato y la red
    deja de secuestrar la pantalla; con mala señal se sigue pudiendo usar lo que no depende de ella.
  - Why: la app se está mudando a contenido remoto. Sin estado de carga por sección, cada endpoint
    que se sume vuelve a apagar la pantalla entera, y el pilar se lee como una promesa que tarda.

  Como alguien que abre un pilar desde el móvil con señal irregular
  Quiero ver de inmediato lo que ya está, y un esqueleto sólo donde todavía falta
  Para no mirar una pantalla en blanco cada vez que la red tarda

  Note: el orden lo fija el proyecto Android de referencia (`MindScreen.kt:42-83`): campeones
  semanales, retos activos y, al final, "Buscar productos y servicios". Los tres componentes ya
  están portados en `core/ui/components/sections/`; lo que falta es usarlos en ese orden.

  Note: no se inventan componentes nuevos donde ya hay uno. `HazloChampionsSection`,
  `HazloChallengesSection` y `HazloExploreProductsSection` son las mismas de Sueño e Inicio.

  # ─────────── Slice 1: el orden del tablero ───────────

  @slice-1
  Scenario: Un pilar se lee en el orden del proyecto de referencia
    Given el pilar de Movimiento con su catálogo ya descargado
    When abro su pestaña
    Then las secciones salen en este orden
      | posición | sección                      |
      | 1        | resumen del pilar            |
      | 2        | Campeones Semanales          |
      | 3        | Retos Activos                |
      | 4        | carruseles del catálogo      |
      | 5        | Buscar productos y servicios |

  @slice-1
  Scenario Outline: Cada pilar trae sus propios campeones y retos
    Given el pilar "<pilar>"
    When abro su pestaña
    Then el primer campeón se llama "<campeón>" y presume "<marca>"
    And el primer reto se titula "<reto>"
    And las dos secciones se pintan con el acento del pilar

    Examples:
      | pilar      | campeón    | marca       | reto               |
      | Movimiento | Mateo R.   | 42.5 km     | Montañero 50k      |
      | Nutrición  | Mateo R.   | 12 recetas  | Sin Azúcar Añadida |
      | Mente      | Ana V.     | 7 pausas    | Respira 4-7-8      |
      | Sueño      | Valeria N. | 7 noches    | Apagar pantallas   |

  @slice-1
  Scenario: Sueño lee sus campeones del mismo sitio que los otros tres pilares
    Given que los campeones de Sueño estaban escritos dentro de `MockSleepRepository`
    When los cuatro pilares pasan a leer sus campeones y retos de una sola fuente
    Then la pestaña de Sueño sigue enseñando a "Valeria N." y el reto "Apagar pantallas"
    And no queda una segunda copia de esos campeones en el repositorio de sueño

  @slice-1
  Scenario: Sueño conserva su resumen de anoche encima de todo
    Given la pestaña de Sueño con el análisis de la última noche
    When se le aplica el orden nuevo
    Then el resumen de anoche sigue siendo lo primero
    And debajo van campeones, retos y el catálogo, en ese orden
    And no aparece una segunda tarjeta de resumen a mitad de pantalla

  @slice-1
  Scenario: La rejilla del catálogo se llama por lo que hace
    Given un pilar con publicaciones
    When llego al final de su tablero
    Then la sección con el buscador se titula "Buscar productos y servicios"
    And ya no se titula "Todo el catálogo"

  @slice-1
  Scenario Outline: Una sección sin contenido no se pinta vacía
    Given un pilar <contenido>
    When abro su pestaña
    Then <resultado>

    Examples:
      | contenido               | resultado                            |
      | sin campeones esta semana | no veo la sección "Campeones Semanales" |
      | sin retos abiertos        | no veo la sección "Retos Activos"       |
      | con dos campeones         | veo la sección con esos dos             |

  Note: ningún título de estas secciones se queda escrito dentro de un `@Composable`. "Campeones
  Semanales", "Retos Activos" y "Buscar productos y servicios" pasan al catálogo de recursos, que es
  donde `AGENTS.md` exige que viva la copia visible. `HazloSectionDefaults` se queda sólo con
  dimensiones.

  # ─────────── Slice 2: esqueletos por sección ───────────

  Note: cada sección responde de su propia espera. El tablero es un solo `LazyColumn` en el que el
  resumen, los campeones y el catálogo se pintan o enseñan su hueco por separado; ya no hay un
  `when` que elija entre una ruedita a pantalla completa y el tablero entero.

  @slice-2
  Scenario: Un catálogo que tarda no apaga el tablero
    Given un pilar cuyo catálogo todavía no responde
    And unos campeones ya leídos, que no dependen de la red
    When abro su pestaña
    Then veo ya la sección de campeones
    And donde van el resumen y la rejilla veo su hueco
    And no veo una ruedita a pantalla completa

  @slice-2
  Scenario: El hueco tiene la forma de lo que va a llegar
    Given un tablero cargando
    When miro el hueco de la rejilla
    Then sus tarjetas salen de dos en dos, como las de la rejilla de verdad
    And las dos de una misma fila comparten fila y miden lo mismo

  @slice-2
  Scenario Outline: Cada sección enseña lo suyo según en qué estado esté
    Given un tablero cuyo catálogo <catálogo> y cuyos campeones <campeones>
    When abro la pestaña
    Then <resultado>

    Examples:
      | catálogo        | campeones       | resultado                                            |
      | todavía no está | ya están        | veo los campeones y el hueco del resumen             |
      | ya está         | todavía no están| veo el tablero y el hueco de los campeones           |
      | ya está         | ya están        | no veo ningún hueco                                  |
      | no se pudo leer | ya están        | se me dice que hace falta conexión, sin hueco eterno |

  @slice-2
  Scenario: Un catálogo que no se pudo leer no se queda esperando
    Given un pilar que nunca se descargó
    When lo abro sin red
    Then en lugar del hueco del catálogo se me dice que hace falta conexión
    And los campeones de la semana siguen visibles

  @slice-2
  Scenario: Reintentar no apaga lo que no depende del catálogo
    Given un tablero cargado
    When pido actualizar
    Then los campeones y los retos siguen visibles
    And sólo la parte del catálogo vuelve a su hueco

  # ─────────── Slice 3: Inicio y Sueño cargan por partes ───────────

  Note: el resumen de anoche no lo carga ninguna de las dos pantallas: llega ya calculado desde el
  pilar de sueño. Por eso va fuera del estado de Inicio, y por eso se ve aunque Inicio esté esperando.

  @slice-3
  Scenario: El resumen de anoche no espera a que cargue Inicio
    Given un análisis de sueño ya calculado
    And el contenido de Inicio todavía sin llegar
    When abro la app
    Then veo la tarjeta de la última noche
    And donde van los pilares, los campeones y el feed veo sus huecos

  @slice-3
  Scenario: Lo que ya está en Inicio no deja hueco detrás
    Given el contenido de Inicio ya cargado
    When lo miro
    Then veo la rejilla de pilares, los campeones y el feed
    And no queda ningún hueco en la pantalla

  @slice-3
  Scenario: Un Inicio que falló lo dice sin esconder lo de anoche
    Given un contenido de Inicio que no se pudo leer
    When abro la app
    Then se me dice que no se pudo cargar
    And la tarjeta de la última noche sigue visible

  @slice-3
  Scenario Outline: En Sueño, cada espera enseña lo suyo
    Given una pestaña de Sueño cuyo análisis <análisis>
    When la abro
    Then <resultado>
    And los campeones de la semana siguen visibles

    Examples:
      | análisis            | resultado                                       |
      | se está calculando  | veo el hueco del resumen                        |
      | ya está             | veo la tarjeta de la última noche, sin hueco    |
      | no se pudo leer     | se me dice que no se pudo leer, sin hueco       |
      | no tiene noche aún  | no veo ni tarjeta ni hueco                      |

  @slice-3
  Scenario: El resumen de sueño no espera al catálogo
    Given un análisis de sueño ya calculado y un catálogo que tarda
    When abro la pestaña de Sueño
    Then el resumen de anoche se ve de inmediato
    And sólo la parte del catálogo muestra su hueco

  # ─────────── Slice 4: la pantalla del pilar ───────────

  Note: el texto sale de hazlosano.com/pilares y vive en el catálogo de recursos, no en un modelo de
  dominio. Es contenido editorial fijo: no hay nada que ir a buscar, y `core` no alcanza los recursos
  que lo harían traducible.

  @slice-4
  Scenario: Desde un pilar se llega a qué es ese pilar
    Given la pestaña de un pilar con su tablero cargado
    When toco el botón de información de la tarjeta del pilar
    Then se abre una pantalla que explica de qué va ese pilar
    And puedo volver al tablero donde estaba

  @slice-4
  Scenario Outline: Los cuatro pilares cuentan lo suyo y proponen una práctica
    Given la pantalla de información de "<pilar>"
    Then leo "<titular>"
    And la práctica que propone es "<práctica>"

    Examples:
      | pilar      | titular                                    | práctica                          |
      | Movimiento | Movimiento natural, local y comunitario    | Movimiento vivo, local y funcional |
      | Nutrición  | Alimentación natural, nutritiva y local    | Cena real, local y al atardecer    |
      | Mente      | Mente, espíritu y comunidad cercana        | Presencia, paz y conexión local    |
      | Sueño      | Sueño y Descanso                           | Del atardecer al amanecer          |

  @slice-4
  Scenario: Sin catálogo todavía no hay puerta a la explicación
    Given un pilar que nunca se descargó
    When lo abro sin red
    Then no veo la tarjeta del pilar, y con ella tampoco su botón de información
