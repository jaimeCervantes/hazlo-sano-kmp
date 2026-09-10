# Bitácora — Publicaciones en el Home

Roadmap: [`publicaciones-en-el-home.md`](publicaciones-en-el-home.md) · Spec:
[`publicaciones_en_el_home.feature`](../../features/publicaciones_en_el_home.feature)

---

## Slice 1 — El Home enseña publicaciones de verdad (2026-09-09)

**Lo que había.** Lo primero que se veía al abrir el app era ficción: `MockHomeRepository` servía un
feed de publicaciones inventadas. Mientras tanto el app ya sabía hablar con el sitio, pero sólo dentro
de cada pestaña de pilar y sólo la primera página.

### El hueco era más pequeño de lo que parecía

`CatalogApi` ya llamaba al endpoint correcto, `CatalogRepositoryImpl` ya paginaba con `hasMore` sacado
de `nextPage` y ya cacheaba. Faltaban tres cosas: **que alguien pidiera la página 2**, un feed **sin
pilar**, y que el Home dejara de leer del repositorio de mentira.

### Una operación, no dos

`getPillarCatalog(pillar)` pasa a `getCatalog(pillar: PillarType? = null)`, y `GetPillarCatalogUseCase`
a `GetCatalogUseCase`.

**El pilar es opcional aquí porque lo es allí:** el endpoint del sitio acepta `pillar` como un
parámetro que puede no ir, y sin él devuelve los cuatro. Tener dos operaciones —una con pilar, otra
sin— habría partido en dos lo que al otro lado es una sola, y dejado a cada implementación debiendo
la mitad que no le toca.

En la API, **sin pilar el parámetro no viaja**. Mandar `pillar=` vacío habría funcionado —
`parsePublicationPillar` trata la cadena vacía como ninguno— pero por accidente y no por contrato.

### Dos decisiones sobre la caché que no son obvias

1. **El feed sin filtro no escribe en la caché.** `replacePillar` borra lo de un pilar antes de
   escribir; sin pilar no hay nada acotado que sustituir, y hacerlo sobre todo dejaría al dispositivo
   con veinte publicaciones sueltas donde tenía el catálogo de los cuatro. El feed sin filtro **lee**
   de lo que las pestañas de pilar hayan guardado, y no escribe.
2. **Sin filtro, las publicaciones no afirman pilar.** El sitio no dice a cuál pertenece cada una: lo
   deriva de su categoría raíz con una taxonomía que este app no tiene. Deducirlo aquí habría sido
   una segunda copia de esa tabla, desincronizada desde el primer día. `HazloProduct.pillar` ya era
   nulable, así que la honestidad salió gratis.

### El scroll infinito, traducido

El sitio usa un `IntersectionObserver` sobre un div al final. El equivalente en Compose no es
preguntar por la posición del scroll: **basta con que la última publicación entre en composición**.
Que eso se dispare de más no importa —el ViewModel ignora la petición si ya hay una en vuelo— y es
justamente lo que obliga a que esa guarda exista.

Las dos guardas de `loadMore` (ya cargando, y no hay más) tienen su prueba cada una, porque sin ellas
un scroll decidido dispara media docena de peticiones y las publicaciones llegan repetidas.

### «Todos» es la ausencia de filtro

No hay un `PillarType.ALL` inventado: el filtro que no filtra es `null`, lo mismo que el sitio
entiende por no mandar el parámetro. Un quinto valor en el `enum` habría obligado a cada sitio que
recorre los pilares a acordarse de saltárselo.

Y cambiar de filtro **empieza de nuevo**: conservar lo cargado mezclaría dos filtros en una lista, y
lo que se ve dejaría de corresponder a lo que el chip dice. Elegir el que ya está puesto no hace
nada, que es distinto.

### Perder la red no borra lo leído

Quien lleva dos páginas bajadas y entra en un túnel se queda con **sus dos páginas y un aviso**, no
con una pantalla en blanco — que sería castigarle por haber llegado lejos. Tiene prueba, y el
reintento también.

### Un componente que salió de donde estaba encerrado

Traducir un `HazloProduct` a los parámetros de `HazloProductCard` —precio de evento, portada o
iniciales, distancia— eran veinticinco líneas escritas **dentro** de `HazloExploreProductsSection`,
donde sólo las alcanzaba esa rejilla. Al aparecer el segundo sitio que pinta publicaciones salieron a
`HazloPublicationCard`. Copiarlas habría sido el segundo componente casi idéntico que `AGENTS.md`
llama fallo de diseño, y con él dos sitios donde arreglar el mismo detalle.

