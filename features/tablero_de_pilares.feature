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

  @slice-2 @future
  Scenario: Un catálogo que tarda no apaga el tablero
    Given un pilar cuyo catálogo todavía no responde
    When abro su pestaña
    Then veo ya los campeones y los retos del pilar
    And donde van el resumen y la rejilla veo su esqueleto
    And no veo una ruedita a pantalla completa

  @slice-2 @future
  Scenario: El esqueleto tiene la forma de lo que va a llegar
    Given un tablero cargando
    When miro el hueco de la rejilla
    Then el esqueleto ocupa el mismo alto y la misma rejilla de dos columnas que las tarjetas reales
    And la pantalla no da un salto cuando llegan

  @slice-2 @future
  Scenario: Reintentar no vuelve a apagar lo que ya estaba
    Given un tablero cargado
    When pido actualizar
    Then los campeones y los retos siguen visibles
    And sólo la parte del catálogo vuelve a su esqueleto

  # ─────────── Slice 3: Inicio y Sueño cargan por partes ───────────

  @slice-3 @future
  Scenario: Inicio enseña los pilares antes de que llegue el feed
    Given que el contenido de Inicio todavía no está
    When abro la app
    Then veo la rejilla de pilares en cuanto está
    And el feed de la tribu muestra su esqueleto hasta que llega

  @slice-3 @future
  Scenario: El resumen de sueño no espera al catálogo
    Given un análisis de sueño ya calculado y un catálogo que tarda
    When abro la pestaña de Sueño
    Then el resumen de anoche se ve de inmediato
    And sólo la parte del catálogo muestra esqueleto

  # ─────────── Slice 4: la pantalla del pilar ───────────

  @slice-4 @future
  Scenario: Desde un pilar se llega a qué es ese pilar
    Given la pestaña de un pilar
    When toco el resumen del pilar
    Then se abre una pantalla que explica de qué va ese pilar
    And puedo volver al tablero donde estaba

  @slice-4 @future
  Scenario Outline: Los cuatro pilares tienen su explicación
    Given la pantalla de información de "<pilar>"
    Then encuentro de qué trata el pilar y qué propone hacer

    Examples:
      | pilar      |
      | Movimiento |
      | Nutrición  |
      | Mente      |
      | Sueño      |
