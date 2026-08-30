Feature: Una ruta guardada se puede ver antes de salir con ella

  Context:
  - Problem: importas un GPX y lo único que ves es su nombre y dos cifras en una lista. No hay forma
    de comprobar que el archivo es el que creías, ni de mirar por dónde va antes de salir. Un track
    equivocado se descubre en el monte, que es el peor sitio y el peor momento.
  - Savings: dejar de depender de otra app para mirar una ruta, y no gastar una salida entera
    siguiendo un archivo que no era.
  - Why: "recorrer rutas" es la promesa central del pilar. Sin poder verla, una ruta guardada es una
    fila en una lista; poder verla es lo que la convierte en algo que se pretende repetir.

  Como alguien que guarda rutas para repetirlas
  Quiero abrir una y ver su trazado en el mapa con sus cifras
  Para comprobar que es la que quiero antes de salir a seguirla

  Note: reutiliza `MovementMap` con `fitPathInView`, que ya existe y ya dibuja el recorrido de una
  sesión terminada. Una ruta y una sesión son cosas distintas, pero dibujarlas es lo mismo.

  Note: este slice no sigue la ruta ni avisa de desvíos — eso es C3. Aquí sólo se mira.

  @slice-c2
  Scenario: Abrir una ruta enseña su trazado
    Given una ruta "Cañón del Sumidero" guardada con 412 puntos
    When la abro desde la lista de rutas
    Then veo su trazado dibujado en el mapa
    And el mapa encuadra la ruta entera, no una esquina

  @slice-c2
  Scenario: La ruta enseña sus cifras junto al mapa
    Given una ruta de 8420 metros con 350 metros de desnivel y 412 puntos
    When la abro
    Then veo "8.42 km"
    And veo "350 m" de desnivel
    And veo que tiene 412 puntos

  @slice-c2
  Scenario Outline: Una ruta que no se puede dibujar lo dice en vez de enseñar un mapa vacío
    Given <situación>
    When abro esa ruta
    Then <resultado>

    Examples:
      | situación                              | resultado                                     |
      | una ruta que ya no existe              | se me dice que esa ruta ya no está            |
      | una ruta guardada sin ningún punto     | se me dice que no tiene trazado que enseñar   |

  @slice-c2
  Scenario: Volver deja la lista como estaba
    Given que abrí una ruta desde la lista
    When vuelvo atrás
    Then estoy otra vez en la lista de rutas

  @slice-c2
  Scenario: El desnivel de una ruta sin altitudes no se inventa
    Given una ruta importada de un GPX cuyos puntos no traían elevación
    When la abro
    Then el desnivel se muestra como desconocido
    And no se muestra como cero, que sería decir que es llana
