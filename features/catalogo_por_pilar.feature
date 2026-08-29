Feature: El catálogo de un pilar se lee como un tablero, no como una lista

  Context:
  - Problem: las cuatro pestañas de pilar enseñan el catálogo como una rejilla plana con un buscador
    encima. Sueño e Inicio tienen un tablero —una tarjeta de resumen arriba y secciones con carrusel
    debajo— y las otras tres no se parecen a ellas, así que la app se lee como dos productos
    distintos según la pestaña. Y la rejilla plana esconde lo que caduca: un evento del sábado
    aparece entre productos, sin fecha visible y sin prioridad.
  - Savings: dejar de perder eventos por no verlos a tiempo, y dejar de mantener dos lenguajes
    visuales en la misma app.
  - Why: los cuatro pilares son el producto. Si tres de ellos parecen una pantalla de relleno, el
    pilar deja de leerse como una promesa y pasa a leerse como una sección sin terminar.

  Como alguien que abre un pilar para ver qué hay
  Quiero un tablero que me diga de un vistazo cuánto hay, qué ocurre pronto y qué tengo cerca
  Para no tener que recorrer una rejilla entera para encontrar lo que caduca

  Note: mismo layout que Sueño — LazyColumn, tarjeta de resumen con `LeafCard` sobre fondo oscuro,
  secciones con `SectionHeader` y carrusel `LazyRow`, separadas por `HazloSpaces.md`.

  Note: no se inventan componentes nuevos donde ya hay uno. Las tarjetas de los carruseles son
  `HazloProductCard`, la misma de la rejilla, con ancho fijo.

  @slice-3
  Scenario: El pilar se abre con un resumen de lo que hay
    Given un pilar con 12 publicaciones, de las que 3 son eventos y 2 son servicios
    When abro su pestaña
    Then arriba veo una tarjeta con el nombre del pilar y su icono
    And esa tarjeta dice 12 publicaciones, 3 eventos y 2 servicios

  @slice-3
  Scenario Outline: Una sección sin contenido no se pinta vacía
    Given un pilar cuyo catálogo <contenido>
    When abro su pestaña
    Then <resultado>

    Examples:
      | contenido                        | resultado                                    |
      | no tiene ningún evento           | no veo la sección "Próximos eventos"         |
      | no tiene ningún servicio         | no veo la sección "Servicios"                |
      | no trae distancias               | no veo la sección "Cerca de ti"              |
      | tiene un evento por venir        | veo la sección "Próximos eventos" con él     |

  @slice-3
  Scenario: Un evento que ya pasó no se anuncia como próximo
    Given un evento que empezó el 5 de septiembre a las 15:00
    And que hoy es 20 de septiembre
    When abro el pilar
    Then ese evento no aparece en "Próximos eventos"
    And sigue apareciendo en la rejilla de abajo, porque no deja de existir

  @slice-3
  Scenario: Los próximos eventos salen en el orden en que ocurren
    Given tres eventos que empiezan el 5, el 12 y el 8 de septiembre
    When abro el pilar
    Then en "Próximos eventos" salen en el orden 5, 8, 12

  @slice-3
  Scenario: Un evento enseña cuándo ocurre sin abrirlo
    Given un evento "Rodada del domingo" que empieza el sábado a las 15:00
    When lo veo en "Próximos eventos"
    Then su tarjeta muestra la fecha y la hora encima del título

  @slice-3
  Scenario: Un servicio enseña cuánto dura
    Given un servicio "Masaje deportivo" de 60 minutos a 450
    When lo veo en "Servicios"
    Then su tarjeta muestra "60 min" y su precio

  @slice-3
  Scenario: Lo más cercano se ofrece aparte
    Given que el teléfono conoce su ubicación
    And publicaciones a 1200, 340 y 5600 metros
    When abro el pilar
    Then en "Cerca de ti" salen en el orden 340, 1200, 5600

  @slice-3
  Scenario: Sin conexión el tablero avisa antes de enseñar nada
    Given un pilar que ya se había descargado
    When lo abro sin red
    Then el aviso de que es lo último descargado va encima de la tarjeta de resumen
    And el tablero se enseña completo debajo

  @slice-3
  Scenario: Un pilar nunca descargado no finge un tablero
    Given un pilar que nunca se descargó en este dispositivo
    When lo abro sin red
    Then no veo ni tarjeta de resumen ni secciones
    And se me dice que hace falta conexión la primera vez
