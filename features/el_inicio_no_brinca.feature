Feature: El Inicio no brinca mientras carga

  Context:
  - Problem: abrir el app son tres segundos de empujones. El resumen de anoche no tiene sitio
    reservado —se pinta dentro de un `if (sleepAnalysis != null)` sin rama else—, así que mientras se
    lee de la base de datos no existe ni el hueco, y al llegar empuja hacia abajo los cuatro pilares,
    los campeones y el feed entero. Lo mismo con las publicaciones: entran de golpe donde no había
    más que una ruedita.
  - Savings: dejar de tocar lo que no era. Una lista que se mueve bajo el dedo hace pulsar la tarjeta
    de al lado, y obliga a esperar sin saber cuánto porque nada dice que venga algo.
  - Why: es lo primero que se ve del app. Una pantalla que se recoloca sola tres veces antes de
    quedarse quieta se lee como algo a medio hacer, por muy terminado que esté lo que enseña.

  Como alguien que abre el app
  Quiero que lo que va a aparecer tenga su sitio guardado desde el principio
  Para poder leer y tocar sin que la pantalla se mueva debajo

  Note: **los esqueletos que ya existen cuelgan de la espera equivocada.** Los tres —pilares,
  destacados y feed— se pintan cuando `HomeUiState` es `Loading`, y ese estado sale de
  `MockHomeRepository.getHomeContent()`, que no hace I/O ni espera: se resuelve en un frame. Así que
  no se ven nunca, y las dos esperas que sí duran —la base de datos del sueño y la red de las
  publicaciones— no tienen ninguno.

  Note: **`null` en el análisis de anoche significa una sola cosa.** `GetSleepAnalysisUseCase`
  devuelve un `SleepAnalysis` con ceros cuando no hay sesiones, nunca `null`; el nulo llega de
  aplanar `SleepUiState` a `SleepAnalysis?` al pasarlo por `MainScreen`. Por eso reservar el sitio es
  correcto: la tarjeta siempre acaba llegando. Lo que hay que dejar de perder por el camino es la
  diferencia entre «todavía no» y «no se pudo».

  # ─────── Slice 1 — el resumen de anoche reserva su sitio ───────

  @slice-1
  Scenario: El sitio del resumen existe antes que el resumen
    Given que abro el app con la lectura de anoche todavía en curso
    Then veo un hueco del tamaño de la tarjeta
    And los cuatro pilares ya están donde se van a quedar

  @slice-1
  Scenario: Cuando llega el resumen no empuja nada
    Given el hueco del resumen en su sitio
    When llega el análisis de anoche
    Then la tarjeta ocupa exactamente ese hueco
    And los pilares no se mueven

  Note: la tarjeta cambia de alto ella sola: la fila de horas, la línea de precisión y la fila de
  fases se pintan sólo si el dato las trae. Con el sitio reservado y la tarjeta creciendo, el brinco
  grande se cambiaría por uno pequeño — así que mide siempre lo mismo.

  @slice-1
  Scenario Outline: La tarjeta mide lo mismo tenga el dato que tenga
    Given un análisis <clase>
    When se pinta la tarjeta
    Then mide lo mismo que con cualquiera de los otros

    Examples:
      | clase                                              |
      | de una noche sin registrar: cero segmentos         |
      | con hora de dormirse y de despertar                |

  Note: **las otras dos variantes se sacaron de la tabla al probarlo en un teléfono.** Reservar sitio
  para la línea de precisión y para el desglose de fases dejaba la tarjeta con un tercio de aire
  debajo, y ninguna de las dos puede ocurrir hoy: la precisión sólo se pinta con
  `averageConfidence < 1` y `SleepReceiver` guarda `confidence = 1.0f` fijo; las fases sólo con
  LIGHT, DEEP o REM, que salen de unos `STATUS_` que el Sleep API de Android no devuelve — no reporta
  etapas de sueño. La prueba afirma igualmente lo que costarían, para que quien arregle ese mapeo se
  entere de que la tarjeta crece.

  Note: eso importa **hoy y no en abstracto**, porque `App.kt` relee el análisis a los 3 y a los 8
  segundos de abrir. Es a propósito: el detector de sueño de Android escribe sus segmentos poco
  después de arrancar, y sin esas relecturas la tarjeta abriría vacía. Lo que no puede es que
  rellenarse mueva la pantalla.

  @slice-1
  Scenario: Las relecturas del detector no mueven la pantalla
    Given la tarjeta pintada con lo que había al abrir
    When a los 3 segundos el detector ya ha escrito sus segmentos
    Then la tarjeta se rellena por dentro
    And nada de lo que hay debajo cambia de sitio

  @slice-1
  Scenario: Si no se pudo leer anoche, el hueco no se queda para siempre
    Given que la lectura de anoche falló
    Then no hay tarjeta ni hueco latiendo
    And lo que va debajo ocupa ese espacio

  # ─────── Slice 2 — las publicaciones enseñan su hueco ───────

  Note: el esqueleto del feed dibuja dos tarjetas de ancho completo de 320 dp. El feed va en **dos
  columnas** desde la segunda pasada del slice 1 de `publicaciones_en_el_home.feature`. Así que
  además de colgar de la espera equivocada, tiene la forma equivocada.

  @slice-2
  Scenario: El feed enseña su hueco mientras llega su primera página
    Given el Inicio recién abierto con las publicaciones en camino
    Then veo huecos con la forma que van a tener las publicaciones
    And no una ruedita al pie de una lista vacía

  @slice-2
  Scenario: El hueco va de dos en dos, como el feed
    Given los huecos del feed
    Then van dos por fila
    And cuando llegan las publicaciones ocupan ese mismo sitio

  @slice-2
  Scenario: El hueco es de la espera del feed y no de la del Inicio
    Given el contenido del Inicio ya cargado
    When las publicaciones todavía no han llegado
    Then el hueco del feed sigue puesto

  @slice-2
  Scenario: Los resultados de la búsqueda enseñan el mismo hueco
    Given que escribo «miel» en la barra de arriba
    When el sitio todavía no ha contestado
    Then veo los mismos huecos de dos por fila

  @slice-2
  Scenario: El hueco no tapa lo que ya se había leído
    Given tres páginas de publicaciones ya cargadas
    When pido la siguiente
    Then sigo viendo las tres
    And lo que dice que viene más es la ruedita del pie, no un hueco encima

  # ─────────────────── Lo que esta spec no cubre ───────────────────

  Note: **las pestañas de pilar ya están bien** y no entran. `PillarCatalogScreen` cuelga su
  `pillarCatalogSkeleton` de `PillarCatalogUiState.Loading`, que sí es la espera de la red. Se miró
  antes de escribir esto.

  Note: tampoco entra el temporizador de `App.kt`. Está ahí por el detector de Android y se queda;
  lo que este trabajo hace es que su efecto no se note en la posición de las cosas.
