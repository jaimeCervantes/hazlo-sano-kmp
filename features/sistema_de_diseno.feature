Feature: El app habla el idioma de diseño del sitio

  Context:
  - Problem: el app y hazlosano.com son la misma marca sobre la misma base de datos y no se parecen.
    El app se quedó con la paleta que el sitio ya jubiló, y no es cuestión de gusto: tres de los
    cuatro colores de pilar no llegan a AA con texto encima, y el verde de marca tampoco —
    `Theme.kt` declara `primary = #538F39` con `onPrimary = White`, que da 3.92:1. Encima hay 121
    cadenas en español contra 54 en inglés, 46 textos escritos dentro de Composables, y ninguna
    forma de elegir tema ni idioma; el sitio ya tiene las dos.
  - Savings: una sola paleta que mantener en vez de dos que divergen, y una deuda que deja de crecer
    con cada pantalla nueva. Quien prefiere el modo claro deja de depender de lo que decida su
    teléfono, y quien no habla español deja de encontrarse media app sin traducir.
  - Why: el catálogo del app ya se lee del sitio, y el login va a atarlo a la misma cuenta. Si el
    contenido es el mismo y la cuenta va a ser la misma, parecer dos productos distintos es un
    defecto y no una decisión.

  Como alguien que usa Hazlo Sano en el teléfono y en el navegador
  Quiero que las dos cosas se vean como el mismo producto y se puedan leer
  Para no dudar de si estoy en el sitio correcto, ni forzar la vista para leer una cifra

  Note: el fallo de fondo no son los hex, es que el app usa **un solo color por pilar para dos
  trabajos distintos** — rellenar una insignia y escribir una cifra. El sitio ya descubrió que
  ninguna semilla de marca sirve para las dos cosas y respondió con una rampa de tres papeles por
  pilar: `solid` rellena, `soft` es el fondo tenue, `ink` escribe sobre `soft`. Es esa separación,
  y no la lista de colores, lo que este slice trae.

  Note: los contrastes de abajo están medidos con la fórmula de WCAG sobre los hex de los dos repos.
  Las dos cifras que el sitio documenta por su cuenta —3.92 para la semilla verde y 3.98 para la
  naranja— salen idénticas, que es lo que da confianza en las demás.

  # ─────────────────── Slice 1 — la paleta del sitio llega al tema ───────────────────

  @slice-1
  Scenario Outline: El relleno de un pilar sostiene el texto blanco que lleva encima
    Given el color de relleno "<relleno>" del pilar "<pilar>"
    When se mide su contraste contra el blanco "#FFFFFF"
    Then el contraste llega al menos a 4.5:1
    And deja atrás el "<jubilado>" que el app traía, que daba <antes>

    Examples: los cuatro pilares en tema claro
      | pilar     | relleno | jubilado | antes | por qué se jubila                       |
      | SLEEP     | #7C3AED | #8B5CF6  | 4.24  | el violeta anterior se quedaba corto    |
      | NUTRITION | #DD340D | #F0380E  | 3.98  | es la semilla del círculo del logo      |
      | MOVEMENT  | #408410 | #538F39  | 3.92  | es la semilla del corazón del logo      |
      | MIND      | #0369A1 | #38BDF8  | 2.14  | el azul cielo era el peor de los cuatro |

  @slice-1
  Scenario Outline: La tinta de un pilar se lee sobre su propio papel tenue
    Given el papel tenue "<soft>" del pilar "<pilar>" en tema "<tema>"
    And su tinta "<ink>"
    When se mide el contraste de la tinta contra el papel
    Then el contraste llega al menos a 4.5:1

    Examples: tema claro
      | pilar     | tema   | soft    | ink     |
      | SLEEP     | claro  | #F5F3FF | #7C3AED |
      | NUTRITION | claro  | #FDE3DD | #C52E0B |
      | MOVEMENT  | claro  | #E8F6DF | #3C7B0F |
      | MIND      | claro  | #F0F9FF | #0369A1 |

    Examples: tema oscuro — la rampa se invierte, el requisito no
      | pilar     | tema   | soft    | ink     |
      | SLEEP     | oscuro | #2E1065 | #C4B5FD |
      | NUTRITION | oscuro | #36150D | #F4522E |
      | MOVEMENT  | oscuro | #1B2D0F | #5DBF17 |
      | MIND      | oscuro | #0C2A3B | #38BDF8 |

  @slice-1
  Scenario: El botón primario deja de escribir en blanco sobre un verde que no lo sostiene
    Given el tema claro con su par "primary" y "onPrimary"
    When se mide el contraste del par
    Then "primary" es "#3F6F2A" y no la semilla "#538F39" del logo
    And el contraste es de 5.97:1, donde antes era de 3.92:1

  @slice-1
  Scenario: Un pilar entrega tres papeles y no un color suelto
    Given el pilar "MOVEMENT"
    When la interfaz le pide su paleta
    Then recibe tres papeles, cada uno con su trabajo
      | papel | valor   | trabajo                                  |
      | solid | #408410 | rellenar una insignia, con texto blanco  |
      | soft  | #E8F6DF | el fondo tenue de una tarjeta del pilar  |
      | ink   | #3C7B0F | escribir una cifra sobre ese fondo tenue |
    And no queda ninguna forma de pedirle un color sin decir para qué lo quiere

  @slice-1
  Scenario: El papel del app deja de ser blanco frío
    Given el tema claro
    When se lee el color de fondo
    Then es el papel cálido "#FAF7F1" del sitio
    And no el "#FFFBFF" que el app traía

  Note: la paridad entre temas se comprueba distinto que en el sitio, porque el riesgo es distinto.
  Allí el tema oscuro se declara dos veces —una media query y un atributo— porque CSS no deja
  compartir un bloque, así que su prueba compara las dos copias. Aquí no hay dos copias: hay un tipo
  con dos instancias, y el compilador ya obliga a que ningún token se quede sin valor. Lo que el
  compilador no ve es un token copiado del claro por descuido, que deja el oscuro ilegible.

  @slice-1
  Scenario: Lo que tiene que cambiar con el tema, cambia
    Given los tokens de superficie, de texto y de borde
    When se comparan los del tema claro con los del oscuro
    Then ninguno vale lo mismo en los dos
    And el papel tenue y la tinta de cada pilar también se invierten

  @slice-1
  Scenario: Lo que no debe cambiar con el tema, no cambia
    Given el relleno de cada pilar
    When se compara entre temas
    Then es el mismo en claro y en oscuro
    And sigue llevando texto blanco encima en los dos

  @slice-1
  Scenario Outline: La paleta jubilada no puede volver por una fusión
    Given el color "<jubilado>" con el que el pilar "<pilar>" se pintaba antes
    When se lee la paleta del pilar
    Then ni su relleno ni su tinta son ese color

    Examples:
      | pilar     | jubilado |
      | SLEEP     | #8B5CF6  |
      | NUTRITION | #F0380E  |
      | MOVEMENT  | #538F39  |
      | MIND      | #38BDF8  |

  @slice-1
  Scenario: El color de un pilar deja de escribirse a mano en las pantallas
    Given que "MovementHistoryScreen", "RoutesScreen" y "TrackerScreen" pintan hoy sus cifras con la
      constante "PillarMovement"
    When se busca un color literal de pilar fuera del tema
    Then no aparece ninguno

  # ─────────────────── Slice 2 — las dos voces de la marca ───────────────────

  @slice-2 @future
  Scenario: Un titular suena a marca y una cifra suena a interfaz
    Given un titular de pilar y una etiqueta de dato
    When se pintan
    Then el titular usa la serif editorial y la etiqueta la sans humanista
    And ninguna de las dos se le pide a un servidor de fuentes ajeno

  @slice-2 @future
  Scenario: La escala de tamaños es la del sitio
    Given la escala tipográfica del sitio
    When se mapea sobre los estilos de Material 3
    Then los tamaños coinciden con los que usa el sitio

  # ─────────────────── Slice 3 — el tema se elige ───────────────────

  @slice-3 @future
  Scenario: El tema deja de decidirlo el teléfono
    Given que el sistema está en modo oscuro
    When se elige el tema claro en los ajustes
    Then el app se pinta en claro sin reiniciarse
    And sigue en claro la próxima vez que se abre

  @slice-3 @future
  Scenario: Seguir al sistema es una opción, y es la de partida
    Given un app recién instalado
    When se abre por primera vez
    Then sigue el tema del sistema
    And esa opción se puede volver a elegir después de haber fijado uno

  # ─────────────────── Slice 4 — ningún texto vive fuera del catálogo ───────────────────

  @slice-4 @future
  Scenario: Ninguna pantalla lleva su copia escrita dentro
    Given los 46 textos hoy escritos dentro de Composables
    When se busca un texto visible fuera del catálogo de recursos
    Then no aparece ninguno

  @slice-4 @future
  Scenario: Un ViewModel no redacta lo que se lee
    Given que "RoutesViewModel" expone hoy un mensaje ya redactado
    When informa del resultado de importar una ruta
    Then entrega un tipo de mensaje y no una cadena
    And es la UI quien elige con qué palabras decirlo

  @slice-4 @future
  Scenario: El inglés cubre lo mismo que el español
    Given el catálogo en español con sus 121 cadenas
    When se compara con el catálogo en inglés, que hoy tiene 54
    Then ninguna clave del español falta en el inglés

  # ─────────────────── Slice 5 — el idioma se elige ───────────────────

  Note: al construirlo se descubrió que el camino previsto no existe. En Compose Multiplatform 1.11
  la interfaz `ComposeEnvironment`, `LocalComposeEnvironment` y el constructor de
  `ResourceEnvironment` son **internos**, así que no se puede proveer un entorno de recursos con
  otro idioma desde fuera de la librería. Lo que sí es alcanzable es su entrada: el entorno se
  calcula desde `Locale.current` y se memoiza con él como clave, así que mover el locale de la
  plataforma mueve el idioma de los recursos.
  Consecuencia, y por eso los escenarios de abajo distinguen plataformas: en Android y escritorio el
  cambio es inmediato; en iOS el locale sólo se mueve reiniciando el app, y en la web no se mueve en
  absoluto. Allí el ajuste se guarda, el app sigue al sistema —que es lo que hacía antes— y la
  pantalla lo dice en vez de ofrecer un control que no hace nada.

  @slice-5
  Scenario: El idioma se cambia sin cerrar la app
    Given el app en español
    When se elige inglés en los ajustes
    Then los textos visibles pasan a inglés de inmediato
    And siguen en inglés la próxima vez que se abre

  @slice-5
  Scenario: Se puede volver a seguir al sistema
    Given el app con el inglés elegido a mano
    When se elige "seguir al sistema"
    Then vuelve al idioma con el que arrancó
    And no se queda en inglés para siempre

  @slice-5
  Scenario Outline: Donde el idioma no se puede aplicar, se dice
    Given el app corriendo en "<plataforma>"
    When se abren los ajustes
    Then <dice> que este ajuste no cambia lo que se ve

    Examples: lo aplican en caliente
      | plataforma | dice    |
      | Android    | no dice |
      | escritorio | no dice |

    Examples: no lo aplican — el locale no se puede mover desde el app
      | plataforma | dice |
      | iOS        | dice |
      | web        | dice |

  @slice-5
  Scenario: Los dos ajustes no se pisan
    Given el tema en oscuro y el idioma en inglés
    When se lee cada uno
    Then cada uno conserva lo suyo
    And añadir el idioma no necesitó ninguna migración nueva
