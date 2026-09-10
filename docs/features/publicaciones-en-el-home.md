# Publicaciones en el Home

## Encuadre

- **Problem:** lo primero que se ve al abrir el app es ficción. El Home sirve `MockHomeRepository`:
  «Tu Ecosistema», fotos de stock, campeones inventados llamados **Ana** y **Carlos**, y un feed de
  publicaciones que no existen. Mientras tanto el sitio hermano publica de verdad, y el app **ya sabe
  hablar con él** — sólo lo hace dentro de cada pestaña de pilar y sólo la primera página.
- **Savings:** deja de haber una pantalla que hay que explicar («eso es de mentira»), y el contenido
  que el proyecto ya produce llega al teléfono sin trabajo de backend: el endpoint existe, filtra por
  pilar y pagina.
- **Why:** los cuatro pilares son la promesa del proyecto y las publicaciones son cómo se cuenta.
  Un app cuyo Home no las enseña es un tracker de bici con pestañas de adorno.

## Lo que ya existe, y por eso esto es más pequeño de lo que parece

Se revisó el sitio hermano (`salud-justa/comida-justa`) y el app antes de escribir esto.

| Pieza | Estado |
|---|---|
| Endpoint del feed | **Existe.** `GET /api/posts/page/{n}/pageSize/{m}?locale=&pillar=` → `{posts, nextPage, prevPage, total}` |
| Endpoint de búsqueda por texto | **Existe.** `GET /api/search?q=&page=&limit=&pillar=&locale=` → `{results, total}` |
| Cliente del feed en el app | **Existe.** `CatalogApi.fetchPillarPage(...)`, contra ese mismo endpoint |
| Paginación en el repositorio | **Existe.** `CatalogRepositoryImpl` ya devuelve `hasMore` desde `nextPage`, y cachea |
| Pedir la página 2 | **No lo hace nadie.** `PillarCatalogViewModel` sólo carga la primera |
| Feed sin filtro (los cuatro pilares) | **No existe.** `fetchPillarPage` exige un pilar |
| Cliente de `/api/search` | **No existe** |
| El Home | Sirve `MockHomeRepository`, incluido su `feedPosts` de mentira |

**La referencia concreta es `src/app/(home)/PostsWithLoadMore.tsx`**, que hace exactamente lo pedido:
primera página del servidor, filtro por pilar en el parámetro `pillar`, y más publicaciones al llegar
al final — con un botón de «cargar más» además del scroll, para quien no llega por scroll.

## Una ambigüedad, resuelta cortando en vez de preguntando

«Búsqueda de publicaciones» admite dos lecturas: **recorrer** el feed (que es lo que hace el home del
sitio, y lo que describe el resto de la petición — filtro por pilar y scroll infinito) o **buscar por
texto** (que en el sitio es otra página, `/buscar`).

No se elige una: se ordenan. El **slice 1 es el feed**, que hace falta en las dos lecturas y es lo que
se describió con detalle. El **slice 2 es el buscador por texto**, que se hace o no se hace según lo
que el slice 1 deje ver. Nada del 1 se tira si el 2 ocurre.

## Slices

### Slice 1 — el Home enseña publicaciones de verdad, filtradas y sin fondo

**Alcance.**
- El feed del Home deja de venir de `MockHomeRepository` y viene del sitio.
- **Una fila de filtros por pilar**, más «Todos». Cambiar de filtro reinicia el feed.
- **Scroll infinito**: al acercarse al final se pide la página siguiente, con el filtro puesto.
- `CatalogApi` aprende a pedir **sin pilar**, que es el caso de «Todos» y hoy no existe.
- Un fallo de red no vacía lo que ya se leyó: se sigue viendo lo cargado y se puede reintentar.

**Lo que NO entra**, a propósito: los campeones inventados (Ana, Carlos) y el resto de
`MockHomeRepository`. Se llevan tocando desde el roadmap de pulido y merecen su propio encuadre;
mezclarlos aquí haría este slice irrevisable. **Este slice quita una ficción —el feed— y deja las
otras dichas.**

**Criterios de aceptación.**
- El Home enseña publicaciones que vienen del sitio, no de un archivo del repositorio.
- Con un pilar elegido, sólo se ven las suyas; con «Todos», todas.
- Al llegar al final se cargan más **sin pulsar nada**, y las nuevas respetan el filtro.
- Cambiar de pilar empieza de nuevo por la página 1 en vez de mezclar dos filtros.
- No se piden dos páginas a la vez, ni se pide una más allá de la última.
- Sin conexión se ve lo que haya cacheado y se dice que está desactualizado, como ya hace el catálogo.

