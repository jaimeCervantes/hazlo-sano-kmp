Feature: Las pantallas de Movimiento dejan de estorbar

  Context:
  - Problem: el pilar con más profundidad del app es el que más se usa, y sus pantallas tienen fallos
    que se notan en cada uso: abrir una ruta desde «Mis rutas» rompe el tema, importar un GPX parece
    que no hace nada, el botón «atrás» de Android no existe en el proyecto, y el tracker enseña dos
    cifras mientras la sesión ya terminada enseña ocho.
  - Savings: cada pregunta de campo que queda cuesta una salida en bici. Salir con el app estorbando
    gasta salidas en fricción en vez de en datos. Y son fallos que se arreglan una vez y se dejan de
    pagar en cada uso, en vez de esquivarse cada vez.
  - Why: «recorrer rutas» es la promesa central del pilar. Por dentro está entregada —el filtro, el
    aviso de desvío, las cifras que se callan cuando no las midieron—. Por fuera está a medias, y lo
    que se ve es lo único que el usuario puede juzgar.

  Como alguien que usa el pilar de Movimiento en la calle
  Quiero que las pantallas no me hagan dudar de si el app está funcionando
  Para gastar las salidas en medir y no en pelearme con la interfaz

  Note: este roadmap no sale de una revisión de escritorio. Sale de usar el app en una salida real y
  reportar dos cosas concretas —el header de la ruta y la carga del GPX—; los demás slices son lo que
  apareció al buscar la causa de esos dos.

  Note: **la barra superior no pinta su propio fondo.** Es un `Row` transparente que delega el color
  en quien la llame, y de sus ocho llamantes siete repiten a mano la misma línea de fondo.
  `RouteDetailScreen` es el que la olvidó, y por eso es la única pantalla cuyo header no respeta el
  tema. El slice 2 no arregla esa pantalla: arregla que una pantalla pueda nacer rota por olvido.

  # ─────── Slice 1 — importar se ve, y una ruta se maneja donde se mira ───────

  Note: **el parseo no era el problema, y se midió antes de maquetar** (`GpxImportBenchmark`). Un GPX
  de 20.000 puntos y 2,5 MB —una ruta de seis horas a un punto por segundo— se importa entero en
  378 ms en escritorio, repartidos en parsear 144, medir 9, guardar 184 y releer 41. Lo que se vive
  como lentitud es el selector de archivos de Android, que descarga el archivo si está en Drive antes
  de entregarlo.

  Note: de ese número sale una decisión: **no hay porcentaje**. Con 378 ms no hay trabajo largo que
  repartir, y una barra que va del 0 al 100 en un parpadeo es un adorno que finge medir — la misma
  clase de cifra falsa que B3 y B4 se pasaron cuatro slices quitando de este pilar. Indicador
  indeterminado, y **empieza al pedir el archivo**, para cubrir la espera del selector, que es la que
  de verdad se sufre.

  Note: borrar, descargar y renombrar ya existen, pero **sólo en las filas de la lista**. Quien abre
  una ruta para mirarla en el mapa tiene que volver atrás para hacer cualquier cosa con ella. Y
  borrar no pregunta: un toque en la papelera y la ruta se fue.

  @slice-1
  Scenario: Mientras se importa, la pantalla lo dice
    Given que estoy en Mis rutas
    When pido importar un archivo GPX
    Then la pantalla dice que está importando
    And lo dice desde que pido el archivo, no desde que empieza a leerse

  @slice-1
  Scenario Outline: Al terminar, deje de decirlo pase lo que pase
    Given una importación en curso
    When la importación <desenlace>
    Then la pantalla deja de decir que está importando

    Examples:
      | desenlace                         |
      | guarda la ruta                    |
      | falla porque el GPX no se lee     |
      | falla porque no trae ningún punto |
      | descubre que la ruta ya existe    |

  @slice-1
  Scenario: No se importan dos archivos a la vez
    Given una importación en curso
    When pido importar otro archivo
    Then no arranca una segunda importación

  @slice-1
  Scenario: Cancelar el selector no deja la pantalla esperando
    Given que pedí importar un archivo
    When cierro el selector sin elegir nada
    Then la pantalla deja de decir que está importando

  @slice-1
  Scenario: Una ruta se descarga desde donde se está mirando
    Given que abrí una ruta desde Mis rutas
    When pido descargarla
    Then se escribe su GPX
    And no he tenido que volver a la lista para conseguirlo

  @slice-1
  Scenario: Borrar pregunta antes
    Given una ruta guardada
    When pido borrarla
    Then se me pregunta si estoy seguro
    And la ruta sigue estando hasta que confirmo

  @slice-1
  Scenario: Cancelar el borrado no borra
    Given que pedí borrar una ruta
    When cancelo
    Then la ruta sigue guardada

  @slice-1
  Scenario: Borrar desde el detalle devuelve a la lista
    Given que abrí una ruta y pedí borrarla
    When confirmo
    Then vuelvo a Mis rutas
    And no me quedo mirando el detalle de una ruta que ya no existe

  # ─────────── Slice 2 — la barra pinta su propio fondo ───────────

  @slice-2
  Scenario Outline: El header respeta el tema en todas las pantallas del pilar
    Given el tema <tema>
    When abro <pantalla>
    Then la barra superior se ve con el fondo del tema
    And no con el color que el sistema pinte por debajo

    Examples: las pantallas del pilar, en los dos temas
      | pantalla                  | tema    |
      | el tracker                | claro   |
      | el tracker                | oscuro  |
      | Mis salidas               | claro   |
      | Mis salidas               | oscuro  |
      | Mis rutas                 | claro   |
      | Mis rutas                 | oscuro  |
      | el detalle de una ruta    | claro   |
      | el detalle de una ruta    | oscuro  |
      | el detalle de una salida  | claro   |
      | el detalle de una salida  | oscuro  |

  @slice-2
  Scenario: La barra no depende de que su pantalla se acuerde de pintar el fondo
    Given una pantalla que no pinta ningún fondo en su raíz
    When compone la barra superior
    Then la barra sigue teniendo su fondo
    And es el mismo que en las pantallas que sí lo pintan

  @slice-2
  Scenario: Ninguna pantalla existente cambia de aspecto
    Given las siete pantallas que hoy pintan el fondo en su raíz
    When la barra pasa a pintar el suyo
    Then se siguen viendo igual que antes

  # ─────────── Slice 3 — la barra sólo pinta lo que hace algo ───────────

  Note: la campana, el ⋮ y el icono de perfil tienen handlers vacíos por defecto. La pantalla
  principal conecta el ⋮ a Ajustes; **las otras siete pantallas no conectan ninguno**, así que se
  pintan, responden al toque con su ondita y no pasa nada. Un control que miente sobre lo que hace
  enseña a desconfiar de la interfaz entera — el mismo principio que el pilar aplica a las cifras.

  @slice-3
  Scenario: Un control que no hace nada no se pinta

  @slice-3
  Scenario: Ajustes se alcanza sin salir de donde estoy

  @slice-3
  Scenario: Volver de Ajustes devuelve a donde estaba

  # ─────────── Slice 4 — el botón «atrás» del sistema ───────────

  Note: no hay un solo `BackHandler` en `app/`. `MovementNavState` ya es una pila con `back()`; lo que
  falta es que el gesto de Android la use. Va después del slice 3 porque Ajustes entra en esa pila
  allí, y conectarlo antes obligaría a tocarlo dos veces.

  @slice-4
  Scenario: Atrás desde el detalle de una ruta devuelve a Mis rutas

  @slice-4
  Scenario: Atrás desde el fondo del pilar cierra el pilar

  @slice-4
  Scenario: Atrás con una grabación en curso no la descarta sin avisar

  @slice-4
  Scenario: El gesto y el botón de la barra recorren la misma historia

  # ─────── Slice 5 — moverse por el pilar se hace siempre igual ───────

  Note: la regla que homogeneiza, y de la que sale todo lo demás: **abajo se va a sitios, en el ⋮ se
  hacen cosas**. Hoy están mezclados — el tracker tiene un «Mis salidas» delineado al lado del botón
  de Iniciar, «Mis salidas» tiene una puerta a «Mis rutas» flotando dentro del contenido, y «Mis
  rutas» no ofrece ninguna de las otras dos.

  Note: de las tres opciones que el usuario dio —barra superior, ⋮, o barra inferior— se elige la
  **barra inferior**, y las otras dos se descartan por lo mismo: esconder los destinos hermanos en un
  menú es lo que hacía difícil moverse por el pilar.

  Note: **un nombre cambia respecto a lo pedido.** El destino del tracker se pidió como «Iniciar»,
  pero en esa pantalla el botón grande ya dice «Iniciar» y hace otra cosa. Dos «Iniciar» distintos en
  la misma pantalla serían justo la confusión que este slice existe para quitar, así que el destino
  se llama «Grabar» y la acción sigue siendo «Iniciar» y «Detener».

  @slice-5
  Scenario Outline: Los tres lugares del pilar enseñan la misma barra
    Given que estoy en <pantalla>
    When miro abajo
    Then veo los tres lugares del pilar
    And el que veo marcado es <pantalla>

    Examples:
      | pantalla    |
      | Grabar      |
      | Mis rutas   |
      | Mis salidas |

  @slice-5
  Scenario Outline: Un detalle no es un lugar, y no lleva barra
    Given que abrí <detalle>
    When miro abajo
    Then no hay barra de lugares
    And se sale de aquí volviendo atrás

    Examples:
      | detalle                  |
      | el detalle de una ruta   |
      | el detalle de una salida |

  @slice-5
  Scenario: Ninguna navegación entre lugares queda dentro del contenido
    Given cualquiera de los tres lugares del pilar
    When miro lo que hay entre la barra de arriba y la de abajo
    Then no encuentro botones que lleven a otro lugar del pilar

  @slice-5
  Scenario: Desde una ruta se sale a seguirla
    Given que abrí una ruta desde Mis rutas
    When pulso Iniciar
    Then empiezo una salida con esa ruta ya cargada
    And no he tenido que buscarla en la lista del tracker

  @slice-5
  Scenario: Y volver devuelve a la ruta
    Given que empecé una salida desde el detalle de una ruta
    When vuelvo atrás
    Then estoy otra vez en el detalle de esa ruta
    And no en la lista ni en la pantalla principal

  Note: **una cosa se hace desde un solo sitio.** Con «Iniciar» en el detalle de una ruta, el selector
  que el tracker tenía pasó a ser una segunda puerta a una decisión ya tomada al llegar, así que se
  va. Se queda el renglón que dice con qué ruta sales: llegar siguiendo una y llegar sin ella son dos
  salidas distintas, y sin él no se sabría cuál se empieza hasta ver el trazado.

  @slice-5
  Scenario: El tracker dice con qué ruta sales, pero no deja elegirla
    Given que llegué al tracker siguiendo una ruta
    Then veo cuál es
    And no hay con qué cambiarla ni quitarla desde aquí

  @slice-5
  Scenario: Volver al tracker sin ruta no arrastra la anterior
    Given que salí siguiendo una ruta
    When entro al tracker desde la barra de sitios
    Then no sigo ninguna ruta
    And no me avisa de desviarme de una que ya no llevo

  @slice-5
  Scenario: Guardar una salida como ruta es una acción, no contenido
    Given el detalle de una salida que puede guardarse como ruta
    When abro los tres puntos
    Then puedo guardarla como ruta desde ahí
    And la opción no está suelta dentro del contenido

  @slice-5
  Scenario: Los dos detalles se leen igual
    Given el detalle de una ruta y el de una salida
    When abro los tres puntos en cualquiera de los dos
    Then primero están las acciones sobre lo que estoy mirando
    And después de una raya, Ajustes

  # ─────────── Slice 6 — el tracker durante la salida ───────────

  Note: `SessionMetric` define ocho cifras —distancia, duración, tiempo en movimiento, ritmo, ascenso,
  descenso, altitud máxima y mínima— y el detalle de una salida terminada las enseña. Durante la
  salida se ven dos. Las otras seis ya están calculadas.

  Note: la regla heredada de B3 y B4 vale igual en marcha que al terminar: **una cifra que no se midió
  no se enseña**. El tracker no puede enseñar un ascenso que el detalle luego se niega a dar.

  Note: **el directo usa el mismo cálculo que el detalle**, no uno propio. Acumular metros y segundos
  según llegan sería más barato, pero un segundo cálculo es la manera de que el tracker y el detalle
  acaben diciendo cosas distintas de la misma salida. Recalcular entero son dos pasadas sobre unos
  cinco mil puntos en una salida de tres horas, una vez cada dos segundos.

  Note: **el desnivel y las altitudes no salen en el directo**, aunque ya se calculan. La salida de
  campo del 6 de septiembre enseñó por qué: la altitud se congeló 150 s mientras se pedaleaba, así que
  en un teléfono real esas cifras dicen «—» buena parte del tiempo. Un hueco permanente en una esquina
  del mapa no informa, ocupa. Siguen enteras en el detalle.

  @slice-6
  Scenario: El ritmo y el tiempo en movimiento se ven mientras voy
    Given una salida en curso que ya ha recorrido un trecho
    When miro las cifras sobre el mapa
    Then veo el ritmo y el tiempo en movimiento
    And además la distancia y el tiempo, como antes

  @slice-6
  Scenario: Una salida que acaba de empezar no inventa cifras
    Given una salida que acaba de arrancar
    When miro las cifras
    Then el ritmo y el tiempo en movimiento enseñan una raya
    And no un cero, que sería decir que no me he movido

  @slice-6
  Scenario Outline: Lo que no se midió se calla, también en marcha
    Given una salida en curso <caso>
    When miro las cifras
    Then el desnivel no se afirma

    Examples:
      | caso                                  |
      | cuyas lecturas no traen altitud       |
      | cuya altitud se quedó pegada          |

  @slice-6
  Scenario: Y con altitud de verdad, el desnivel sí sale
    Given una salida en curso cuya altitud cambia de verdad
    When miro las cifras
    Then el desnivel está medido

  @slice-6
  Scenario: Las cifras no tapan el trazado ni el aviso de desvío
    Given una salida en curso
    Then las cifras van arriba a la izquierda en dos filas de dos
    And el aviso de desvío abajo al centro, sin competir con ellas

  @slice-6
  Scenario: El bloque de cifras no baila con los valores
    Given una salida en curso
    When una cifra pasa de una raya a un número, o de «0:00» a «10:32»
    Then el bloque no cambia de ancho ni de alto

  # ─────────────────── Lo que esta spec no cubre ───────────────────

  Note: **la pantalla de Inicio queda fuera a propósito.** Sirve datos inventados —«Ana», «Carlos»,
  «42 hrs», fotos de stock— y esos textos además no se traducen, porque viven en `data` y no en un
  `@Composable`, que es como se escaparon de la regla de i18n. Es un problema real y probablemente el
  siguiente, pero no es una pantalla de Movimiento y mezclarlo haría este roadmap irrevisable.

  Note: también quedan fuera Nutrición y Mente sin herramienta propia (es producto, no pulido) y la
  pantalla de licencias de las fuentes, pendiente desde el slice 2 del sistema de diseño.

  Note: `RoutesScreen` sigue sin test de pantalla desde el slice 13. Los slices 1 y 2 la rozan; si el
  andamiaje queda puesto, la deuda se salda ahí y se dice en la bitácora.

  Note: **no se simplifican los puntos de una ruta importada**, aunque probablemente sobren. El coste
  medido de conservarlos es despreciable —el aviso de desvío tarda 0,39 ms por lectura con una ruta
  de 20.000 puntos, 0,7 s por hora de salida— así que hoy no duele nada. Que sobren es cierto (la
  señal cruda sólo es buena hasta ~21 m y un GPX a 1 Hz guarda diferencias de centímetros), pero
  tocar la geometría de lo guardado merece su propio encuadre y no cabe de propina aquí.
