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

  @slice-2 @future
  Scenario: Buscar publicaciones por texto

  @slice-2 @future
  Scenario: La búsqueda respeta el filtro por pilar

  @slice-2 @future
  Scenario: Los resultados también se cargan al bajar

  # ─────────────────── Lo que esta spec no cubre ───────────────────

  Note: **el resto de `MockHomeRepository` se queda**, a propósito: los campeones inventados —Ana,
  Carlos, «42 hrs»— y las fotos de stock. Este slice quita **una** ficción, la del feed, y deja las
  otras dichas por escrito. Mezclarlas aquí haría el cambio irrevisable, y además esos textos viven
  en `data/` y no en un `@Composable`, que es como se escaparon de la regla de i18n: quitarlos es su
  propio encuadre.

  Note: tampoco entra abrir una publicación. Hoy el Home no lleva a ninguna parte al tocar una tarjeta
  y este slice no lo cambia; el sitio abre `/[slug]` y el app no tiene esa pantalla.