### Lo que se quitó, y lo que deliberadamente no

Se quitó **una** ficción: `content.feedPosts`, las publicaciones inventadas. **Siguen ahí** los
campeones —Ana, Carlos, «42 hrs»— y el resto de `MockHomeRepository`.

No es descuido. El usuario pidió sustituir los campeones por «los practicantes más disciplinados», y
al buscarlo en el sitio hermano apareció que **el concepto existe** (`GardenContributor`, con
`sustainedWeeks`), que el sitio ya decidió por escrito no proclamar posiciones, y que **no hay
endpoint**: a `/habitos` llega desde un componente de servidor, sin pasar por HTTP. Queda anotado en
el roadmap como bloqueado. Inventarse esos datos mientras tanto habría sido sustituir una ficción por
otra.

### Archivos tocados

- **core:** `CatalogRepository.kt` (pilar opcional), `GetCatalogUseCase.kt` (era
  `GetPillarCatalogUseCase`).
- **Datos:** `CatalogApi.kt` (`fetchPage`), `CatalogRepositoryImpl.kt` (sin pilar: no escribe caché,
  cae a `getAll`/`count`).
- **Presentación:** `HomeFeedViewModel.kt` y su factoría (nuevos).
- **UI:** `HomeFeed.kt` (nuevo: chips y lista), `HomeScreen.kt` (el feed real sustituye al de
  mentira), `HazloPublicationCard.kt` (nuevo, extraído), `HazloExploreProductsSection.kt` (lo usa).
- **Recursos:** seis cadenas del feed, en los dos idiomas.
- **Tests:** `HomeFeedViewModelTest` (nuevo, 10); renombres en los del catálogo.

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **436 pruebas, 0 fallos** (venían 426).
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Sin cobertura de host

- **Nada de la pantalla lo mira un test**: ni que los chips se pinten, ni que el scroll dispare la
  carga, ni el aviso de desactualizado. Lo que se prueba es el ViewModel, que es donde vive lo que
  puede estar mal. Las etiquetas están puestas (`HomeFeedTags`).
- **Contra el sitio de verdad no se ha probado.** Los tests usan un catálogo de mentira; que el
  endpoint conteste lo esperado sin filtro se comprueba abriendo el app con red.
- **El tamaño de página son 20**, heredado de `CatalogRepository.DEFAULT_PAGE_SIZE`. Si en el
  teléfono eso resulta ser tres pantallas por petición, el número está en un solo sitio.

**Recap.** El Home enseña por fin lo que el proyecto publica, con los cuatro pilares como filtro y más
publicaciones al llegar al final. Casi todo el trabajo ya estaba hecho: lo que faltaba era que alguien
pidiera la segunda página y que el pilar pudiera no ir — que es como el sitio lo entiende desde
siempre. Y de paso, la traducción de una publicación a una tarjeta sale de donde estaba encerrada.

**Próximos pasos (opciones).** (1) Probarlo con red en el teléfono; (2) el slice 2, buscar por texto
—`/api/search` existe—; (3) el jardín, que empieza con un endpoint en el proyecto hermano.

---

## Slice 1, segunda pasada — dos columnas y un campo para acotar (2026-09-09)

Probado en el teléfono. Dos cosas: el feed pedía la rejilla de dos columnas que usan las secciones de
los pilares, y faltaba el campo de búsqueda.

### Dos por fila, con la misma función que ya repartía

En un teléfono en vertical, una tarjeta a lo ancho por publicación convierte el feed en un pasillo.
Ahora son dos, **reusando `chunkExploreSectionItems`**, que es la que ya repartía las rejillas de los
pilares — copiar el reparto habría sido la segunda versión de una función de cuatro líneas cuyo único
detalle interesante es qué hacer con la última fila incompleta.

Se reparte en filas y **cada fila es un elemento de la lista perezosa**, en vez de meter una rejilla
perezosa dentro de la `LazyColumn` del Inicio: dos de esas en el mismo eje no se pueden medir. Así el
scroll infinito sigue funcionando —el disparo pasa a ser la última *fila*— y la rejilla no obliga a
componer lo que no se ve.

La última fila puede venir a medias, y su hueco se reserva con un `Spacer` de peso: sin él, la
tarjeta que sí está crecería al doble de las de arriba.