### Slice 2 — buscar publicaciones por texto

**Alcance.** Un campo de búsqueda que llama a `/api/search` respetando el filtro por pilar, con la
misma paginación. Se decide **después** del slice 1, mirando si el feed filtrado ya resuelve lo que se
quería.

**Nota del contrato, que no es igual al del feed:** `/api/search` devuelve `{results, total}` y **no**
`nextPage`. Saber si hay más es `página × tamaño < total`, no leer un campo. Traducir uno al otro sin
mirar sería la clase de suposición que este repositorio no se permite.

**Criterios de aceptación.** Se detallan cuando el slice empiece; hasta entonces quedan gruesos a
propósito.

### Slice 3 — La búsqueda se queda cargando

**Reportado probando el app:** al escribir un término, la pantalla se queda cargando y no sale de
ahí. El usuario sospechó del *debounce*; **ya existe** (350 ms desde la última tecla, slice 2), así
que la causa es otra.

**Diagnóstico, leyendo el código.** Son **tres fallos que se encadenan**, todos en
`HomeFeedViewModel`, y el resultado no es una carrera: es determinista.

1. **`loadMore` deja `loading = true` para siempre cuando descarta una respuesta vieja.** Pone
   `loading = true` antes de lanzar la petición, y su guarda de frescura
   (`if (_state.value.query != query) return@launch`) sale **sin volver a apagarlo**. Ese `loading`
   colgado es literalmente el indicador que no se va.
2. **Y con él, bloquea todo lo que venga después.** La primera línea de `loadMore` es
   `if (current.loading) return`. Con `loading` colgado en `true`, **ninguna petición futura
   arranca**: ni la búsqueda, ni el scroll, ni el reintento.
3. **`search` no apaga `loading` al empezar de nuevo.** Hace `copy(query = ..., publications = ...)`
   y deja el `loading` de la petición anterior tal cual.

**La secuencia que lo reproduce, sin necesidad de mala suerte:**

1. Se abre el Inicio → `loadMore()` pide la página 1 del feed → `loading = true`.
2. Se escribe antes de que vuelva → `search("m")` cambia el término y **no toca `loading`**.
3. Pasan los 350 ms → `loadMore()` → `current.loading` es `true` → **`return`**. No se busca nada.
4. Vuelve la respuesta del feed → el término ya no coincide → **`return@launch`**, `loading` sigue
   en `true`.
5. A partir de aquí no se carga nada más, nunca. Indicador para siempre.

**Un cuarto fallo del mismo tipo, que conviene arreglar a la vez:** `loadMore` captura `current` al
principio y al final escribe `current.copy(...)`. Eso **devuelve un estado anterior a la petición**,
así que cualquier cambio ocurrido mientras viajaba —el término, el filtro— se pisa. Y
`searchJob.cancel()` cancela sólo la espera del *debounce*: la corrutina de red que `loadMore` lanza
aparte no la cancela nadie.

**Alcance.**
- `loading` se apaga **siempre** que la petición termina, sea con datos, con error o descartada.
- La petición en vuelo se cancela cuando cambia el término o el filtro, en vez de dejarla llegar para
  descartarla.
- El estado se escribe sobre el que hay al **volver**, no sobre la foto de antes de salir.

**Criterios de aceptación.**
- Escribir un término mientras el feed está cargando **busca igual**.
- Terminar una petición deja el indicador apagado, gane, pierda o llegue tarde.
- Después de una respuesta descartada se puede seguir cargando: el scroll y el reintento funcionan.
- Escribir un segundo término mientras el primero viaja da los resultados del segundo.
- Las guardas del slice 1 siguen valiendo: ni dos páginas a la vez, ni una más allá de la última.

**Y una prueba que este slice tiene que dejar puesta**, porque es la que no existía: el ViewModel se
ejercita hoy con peticiones que responden al instante. Ninguna de las diez pruebas del slice 1 ni de
las seis del 2 tiene una petición **todavía en vuelo** cuando ocurre lo siguiente, que es justo la
condición de los cuatro fallos. Hace falta un doble que pueda quedarse a medias y soltarse cuando el
test quiera.

