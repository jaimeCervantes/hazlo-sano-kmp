Feature: Salirse de la ruta se nota mientras todavía se puede volver

  Context:
  - Problem: se puede salir con una ruta cargada y verla dibujada bajo el recorrido, pero el app no
    dice nada cuando te alejas de ella. En un sendero con bifurcaciones eso significa enterarte al
    llegar a un sitio que no reconoces, con el error ya andado — que es justo lo que se evita
    llevando la ruta.
  - Savings: los minutos y el desgaste de volver sobre lo andado, y el riesgo de perderse donde no
    hay a quién preguntar. Un aviso a los 50 m cuesta desandar 50 m; enterarse en la cima cuesta la
    tarde.
  - Why: «recorrer rutas» es la promesa central del pilar. Cargar la ruta y dibujarla es la mitad;
    saber si la estás siguiendo es la otra.

  Como alguien que sale al monte con una ruta cargada
  Quiero que el app me avise cuando me alejo del trazado
  Para poder volver a él andando metros y no kilómetros

  Note: **no hay «smart snap».** La referencia mueve tu posición sobre la ruta cuando estás cerca;
  aquí no, y es deliberado. Lo que se graba tiene que ser por dónde fuiste, no por dónde ibas a ir:
  pegar la posición al trazado haría que la salida guardada siguiera la ruta perfectamente aunque te
  hubieras desviado, que es exactamente la clase de mentira que B3 y B4 se pasaron cuatro slices
  quitando. Se mide y se avisa; no se corrige.

  Note: el aviso mira la posición **cruda**, la misma que el punto azul del mapa, y no la que el
  filtro confirma. El filtro retiene hasta un minuto antes de confirmar movimiento — es lo que hace
  que la distancia no mienta — y un aviso de desvío con un minuto de retraso llega cuando la
  bifurcación ya quedó atrás. El desvío no acumula nada: sólo compara una posición con una línea, así
  que no necesita la protección que el filtro da a la distancia.

  Note: los umbrales salen de medir, no de suponer. Se replayaron las cuatro trazas de campo midiendo
  cuánto se aleja la señal cruda del camino que de verdad se recorrió (`RouteDeviationCalibration`):

    | traza | qué fue                          | p90    | máximo   |
    | T2    | caminata y trote, 7,6 min        | 1,4 m  | 15,9 m   |
    | T3    | bici, 5,2 min                    | 6,9 m  | 19,4 m   |
    | T4    | bici 2 min + 31 min parado       | 8,6 m  | 291,5 m  |

  Moviéndose de verdad, la señal nunca se aleja más de ~20 m del camino recorrido: **50 m deja 2,5
  veces de holgura** sobre el peor caso medido. Los 291 m de T4 son el teléfono quieto bajo techo, y
  son la razón del escenario de abajo: parado, la distancia a la ruta es ruido y el aviso se calla.

  Note: los 30 s de persistencia son una elección de producto y **no una medida**, igual que los 60 s
  de B4: bastante rápido para servir en una bifurcación, bastante lento para que una lectura mala
  suelta no dispare nada. Es el número que la próxima salida de campo tiene que calibrar.

  # ─────────────────── Slice 1 — el aviso ───────────────────

  @slice-1
  Scenario Outline: Estar en la ruta o haberse salido de ella
    Given una salida siguiendo una ruta
    And que me estoy moviendo
    When llevo <segundos> s a <metros> m del trazado
    Then el app <avisa>

    Examples: dentro de lo que la señal puede equivocarse
      | metros | segundos | avisa    | reason                                          |
      | 0      | 60       | no avisa | encima de la ruta                               |
      | 20     | 60       | no avisa | el peor desvío medido moviéndose fue de 19,4 m  |
      | 49     | 60       | no avisa | por debajo del umbral                           |

    Examples: fuera, y sostenido
      | metros | segundos | avisa | reason                                    |
      | 60     | 30       | avisa | pasa el umbral y aguanta la persistencia  |
      | 300    | 30       | avisa | inequívocamente fuera                     |

    Examples: fuera, pero sin aguantar
      | metros | segundos | avisa    | reason                                        |
      | 60     | 10       | no avisa | una lectura mala suelta no es un desvío       |
      | 300    | 5        | no avisa | ni siquiera un salto grande, si no persiste   |

  @slice-1
  Scenario: Parado, la distancia a la ruta no significa nada
    Given una salida siguiendo una ruta
    And que llevo un rato sin moverme
    When la señal me coloca a 300 m del trazado
    Then el app no avisa de ningún desvío
    And vuelve a juzgar en cuanto me muevo otra vez

  @slice-1
  Scenario: Volver a la ruta calla el aviso
    Given una salida que ya avisó de un desvío
    When vuelvo a menos de 50 m del trazado
    Then el aviso desaparece
    And no hace falta esperar otros 30 s para que se calle

  @slice-1
  Scenario: Sin ruta no hay desvío del que hablar
    Given una salida sin ninguna ruta cargada
    When me muevo a donde sea
    Then el app nunca avisa de un desvío

  @slice-1
  Scenario: Una ruta de un solo punto no es un trazado
    Given una ruta guardada con un único punto
    When salgo siguiéndola
    Then el app no avisa de ningún desvío
    And no se cae al intentar medir contra ella

  @slice-1
  Scenario: La distancia se mide al trazado, no a sus vértices
    Given una ruta cuyos dos puntos están a 1 km uno del otro
    And que estoy justo en mitad del tramo recto entre ellos
    When se mide mi distancia a la ruta
    Then es prácticamente cero
    And no los 500 m que hay hasta el vértice más cercano

  @slice-1
  Scenario: Lo que se graba es por donde fui
    Given una salida siguiendo una ruta de la que me desvié 300 m
    When termino y la guardo
    Then el recorrido guardado pasa por donde estuve
    And no por el trazado de la ruta

  # ─────────────────── Lo que esta spec no cubre ───────────────────

  Note: **nadie ha medido un desvío real.** Las cuatro trazas son salidas libres, así que dan el
  ruido que el umbral tiene que dejar pasar pero no si un desvío de verdad se detecta a tiempo. Eso
  queda para la próxima salida: caminar una ruta conocida y salirse de ella a propósito. Hasta
  entonces, los 50 m están calibrados contra el falso positivo y los 30 s no están calibrados.