### El campo de búsqueda, y por qué **no** llama al buscador del sitio

Acota **lo que ya se trajo**, con `matchesHazloProductQuery` — la misma función que filtra las
rejillas de los pilares, que es donde el usuario señaló.

**Eso tiene un límite, y la redacción lo dice en vez de esconderlo.** El sitio tiene un buscador de
verdad (`/api/search`, con su embedding y su paginación), y esto no lo es: quien escriba algo que no
está entre las veinte publicaciones cargadas no verá resultados aunque el sitio los tenga. Por eso:

- el marcador dice **«Filtrar lo cargado…»** y no «Buscar»;
- cuando no hay coincidencias, el mensaje es **«Nada de lo cargado coincide. Baja para traer más.»**,
  no «no hay resultados» — que sería afirmar algo que veinte publicaciones no pueden sostener.

**Por qué no se hizo contra `/api/search` ahora:** su respuesta es
`ISearchPostResultDTO = Post & {...}`, que pasa por un mapeador distinto del que produce el feed, y
**no se puede comprobar sin llamar a la API**. El cliente del app es tolerante a claves desconocidas,
así que unos nombres de campo distintos no fallarían: devolverían tarjetas en blanco. Enviar eso a
ciegas era peor que no enviarlo. Sigue siendo el slice 2, ahora con esa comprobación como primer
paso.

### Un componente más que sale de donde estaba encerrado

El campo de escribir era `private` dentro de `HazloExploreItemsSection`. Sale a
`HazloSearchField`, en `atomic/`, donde le corresponde: no lee el catálogo de recursos ni sabe qué es
una publicación — el texto de ayuda entra por parámetro. Es el segundo componente que este slice
libera, después de `HazloPublicationCard`.

### Archivos tocados

- **UI:** `HazloSearchField.kt` (nuevo, promovido), `HazloExploreItemsSection.kt` (lo usa),
  `HomeFeed.kt` (rejilla y filtro), `HomeScreen.kt` (el campo y su término).
- **Recursos:** `home_feed_search_hint` y `home_feed_no_matches`, en los dos idiomas.

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **436 pruebas, 0 fallos**.
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Sin cobertura de host

**Ni la rejilla ni el filtro los mira un test**, por lo de siempre: componer el Inicio entero necesita
sus ViewModels con la persistencia real. Lo que sí está probado por debajo es el reparto en filas
(`chunkExploreSectionItems`) y la coincidencia (`matchesHazloProductQuery`), que ya venían con sus
pruebas de cuando servían a los pilares — reusarlas fue también reusar su cobertura.

**Recap.** El feed va en dos columnas como las rejillas de los pilares, reusando su misma función de
reparto, y con cada fila como un elemento perezoso para no romper el scroll infinito. El campo de
búsqueda acota lo cargado y **lo dice**: se llama «Filtrar lo cargado» porque eso es lo que hace, y el
buscador de verdad del sitio sigue esperando a que alguien compruebe qué forma tiene su respuesta.

---

## Slice 2 — La búsqueda le pregunta al sitio (2026-09-10)

**Lo primero fue comprobar la forma de la respuesta**, que es lo que el slice anterior dejó dicho
como paso uno. Se llamó al endpoint público y **la cautela estaba justificada**:

| | feed (`/api/posts`) | búsqueda (`/api/search`) |
|---|---|---|
| título | `title`, ya en el idioma | **`translations.{locale}.title`** |
| contenido | `content` / `summary` | `translations.{locale}.content` |
| categoría | `categoryLabel` traducida | sólo `category`, la clave cruda (`cuidado_personal`) |
| enlace | `to` absoluto | **no viene** |
| vendedor | trae `id` | trae `handle`, no `id` |
| media, precio, tipo, distancia | ✓ | ✓ iguales |

**No hay `title` plano.** Reutilizar `CatalogPostDto` habría compilado y habría dado **tarjetas sin
título** —el campo que más importa— porque el cliente ignora las claves que no conoce: un `title`
que no existe no falla, se queda vacío. Ése era exactamente el fallo silencioso que hizo que esto no
se cableara a ciegas.

Así que hay `SearchDto` y `SearchMapper` aparte, y el mapeador **cae a otra traducción** si no está
la pedida: una publicación escrita sólo en inglés existe y se puede encontrar; un resultado sin
título no se puede ni leer ni tocar.

### Un hallazgo que no se buscaba: el endpoint devuelve correos