### Slice 4 — Lo que el buscador dejó a medias

Tres cosas anotadas al cerrar el slice 2, ninguna urgente:

- **El mapeador no se prueba contra una respuesta real.** Lo que se comprobó fue la *forma*, una vez
  y a mano. Guardar esa respuesta como fixture y mapearla en un test cerraría el hueco por el que
  entró el fallo que sí se evitó — un campo que cambia de nombre en el sitio no rompería nada, se
  quedaría en blanco.
- **La categoría se pinta con su clave cruda** (`cuidado_personal`) porque la búsqueda no manda la
  etiqueta traducida. Es feo y es verdad. Traducirla en el app sería copiar la taxonomía del sitio,
  que es lo que ya se decidió no hacer con el pilar; la salida buena es que el endpoint la mande.
- **Los resultados no llevan enlace.** El buscador no devuelve `to`, y componer una URL adivinando la
  ruta del sitio para que lleve a un 404 es peor que no ofrecer enlace.

## El jardín: lo que sustituye a los campeones, y por qué no puede entrar todavía (2026-09-09)

El usuario pidió sustituir los campeones semanales por «los practicantes de los 4 pilares más
disciplinados, que llevan varios días o semanas haciendo las prácticas». Al ir a buscarlo al sitio
hermano aparecieron dos cosas.

**Una: el concepto existe, con nombre y con una decisión de producto detrás.** Vive en
`src/domain/habits/habitLeague.ts` y se llama **el jardín**. Su fila es `GardenContributor`: alias,
aportes de la semana, y `sustainedWeeks` — «en cuántas semanas distintas ha practicado. **No baja
nunca: un hueco no borra nada**». Eso es exactamente «lleva semanas haciendo las prácticas».

Y el sitio ya decidió, por escrito, no llamarlos campeones:

> **No lleva posición.** La tabla se ordena, y el orden se ve; lo que no existe es un puesto
> proclamado, una corona ni un premio. Un ganador semanal fabrica nueve perdedores por cada ganador
> —en salud, y casi siempre gana quien tiene la vida menos caótica—, y contradice lo que este
> producto ya afirma de sí mismo.

Sustituir los campeones es lo correcto, entonces, y el sustituto ya está diseñado: una tabla ordenada
**sin puesto proclamado**, ordenada por aportes y desempatada por semanas sostenidas y luego por
alias, para que no baile entre recargas.

**Dos, y esto bloquea: no hay endpoint.** El sitio expone cinco rutas —`auth`, `posts`, `search` y
dos de `storage`— y ninguna es el jardín. A la página `/habitos` llega desde un componente de
servidor que llama a `HabitLeagueUseCase` directamente, sin pasar por HTTP. El app no tiene por dónde
pedirlo.

Así que el jardín **no es un slice de este repositorio**: empieza con un endpoint en el proyecto
hermano y sigue aquí. Lo que **no** se va a hacer es inventarse los datos mientras tanto: sustituir a
Ana y Carlos por otra ficción sería exactamente lo que este roadmap existe para deshacer.

## Preguntas abiertas

1. **¿El filtro por pilar se guarda entre sesiones?** El sitio lo lleva en la URL, que el app no
   tiene. Lo más simple es que no se guarde y se abra en «Todos»; se decide en el slice 1 y se dice.
2. **¿Cuántas publicaciones por página?** El sitio usa su `PAGINATION_PAGE_SIZE`. En un teléfono, una
   página que llene tres pantallas es peor que dos que llenen una y media. Se mira al montarlo.

## Estado

| Slice | Estado |
|---|---|
| 1 — el Home enseña publicaciones de verdad | **Hecho** — ver la [bitácora](publicaciones-en-el-home-bitacora.md) |
| 2 — buscar por texto | **Hecho** — ver la [bitácora](publicaciones-en-el-home-bitacora.md) |
| 3 — la búsqueda se queda cargando | **Hecho** — ver la [bitácora](publicaciones-en-el-home-bitacora.md) |
| 4 — lo que el buscador dejó a medias | Pendiente |
| — la búsqueda se muda a la barra de arriba | En curso: [`busqueda-en-la-barra.md`](busqueda-en-la-barra.md) |
| — el jardín en lugar de los campeones | **Bloqueado**: necesita un endpoint en el proyecto hermano |
