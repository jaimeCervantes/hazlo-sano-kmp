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
