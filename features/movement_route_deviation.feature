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

  Note: los umbrales salen de medir, no de suponer. Se replayaron las nueve trazas de campo midiendo
  cuánto se aleja la señal cruda del camino que de verdad se recorrió (`RouteDeviationCalibration`):

    | traza | qué fue                          | p90    | máximo   |
    | T2    | caminata y trote, 7,6 min        | 1,4 m  | 15,9 m   |
    | T3    | bici, 5,2 min                    | 6,9 m  | 19,4 m   |
    | T4    | bici 2 min + 31 min parado       | 8,6 m  | 291,5 m  |
    | T5    | bici, 5,7 min                    | 11,1 m | 14,4 m   |
    | T6    | bici, 5,6 min                    | 11,7 m | 21,3 m   |
    | T7    | bici, 9,4 min (la ruta guardada) | 2,5 m  | 4,2 m    |
    | T8    | bici siguiendo esa ruta, 6,4 min | 1,8 m  | 8,2 m    |

  (T1 y una novena captura no llegaron a tener recorrido aceptado y no dicen nada aquí.)

  Moviéndose de verdad, la señal nunca se aleja más de **21,3 m** del camino recorrido, y **ninguna
  lectura de ninguna traza en movimiento pasó de los 50 m**: el umbral deja algo más del doble de
  holgura sobre el peor caso medido. Los 291 m de T4 son el teléfono quieto bajo techo, y son la
  razón del escenario de abajo: parado, la distancia a la ruta es ruido y el aviso se calla.

  Note: **los 15 s de persistencia salen de una salida de campo, y corrigen los 30 s que no salían de
  nada.** T8 recorrió la ruta guardada de T7 y se desvió a propósito: la señal se alejó
  progresivamente hasta 77,7 m y volvió, dejando nueve lecturas por encima de los 50 m repartidas en
  16 s. **Con 30 s el aviso no llegó a hablar.** Replayando T8 con varias persistencias
  (`RouteFollowingReplay`), avisa con 0, 10 y 15 s, y se calla con 20 s o más. El otro lado —que una
  lectura mala suelta no dispare nada— resultó estar sostenido por los 50 m y no por este número: T5,
  que recorre las mismas calles sin desviarse, no produce un solo aviso ni con la persistencia a
  cero. Lo que sigue sin cubrir: un desvío más corto que 15 s se escapa igual.

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
      | 21     | 60       | no avisa | el peor desvío medido moviéndose fue de 21,3 m  |
      | 49     | 60       | no avisa | por debajo del umbral                           |

    Examples: fuera, y sostenido
      | metros | segundos | avisa | reason                                        |
      | 60     | 15       | avisa | pasa el umbral y aguanta la persistencia      |
      | 78     | 16       | avisa | el desvío que T8 midió en campo, con su forma |
      | 300    | 30       | avisa | inequívocamente fuera                         |

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
    And no hace falta esperar otros 15 s para que se calle

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

  Note: **la parada legítima sigue sin medirse.** La salida que calibró la persistencia se hizo sin
  pausas, así que la regla de callarse parado está sostenida por T4 —el teléfono quieto bajo techo— y
  no por un semáforo real en mitad de una ruta. Los 60 s de `isMoving` siguen sin contrastar contra
  una parada de verdad mientras se sigue un trazado.

  Note: la persistencia se cuenta en **tiempo y no en lecturas**, y ahí queda una holgura sin cerrar:
  con un receptor que reporte cada 10 s en vez de cada 2, los 15 s dejan de ser siete lecturas y
  pasan a ser una o dos. Por eso no se bajó más, aun cuando el falso positivo no aparece ni con la
  persistencia a cero.

  Note: el aviso en pantalla sigue sin mirarlo ningún test de host: sólo el estado que lo produce.
