Feature: La búsqueda vive en la barra de arriba

  Context:
  - Problem: el buscador de verdad está enterrado dentro de una pestaña. Hay que estar en Inicio y
    bajar más allá de los pilares, los campeones y el encabezado del feed para llegar a él — y lo que
    contesta no tiene nada que ver con dónde está: `/api/search` devuelve publicaciones de cualquier
    pilar, y aparecen dentro del feed del Inicio. A la vez, las cuatro pestañas de pilar tienen su
    propio campo, con la misma forma redonda, que hace otra cosa: acota localmente lo que esa pestaña
    ya cargó. Dos campos iguales, dos promesas distintas, y ninguno donde el sitio hermano —o
    cualquier app de catálogo— lo pone.
  - Savings: una búsqueda en vez de dos que se parecen; el buscador alcanzable desde las cinco
    pestañas sin bajar nada; y los resultados dejan de fingir que son el feed del Inicio.
  - Why: los cuatro pilares son la promesa, pero sólo encuentras una publicación si ya sabes en qué
    pilar está. La barra de arriba es el único sitio del app que no obliga a saberlo de antemano.

  Como alguien que busca algo sin saber de qué pilar es
  Quiero escribirlo en la barra de arriba desde donde esté
  Para encontrarlo sin tener que adivinar primero en qué pestaña vivía

  Note: **esto no estrena la búsqueda, la muda.** El cliente de `/api/search`, su mapeador, la espera
  al teclear y el filtro por pilar existen desde el slice 2 de
  `publicaciones_en_el_home.feature`. Lo que cambia es dónde se escribe y dónde se lee la respuesta.
  Los seis escenarios `@slice-2` de aquel archivo **siguen valiendo tal cual**, ahora sobre la
  pantalla nueva; no se copian aquí para no tener dos sitios donde arreglar la misma frase.

  Note: y **antes de mudarla hay que arreglarla**. Los cuatro fallos de `@slice-3` de aquel mismo
  archivo viven en `HomeFeedViewModel`, que es justo la clase que este trabajo parte en dos. Mudar
  una búsqueda que se queda cargando para siempre sería estrenar el fallo en su sitio nuevo. Ese
  arreglo se ejecuta primero, con los escenarios que ya están escritos allí.

  Note: **la capa ya existe y no hay que inventar navegación.** `AppLayer` + `topLayer` es una pila
  de una sola capa encima del cuerpo principal, con su `BackHandler` y su `depth` para saber hacia
  dónde animar. `SleepHistory` ya hace exactamente lo pedido: tapa la barra de abajo, deja
  `selectedTab` intacto, y volver reaparece en la pestaña donde estabas. Los resultados son una capa
  más en esa misma lista.

  # ─────── Slice 1 — el campo en la barra y los resultados en su pantalla ───────

  @slice-1
  Scenario Outline: Buscar desde donde sea, sin perder dónde estaba
    Given que estoy en la pestaña <pestaña>
    When escribo «miel» en la barra de arriba
    Then veo los resultados que devuelve el buscador del sitio
    And abajo sigue elegida <pestaña>

    Examples:
      | pestaña    |
      | Inicio     |
      | Sueño      |
      | Nutrición  |
      | Movimiento |
      | Mente      |

  Note: la pestaña elegida no cambia **porque nadie la escribe**, no porque se restaure al volver.
  Los resultados son una capa encima; `selectedTab` vive debajo y no se entera.

  @slice-1
  Scenario Outline: Las dos formas de volver hacen lo mismo
    Given resultados de «miel» abiertos desde la pestaña Nutrición
    When vuelvo con <forma>
    Then vuelvo a ver la pestaña Nutrición
    And el término se olvida

    Examples:
      | forma                       |
      | la flecha de la barra       |
      | el gesto del sistema        |
      | borrar lo escrito           |

  Note: **el término se olvida al cerrar** a propósito. Guardarlo dejaría la barra enseñando una
  pregunta cuya respuesta ya no se está mirando, que es la clase de control que miente sobre lo que
  hace.

  @slice-1
  Scenario: Los resultados no se quedan en el pilar desde el que busqué
    Given que estoy en la pestaña Sueño
    When busco «miel»
    Then entre los resultados hay publicaciones de Nutrición
    And no se acotan a Sueño por haber buscado desde ahí

  @slice-1
  Scenario: La pantalla se abre al escribir, y la petición sigue esperando
    Given la barra vacía en la pestaña Inicio
    When escribo la primera letra
    Then se abre la pantalla de resultados en el acto
    And la petición al sitio sigue esperando a que deje de teclear

  @slice-1
  Scenario: Dos por fila en el teléfono en vertical
    Given resultados de «miel» con cinco publicaciones
    When los miro en un teléfono en vertical
    Then van dos por fila
    And el hueco de la última fila incompleta no ensancha la tarjeta que sí está

  @slice-1
  Scenario: Acotar los resultados a un pilar
    Given resultados de «miel» de los cuatro pilares
    When elijo el chip Nutrición
    Then sólo veo resultados de Nutrición
    And «miel» sigue escrito en la barra

  @slice-1
  Scenario: Bajar trae más resultados
    Given resultados de «miel» con más de los que caben
    When llego al final
    Then se piden más con el mismo término
    And se sabe si quedan porque se cuentan sobre el total y no por un campo

  @slice-1
  Scenario: Los resultados ocupan la pantalla entera
    Given resultados de «miel» abiertos desde la pestaña Nutrición
    Then la barra de abajo no se pinta
    And no hay una pestaña marcada que contradiga lo que estoy viendo

  Note: se tapa la barra de abajo en vez de dejarla puesta porque los resultados **no son de ninguna
  pestaña**: dejar «Nutrición» encendida mientras se leen resultados de los cuatro pilares diría algo
  falso. Es lo que ya hace el historial de sueño.

  @slice-1
  Scenario: Ajustes sigue alcanzable con los resultados abiertos
    Given resultados de «miel» abiertos
    When abro el menú de los tres puntos y elijo Ajustes
    Then se abren los Ajustes encima de los resultados
    And volver me devuelve a los resultados, no a la pestaña

  @slice-1
  Scenario: El Inicio conserva su feed y pierde su campo
    Given que abro el Inicio
    Then sigo viendo el feed con sus chips de pilar
    And ya no hay un campo de búsqueda dentro del Inicio
    And el único sitio donde se escribe una búsqueda es la barra de arriba

  Note: el Inicio conserva sus chips porque **acotar el feed por pilar no es buscar**: es la lista
  que se recorre sin preguntar nada. Los resultados llevan los suyos aparte, que acotan otra cosa.

  # ─── Slice 2 — el campo de las pestañas de pilar deja de competir con el de arriba ───

  Note: cuando la barra tenga una búsqueda de verdad, el campo que cada pestaña de pilar lleva dentro
  —que sólo acota lo que esa pestaña ya bajó— pasa a ser el segundo control con la misma forma y otra
  promesa. Se decide entonces si se quita o si dice lo que hace; ahora sería adivinar.

  @slice-2 @future
  Scenario: Dos campos con la misma forma no prometen cosas distintas

  # ─────── Slice 3 — lo que se buscó antes ───────

  @slice-3 @future
  Scenario: Al enfocar la barra aparecen las últimas búsquedas

  @slice-3 @future
  Scenario: Se puede borrar lo buscado antes

  # ─────────────────── Lo que esta spec no cubre ───────────────────

  Note: **abrir un resultado sigue sin llevar a ninguna parte**, igual que en el feed. No es descuido
  de este cambio: el buscador del sitio no devuelve `to`, y componer una URL adivinando su ruta para
  que lleve a un 404 es peor que no ofrecer enlace. Está anotado como slice 4 de
  `publicaciones_en_el_home.feature`, y sigue allí.

  Note: tampoco entra buscar rutas, sesiones ni nada del dispositivo. La barra busca **lo que el
  proyecto publica**, que es lo que `/api/search` sabe contestar.
