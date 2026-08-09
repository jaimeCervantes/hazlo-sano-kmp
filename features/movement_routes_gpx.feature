Feature: Rutas que se pueden seguir, importar y compartir

  Context:
  - Problem: el pilar de movimiento sabe grabar lo que pasó, pero no sabe qué ruta ibas a seguir.
    `RouteRepository`, `GpxParser` e `ImportRouteUseCase` llevan desde el principio en `core` sin
    ninguna implementación conectada: no hay tabla de rutas, el único `GpxParserImpl` vivía en
    `core/src/jvmMain` con un paquete que no coincidía con su propia carpeta —inalcanzable desde la
    app en todos los targets, JVM incluido— y `MovementSessionEntity.routeId` guardaba `null` en
    cada sesión. Importar un GPX y seguirlo nunca fue posible, y nada lo decía.
  - Savings: una ruta que ya caminaste o que te pasaron deja de ser un archivo que no se puede
    abrir. Y una salida que grabaste deja de morir en el historial: se guarda con nombre y sale en
    GPX, que es lo que entiende cualquier otro reloj, ciclocomputadora o teléfono.
  - Why: seguir una ruta es la mitad del pilar. Grabar sin poder repetir convierte cada salida en
    un dato suelto en lugar de en algo que se pueda volver a hacer.

  Como alguien que sale a caminar o pedalear rutas conocidas
  Quiero importar un GPX, guardar mis salidas como rutas con nombre y exportarlas
  Para poder repetir una ruta y llevarla a cualquier otro aparato que lea el formato

  Note: el lector de GPX entiende la parte del formato que es una ruta —el track, su nombre, y la
  posición, elevación y hora de cada punto— e ignora el resto en lugar de rechazarlo. Un archivo de
  un reloj o de Strava viene lleno de extensiones sobre las que esta app no tiene opinión y aun así
  tiene que abrirse.

  Scenario: Importar una ruta desde un archivo GPX
    Given un archivo GPX con un track y sus puntos
    When lo importo
    Then la ruta queda guardada con el nombre que traía el track
    And con su distancia y su desnivel medidos a partir de los puntos

  Scenario: Un punto sin elevación no se inventa una
    Given un archivo GPX cuyos puntos no traen elevación
    When lo importo
    Then los puntos quedan sin altitud
    And nada los coloca al nivel del mar

  Scenario: Reconocer una ruta que ya tengo
    Given una ruta ya guardada
    When importo el mismo track con otro nombre
    Then la app avisa de que ya existe en vez de guardar dos veces
    And puedo decidir si la reemplazo

  Scenario: Guardar una salida grabada como ruta con nombre
    Given una sesión que grabé
    When la guardo como ruta y le pongo nombre
    Then la ruta queda guardada con ese nombre
    And con los puntos de la salida y sus medidas

  Scenario: Una ruta necesita nombre
    Given una sesión que grabé
    When intento guardarla como ruta sin nombre
    Then no se guarda nada
    And se me dice que la ruta necesita un nombre

  Scenario: Exportar una ruta a GPX
    Given una ruta guardada
    When la exporto
    Then obtengo un archivo GPX válido con su nombre y sus puntos
    And al volver a leerlo dice exactamente lo mismo que decía

  Scenario: Renombrar una ruta guardada
    Given una ruta guardada
    When le cambio el nombre
    Then la ruta conserva su track
    And aparece con el nombre nuevo