La respuesta de `/api/search` incluye, por cada resultado, un objeto `user` con **el correo de quien
publicó**, y responde **200 sin autenticación de ningún tipo** — se comprobó con una petición sin
cookies.

Aquí eso está contenido: `SearchPostDto` no declara ese campo, así que no entra al app, no se pinta y
no se guarda. **Pero el dato sale del servidor a cualquiera que llame**, y eso no se arregla desde
este repositorio. Queda dicho para el proyecto hermano.

### Dos endpoints, dos casos de uso

`searchCatalog` va aparte de `getCatalog` y no como un parámetro suyo, porque al otro lado son dos
contratos: uno pagina con `nextPage` y el otro con un `total` —así que «quedan más» es
`página × tamaño < total`, una cuenta y no un campo— y uno entrega el idioma aplanado y el otro no.
Esconder eso tras un parámetro opcional habría hecho que una de las dos mitades mintiera sobre lo que
sabe.

**Buscar no cae a lo guardado, y ésa es la otra diferencia.** La caché del app guarda publicaciones,
no búsquedas: no hay forma de saber si lo guardado contesta a «miel» sin volver a filtrar por texto
en el dispositivo, que sería una segunda implementación del buscador con reglas peores que las del
sitio. Sin red, una búsqueda no tiene respuesta y se dice.

### La espera al teclear

Escribir «miel» sin espera son cuatro búsquedas de las que tres sobran, **y llegan desordenadas**: la
de «mie» puede pisar a la de «miel». Se espera 350 ms desde la última tecla y se cancela la anterior.

El término se guarda **ya** —para que lo escrito aparezca sin retraso— y sólo la petición espera. La
pausa entra por constructor para que un test no tenga que esperar de verdad.

Y elegir un pilar **acota** la búsqueda en curso en vez de cancelarla, que es lo que hace el sitio con
su parámetro `pillar`: buscar «miel» en Nutrición es una pregunta, y perder el término al tocar un
chip obligaría a escribirlo otra vez.

### La redacción cambia porque el significado cambió

Con el filtrado local, el campo decía «Filtrar lo cargado…» y el vacío «Nada de lo cargado coincide».
Ahora la pregunta la contesta el buscador del sitio, así que se puede afirmar sobre el catálogo:
**«Buscar publicaciones…»** y **«No se encontró nada con ese término.»**

### Archivos tocados

- **core:** `CatalogRepository.kt` (`searchCatalog`), `SearchCatalogUseCase.kt` (nuevo).
- **Datos:** `SearchDto.kt` y `SearchMapper.kt` (nuevos), `CatalogApi.kt` (`search`),
  `CatalogRepositoryImpl.kt`.
- **Presentación:** `HomeFeedViewModel.kt` (término, espera, cancelación) y su factoría.
- **UI:** `HomeFeed.kt` (fuera el filtrado local), `HomeScreen.kt`.
- **Recursos:** dos cadenas reescritas, en los dos idiomas.
- **Tests:** `HomeFeedViewModelTest` (+6); `searchCatalog` en los dobles que ya había.

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **442 pruebas, 0 fallos** (venían 436).
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.
- Una llamada a mano al endpoint de búsqueda del sitio, con `q=miel&limit=2&page=1&locale=es` → 200,
  y de ahí salió la tabla de arriba.

### Sin cobertura de host

- **El mapeador no se prueba contra una respuesta real.** Los tests usan un catálogo de mentira; lo
  que se comprobó contra el sitio fue la **forma**, una vez, a mano. Una prueba con la respuesta
  guardada como fixture cerraría eso y no se hizo.
- **La categoría se pinta con su clave cruda** (`cuidado_personal`) porque la búsqueda no manda la
  etiqueta traducida. Se ve fea y es verdad; traducirla aquí sería copiar la taxonomía del sitio, que
  es lo mismo que ya se decidió no hacer con el pilar.
- Y sin enlace: el buscador no devuelve `to`, y componer una URL adivinando la ruta del sitio para
  que lleve a un 404 es peor que no ofrecer enlace.

**Recap.** La búsqueda del Inicio le pregunta al buscador del sitio, con su filtro por pilar y sus
páginas. Comprobar la forma antes de escribir el cliente evitó el fallo que se había previsto —
tarjetas sin título— y destapó otro que nadie buscaba: el endpoint reparte los correos de quien
publica, sin pedir credenciales.
