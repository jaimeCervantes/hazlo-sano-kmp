Feature: Los datos del app viven en el Postgres compartido

  Context:
  - Problem: el app no sabe quién eres. Debajo de eso, dos síntomas: el catálogo que enseña está
    hardcodeado en `SeedProducts.kt`, así que corregir un precio obliga a recompilar y publicar; y lo
    que graba —salidas, rutas, sueño— solo existe en el SQLite de un teléfono, donde se pierde al
    desinstalar y no se puede consultar desde fuera.
  - Savings: dejar de publicar una versión para cambiar un dato, dejar de perder el historial por un
    cambio de teléfono, y poder atar lo grabado a una persona sin inventar un segundo padrón de
    usuarios junto al que el sitio ya tiene.
  - Why: los cuatro pilares prometen ver progreso en el tiempo. Un progreso anónimo y atado a un
    aparato no es progreso: es un archivo temporal.

  Como alguien que usa el app en el campo y el sitio desde el navegador
  Quiero entrar con la misma cuenta y que lo que grabo quede a mi nombre
  Para que no dependa de este teléfono ni de que me acuerde de respaldarlo

  Note: el app graba sin cobertura, que es para lo que existe el pilar de movimiento. Postgres es el
  destino, nunca el almacenamiento durante una grabación. El almacenamiento local no se retira.

  Note: al app no se le entrega nunca la sesión del navegador. Recibe una credencial propia, con su
  propia caducidad, que se guarda hasheada en el servidor.

  # ─────────────────── Slice 1 — entrar con Google o Microsoft ───────────────────

  @slice-1
  Scenario Outline: Entrar con cualquiera de los proveedores del sitio
    Given que tengo cuenta en el sitio con "<proveedor>"
    When entro desde el app y completo el login en el navegador
    Then el app queda identificado como la misma persona que en el sitio
    And lo que grabe queda a mi nombre, no al de nadie más

    Examples:
      | proveedor          |
      | google             |
      | microsoft-entra-id |

  @slice-1
  Scenario: Entrar por primera vez desde el app deja la cuenta creada
    Given que nunca he entrado al sitio
    When entro desde el app con "google" y completo el login
    Then mi cuenta queda creada igual que si hubiera entrado por el navegador
    And al abrir el sitio después me reconoce sin pedirme nada más

  @slice-1
  Scenario: Lo que guarda el app no es la sesión del navegador
    Given que entré desde el app y desde el navegador con la misma cuenta
    When comparo lo que guarda cada uno
    Then son credenciales distintas
    And cada una tiene su propia caducidad

  @slice-1
  Scenario Outline: Un canje que no cumple las condiciones no entrega nada
    Given un login recién completado que dejó un código de un solo uso
    When se intenta canjear <caso>
    Then no se entrega ninguna credencial
    And el intento no deja la sesión a medias

    Examples: rechazados — por qué existe cada fila
      | caso                                     | razón                                                      |
      | ese mismo código por segunda vez         | interceptar el redirect no puede reproducirse              |
      | el código con un verificador que no toca | quien robó el redirect nunca vio el verificador            |
      | el código pasados 60 segundos            | una ventana corta deja poco margen a quien lo capture      |

  @slice-1
  Scenario: Un volcado de la base no entrega credenciales usables
    Given una sesión de app abierta
    When se mira lo que quedó guardado en el servidor
    Then no está la credencial que tiene el app
    And lo guardado no permite deducirla

  @slice-1
  Scenario: El app nunca recibe las credenciales de Google ni de Microsoft
    Given que entré desde el app con "google"
    When se mira todo lo que el app guardó
    Then no hay ninguna credencial del proveedor
    And solo está la credencial que emitió el sitio

  # ─────────────────── Slice 2 — la sesión se ve, se renueva y se retira ───────────────────

  @slice-2 @future
  Scenario: Renovar deja la credencial anterior inservible
    Given una sesión de app que lleva tiempo abierta
    When el app la renueva
    Then la credencial anterior deja de servir

  @slice-2 @future
  Scenario: Una credencial de refresco reutilizada cierra todo
    Given una sesión de app que ya renovó una vez
    When alguien presenta la credencial de refresco vieja
    Then se cierran todas mis sesiones de app
    And se me pide entrar de nuevo

  @slice-2 @future
  Scenario: Retirar la sesión del app no toca la del navegador
    Given que tengo sesión en el app y en el navegador
    When retiro la sesión del app
    Then el app deja de poder guardar
    And sigo dentro en el navegador

  # ─────────────────── Slice 3 — el catálogo se lee del web ───────────────────

  Note: el alcance creció respecto a lo escrito el 2026-08-16. Entonces se planificó "el catálogo de
  productos"; al auditar el sitio antes de construirlo resultó que el endpoint había ganado filtro
  por pilar y que `posts.kind` publica cuatro tipos, de los que solo dos garantizan precio. Se
  decidió traer los cuatro y repartirlos por pilar.

  Note: la ubicación viaja como la cookie `hs_location` que el sitio ya sabe leer, en formato
  `lat,lng,ts`. Es un acoplamiento declarado a algo interno del web; el fallo, si cambia, es suave
  —se pierde el orden por cercanía, no el catálogo—. El remedio anotado es un endpoint propio con
  `lat` y `lng` explícitos.

  @slice-3
  Scenario: El catálogo que se ve es el que está publicado
    Given que en el sitio está publicada "Suero natural" a 45
    When abro el catálogo del app con red
    Then veo "Suero natural" con precio 45
    And no veo ninguna publicación que solo exista dentro del código del app

  @slice-3
  Scenario Outline: Qué se ve cuando no hay red
    Given que el app <estado previo>
    When abro el catálogo sin red
    Then <resultado>

    Examples:
      | estado previo                     | resultado                              |
      | ya había leído ese pilar          | veo lo último que se leyó              |
      | nunca ha leído ese pilar          | se me dice que todavía no hay catálogo |
      | solo había leído un pilar distinto | se me dice que todavía no hay catálogo |

  @slice-3
  Scenario: Lo que se enseña sin red dice que puede estar desactualizado
    Given que abro un pilar que ya había leído
    When no hay red
    Then veo lo último que se descargó
    And se me avisa de que es lo último descargado y no lo publicado ahora

  @slice-3
  Scenario: Un precio corregido en el sitio llega sin publicar una versión
    Given que "Suero natural" figuraba a 45 la última vez que el app leyó
    When su precio pasa a 60 en el sitio
    And vuelvo a abrir el catálogo con red
    Then veo 60
    And no ha hecho falta instalar una versión nueva del app

  @slice-3
  Scenario Outline: Cada pilar enseña lo suyo
    Given que abro la pestaña "<pilar>"
    When hay red
    Then veo solo publicaciones de ese pilar

    Examples:
      | pilar       |
      | Sueño       |
      | Nutrición   |
      | Movimiento  |
      | Mente       |

  @slice-3
  Scenario Outline: Los cuatro tipos de publicación se enseñan sin inventarles precio
    Given una publicación de tipo "<tipo>" <precio en el sitio>
    When la veo en el catálogo
    Then <lo que se pinta>

    Examples:
      | tipo      | precio en el sitio | lo que se pinta               |
      | producto  | con precio         | su precio                     |
      | servicio  | con precio         | su precio                     |
      | evento    | sin precio         | que es gratis                 |
      | anuncio   | sin precio         | nada donde iría el precio     |

  @slice-3
  Scenario: Un tipo de publicación que este app no conoce no desaparece
    Given que el sitio publica un tipo que esta versión del app no conoce
    When abro el catálogo
    Then esa publicación se ve igual, con su título y su imagen
    And no se esconde por no saber decorarla

  @slice-3
  Scenario: Con ubicación el catálogo sale por cercanía
    Given que el teléfono conoce su última ubicación
    When abro un pilar con red
    Then las publicaciones salen de la más cercana a la más lejana
    And cada una dice a qué distancia está

  @slice-3
  Scenario: Sin ubicación el catálogo sigue saliendo
    Given que el aparato no sabe dónde está o no me pidió el permiso
    When abro un pilar con red
    Then veo el catálogo ordenado por fecha
    And no se me exige compartir mi ubicación para verlo

  @slice-3
  Scenario: Lo que el sitio retira deja de verse
    Given que un pilar tenía guardada una publicación que ya se retiró del sitio
    When vuelvo a abrir ese pilar con red
    Then esa publicación ya no aparece

  # ─────────────────── Slices siguientes (aún sin detallar) ───────────────────

  @slice-4 @future
  Scenario: Una noche registrada deja de depender de este teléfono
    Given una noche que el app registró
    When hay red
    Then esa noche queda guardada a mi nombre en el servidor
    And sigue viéndose en el app aunque el envío todavía no haya ocurrido

  @slice-4 @future
  Scenario: Un envío que falla no pierde ni duplica
    Given una noche registrada que no se pudo enviar
    When vuelve a haber red
    Then se envía una sola vez
    And no aparece dos veces en el servidor

  @slice-5 @future
  Scenario: Una salida sube cuando termina, no mientras ocurre
    Given una salida que estoy grabando
    When la termino y hay red
    Then el recorrido completo queda guardado a mi nombre
    And durante la grabación no hizo falta cobertura en ningún momento

  @slice-6 @future
  Scenario: Una ruta guardada deja de vivir solo en el teléfono
    Given una ruta que guardé en el app
    When hay red
    Then queda guardada a mi nombre en el servidor

  @slice-7 @future
  Scenario: Añadir Facebook no toca el app
    Given que el sitio añade "facebook" como proveedor
    When entro desde el app y elijo ese proveedor
    Then entro igual que con los demás
    And no hizo falta instalar una versión nueva del app
