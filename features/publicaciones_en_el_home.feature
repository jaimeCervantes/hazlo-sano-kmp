Feature: El Home enseña lo que el proyecto publica

  Context:
  - Problem: lo primero que se ve al abrir el app es ficción. El Home sirve `MockHomeRepository`:
    «Tu Ecosistema», fotos de stock, campeones inventados llamados Ana y Carlos, y un feed de
    publicaciones que no existen. Mientras tanto el sitio hermano publica de verdad, y el app ya sabe
    hablar con él — pero sólo dentro de cada pestaña de pilar y sólo la primera página.
  - Savings: deja de haber una pantalla que hay que explicar, y el contenido que el proyecto ya
    produce llega al teléfono sin trabajo de backend: el endpoint existe, filtra por pilar y pagina.
  - Why: los cuatro pilares son la promesa del proyecto y las publicaciones son cómo se cuenta. Un
    app cuyo Home no las enseña es un tracker de bici con pestañas de adorno.

  Como alguien que abre el app sin venir a grabar nada
  Quiero ver lo que se publica, y quedarme sólo con el pilar que me interesa
  Para que el Home sea una razón para abrirlo y no una pantalla de paso

  Note: **la referencia es `src/app/(home)/PostsWithLoadMore.tsx` del sitio hermano**, que hace
  exactamente esto: la primera página viene del servidor, el filtro viaja en el parámetro `pillar`, y
  al llegar al final se piden más. Trae además un botón de «cargar más» junto al scroll, para quien no
  llega por scroll.

  Note: **casi todo lo que hace falta ya está.** `CatalogApi` llama a ese mismo endpoint,
  `CatalogRepositoryImpl` ya pagina y cachea con `hasMore` sacado de `nextPage`. Lo que falta es que
  alguien pida la página 2, un feed sin pilar —hoy `fetchPillarPage` exige uno— y que el Home deje de
  leer del repositorio de mentira.

  # ─────── Slice 1 — publicaciones de verdad, filtradas y sin fondo ───────

  @slice-1
  Scenario: El Home enseña lo que se publica
    Given que abro el app
    When miro el Home
    Then veo publicaciones que vienen del sitio
    And ninguna de ellas sale de un archivo del repositorio

  @slice-1
  Scenario Outline: Quedarse con un pilar
    Given el Home con publicaciones de los cuatro pilares
    When elijo el filtro <filtro>
    Then sólo veo publicaciones <de quién>

    Examples:
      | filtro      | de quién             |
      | Todos       | de cualquier pilar   |
      | Sueño       | de Sueño             |
      | Nutrición   | de Nutrición         |
      | Movimiento  | de Movimiento        |
      | Mente       | de Mente y Espíritu  |

  @slice-1
  Scenario: Cambiar de filtro empieza de nuevo
    Given que llevo tres páginas cargadas de un pilar
    When elijo otro pilar
    Then el feed empieza por su primera página
    And no quedan mezcladas las publicaciones del filtro anterior

  @slice-1
  Scenario: Al llegar al final se cargan más, solas
    Given un feed con más publicaciones de las que caben
    When llego al final de lo cargado
    Then se piden más sin que yo pulse nada
    And las que llegan respetan el filtro que tengo puesto

  @slice-1
  Scenario: No se piden dos páginas a la vez
    Given una página pidiéndose
    When vuelvo a llegar al final mientras llega
    Then no se arranca una segunda petición

  @slice-1
  Scenario: Ni se pide una que no existe
    Given que ya se cargó la última página
    When sigo bajando
    Then no se pide nada más
    And se dice que no hay más publicaciones

  Note: el estado vacío importa **porque el filtro lo produce**: un pilar sin publicaciones todavía es
  el caso normal al principio, no un error. El sitio hermano lo trata así, con un mensaje por pilar.

  @slice-1
  Scenario: Un pilar sin publicaciones lo dice
    Given un pilar del que aún no se ha publicado nada
    When lo elijo
    Then se me dice que ahí no hay nada todavía
    And no parece que el app haya fallado

  @slice-1
  Scenario: Un fallo de red no borra lo que ya se leyó
    Given tres páginas cargadas
    When la siguiente falla
    Then sigo viendo las tres
    And puedo reintentar sin perderlas

  # ─────────── Slice 2 — buscar por texto ───────────

  Note: **el contrato de `/api/search` no es el del feed.** Devuelve `{results, total}` y **no**
  `nextPage`, así que saber si hay más es `página × tamaño < total` y no leer un campo. Traducir uno
  al otro sin mirar sería suponer.

  Note: **la forma de la respuesta se comprobó antes de escribir el cliente**, y no coincidía: el
  buscador no manda `title` sino la traducción del idioma pedido, ni la etiqueta de categoría, ni el
  enlace absoluto. Reutilizar el DTO del feed habría compilado y habría dado tarjetas sin título,
  porque el cliente ignora las claves que no conoce.

  Note: y la misma comprobación destapó que esa respuesta incluye **el correo de quien publicó**, sin
  pedir credenciales. Aquí no se declara ese campo, así que no entra al app; el aviso es para el
  proyecto hermano.

  @slice-2
  Scenario: Buscar publicaciones por texto
    Given el Inicio con su feed
    When escribo un término
    Then los resultados vienen del buscador del sitio
    And no de recortar en el teléfono lo que ya se había traído

  @slice-2
  Scenario: Escribir una palabra es una búsqueda y no una por letra
    Given que escribo una palabra del tirón
    Then se pide una sola búsqueda
    And lo que escribí aparece sin esperar a que llegue

  @slice-2
  Scenario: La búsqueda respeta el filtro por pilar
    Given una búsqueda en curso
    When elijo un pilar
    Then la búsqueda se acota a ese pilar
    And no se pierde el término que había escrito

  @slice-2
  Scenario: Los resultados también se cargan al bajar
    Given resultados de una búsqueda
    When llego al final
    Then se piden más con el mismo término
    And se sabe si quedan porque se cuentan sobre el total y no por un campo

  @slice-2
  Scenario: Borrar el término devuelve al feed
    Given una búsqueda en curso
    When borro lo escrito
    Then vuelvo a ver el feed desde su primera página

  @slice-2
  Scenario: Sin red una búsqueda no tiene respuesta
    Given que no hay conexión
    When busco algo
    Then se dice que no se pudo
    And no se contesta con lo guardado, que no sabe responder a un término

  # ─────── Slice 3 — la búsqueda se queda cargando ───────

  Note: **el debounce no era el problema.** Existe desde el slice 2 —350 ms desde la última tecla, con
  cancelación de la espera anterior— y funciona. Lo que falla es lo que pasa con `loading` cuando una
  petición se descarta por vieja.

  Note: **son tres fallos encadenados, y no hace falta mala suerte para verlos.** `loadMore` enciende
  `loading` antes de salir a la red y su guarda de frescura vuelve sin apagarlo; su primera línea es
  `if (loading) return`, así que ese `loading` colgado bloquea todo lo que venga después; y `search`
  empieza una lista nueva sin apagarlo tampoco. Se abre el Inicio, se escribe antes de que llegue la
  primera página, y ya no se carga nada más en toda la sesión.

  Note: hay un cuarto del mismo tipo: `loadMore` escribe al final `current.copy(...)` sobre la foto
  del estado que tomó **antes** de salir, así que pisa lo que haya cambiado mientras viajaba. Y la
  corrutina de red no la cancela nadie — sólo se cancela la espera del debounce.

  @slice-3
  Scenario: Escribir mientras el feed está cargando busca igual
    Given el Inicio pidiendo su primera página
    When escribo un término antes de que llegue
    Then se busca ese término
    And no me quedo esperando para siempre

  @slice-3
  Scenario Outline: Toda petición que termina apaga el indicador
    Given una petición en vuelo
    When <desenlace>
    Then el indicador de carga se apaga

    Examples:
      | desenlace                                        |
      | llega con resultados                             |
      | falla                                            |
      | queda obsoleta y termina la que la sustituye     |

  Note: la tercera fila se redactó al arreglarlo. Decía «llega tarde y se descarta por ser de otra»,
  que describía el mecanismo viejo: dejar llegar la respuesta abandonada para tirarla. Al cancelarla
  no llega, así que **el indicador pasa a ser de la petición que la sustituye** y se apaga cuando esa
  termina. La garantía que importaba —nadie se queda esperando para siempre— es la misma.

  @slice-3
  Scenario: Una respuesta descartada no bloquea las siguientes
    Given una respuesta que llegó tarde y se descartó
    When bajo hasta el final
    Then se pide la página siguiente
    And el reintento también funciona

  @slice-3
  Scenario: El segundo término gana al primero
    Given una búsqueda viajando
    When escribo otro término
    Then veo los resultados del segundo
    And los del primero no aparecen aunque lleguen después

  @slice-3
  Scenario: Lo que vuelve se escribe sobre lo que hay
    Given una petición que sale con un estado
    When vuelve y ese estado ya cambió
    Then no se pisa lo que cambió mientras viajaba

  Note: la prueba que este slice tiene que dejar puesta es la que no existía. Las diez del slice 1 y
  las seis del 2 usan peticiones que contestan al instante, así que **ninguna tiene una petición
  todavía en vuelo cuando ocurre lo siguiente** — que es exactamente la condición de los cuatro
  fallos. Hace falta un doble que se quede a medias y se suelte cuando el test quiera.

  # ─────── Slice 4 — lo que el buscador dejó a medias ───────

  @slice-4 @future
  Scenario: El mapeador se prueba contra una respuesta guardada del sitio

  @slice-4 @future
  Scenario: La categoría de un resultado se lee, en vez de enseñar su clave cruda

  @slice-4 @future
  Scenario: Un resultado lleva a la publicación en el sitio

  # ─────────────────── Lo que esta spec no cubre ───────────────────

  Note: **el resto de `MockHomeRepository` se queda**, a propósito: los campeones inventados —Ana,
  Carlos, «42 hrs»— y las fotos de stock. Este slice quita **una** ficción, la del feed, y deja las
  otras dichas por escrito. Mezclarlas aquí haría el cambio irrevisable, y además esos textos viven
  en `data/` y no en un `@Composable`, que es como se escaparon de la regla de i18n: quitarlos es su
  propio encuadre.

  Note: tampoco entra abrir una publicación. Hoy el Home no lleva a ninguna parte al tocar una tarjeta
  y este slice no lo cambia; el sitio abre `/[slug]` y el app no tiene esa pantalla.
