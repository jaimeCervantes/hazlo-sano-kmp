# Bitácora — La búsqueda en la barra de arriba

Roadmap: [`busqueda-en-la-barra.md`](busqueda-en-la-barra.md) · Spec:
[`busqueda_en_la_barra.feature`](../../features/busqueda_en_la_barra.feature)

---

## Slice 1 — El campo en la barra y los resultados en su pantalla (2026-09-10)

**Lo que había.** El buscador de verdad estaba enterrado dentro del Inicio, debajo de los pilares,
los campeones y el encabezado del feed. Y contestaba con publicaciones de los cuatro pilares dentro
de una lista que decía ser el feed del Inicio.

### Lo que no hubo que inventar

`AppLayer` ya era una pila de una capa encima del cuerpo, con su `BackHandler` y su `depth`. Y
`SleepHistory` ya hacía exactamente lo pedido: ocupar el cuerpo entero, no pintar la barra de abajo,
y **no tocar `selectedTab`**. Los resultados son una capa más en esa lista, con dos consecuencias que
no costaron nada:

- **La pestaña sobrevive porque nadie la escribe**, no porque se restaure al volver. No hay código
  que guarde y reponga la pestaña: `selectedTab` está debajo y no se entera.
- **La barra de arriba no se mueve.** Los resultados se pintan en la misma rama que el cuerpo, así
  que la barra se queda quieta y lo de debajo cambia. Por eso el campo no salta al abrirse la
  pantalla: es el mismo campo.

### Abierta es «hay algo escrito»

No hay una bandera de «búsqueda abierta». La capa está abierta exactamente cuando el término no está
en blanco, y de ahí sale gratis que **las tres formas de volver sean una sola operación**: la flecha
de la barra, el gesto del sistema y la equis del campo llaman todas a `clear()`. Una segunda bandera
habría podido discrepar del término, y ése es justo el fallo que nadie encuentra hasta que lo tiene
delante.

### Partir el ViewModel destapó lo que de verdad compartían

`HomeFeedViewModel` servía al feed y al buscador decidiendo por el término a qué endpoint preguntar.
Al separarlos, lo único que las dos pantallas comparten no es una pantalla ni un estado: es
**acumular páginas sin pedir dos veces lo mismo**. Eso salió a `PublicationPager`.

Dejarlo duplicado habría significado dos sitios donde volver a arreglar los cuatro fallos que costó
entender una vez, la semana pasada. Lo que cambia entre una lista y otra —de dónde sale una página—
entra por `restart` como una función: el paginador no sabe si detrás hay un feed o un buscador.

Y con la mecánica se mudaron sus pruebas: las nueve de `HomeFeedInFlightTest` son ahora once en
`PublicationPagerTest`. **No se copiaron**: en el ViewModel ya no queda nada de eso que probar.

Una vez ahí, distinguir listas por **generación** en vez de comparando el filtro y el término dejó al
paginador sin saber qué separa una lista de otra, que es exactamente lo que no le toca saber.

### Tres promociones, todas movimientos

`AGENTS.md` dice que promover es mover y no copiar. Se cumplió tres veces:

| Qué | De dónde | A dónde |
|---|---|---|
| La lista de dos columnas | `feature/home/ui/HomeFeed.kt` | `core/ui/components/sections/PublicationList.kt` |
| Los chips de pilar | el mismo archivo | `core/ui/components/sections/PillarFilterChips.kt` |
| El catálogo de mentira de los tests | privado dentro de `HomeFeedViewModelTest` | `data/catalog/FakeCatalog.kt` |

`HomeFeed.kt` **ya no existe**. Y las dos columnas en vertical, que era una de las tres cosas
pedidas, no costaron nada: venían con la lista.

### «Vacío» no significa lo mismo en las dos pantallas

En el feed es «aquí todavía no hay nada publicado»; en la búsqueda es «no se encontró nada con ese
término». Antes eran dos ramas del mismo `when` distinguidas por si el término estaba en blanco —
ahora el término ya no está en ese estado. Así que el texto entra por parámetro: la lista no sabe
cuál de las dos es, y deducirlo de la forma del estado sería inventar una diferencia que quien llama
ya conoce.

### Una cadena que era literalmente otra

Al renombrar el catálogo apareció que `home_feed_retry` era **carácter por carácter** `action_retry`,
en los dos idiomas. Se borró. Las demás pasaron de `home_feed_*` a `publication_list_*` porque una
lista de resultados leyendo una cadena llamada «home feed» es un nombre que miente.

### Dos decisiones que van contra la referencia, dichas

1. **Los resultados tapan la barra de abajo.** Mercado Libre la deja puesta; aquí no. Dejar
   «Nutrición» encendida mientras se leen resultados de los cuatro pilares diría algo falso, y este
   app ya tiene precedente propio: el historial de sueño hace lo mismo.
2. **El campo no se pinta sobre el historial de sueño.** Allí la flecha de volver ya significa otra
   cosa, y dos vueltas atrás en la misma barra no se distinguen; además esa pantalla tiene título
   propio, que es lo que el campo sustituye.

Y una tercera, menor: **cerrar la búsqueda devuelve el chip a «Todos»**. La siguiente búsqueda no
hereda el pilar de la anterior, que sería una pregunta acotada por algo que ya no se ve.

### Archivos tocados

- **Presentación compartida:** `core/presentation/PublicationPager.kt` (nuevo, con
  `PublicationListUiState`).
- **Presentación:** `HomeFeedViewModel.kt` (pierde el término y la mecánica), su factoría,
  `feature/search/presentation/SearchViewModel.kt` y su factoría (nuevos).
- **UI compartida:** `PublicationList.kt` y `PillarFilterChips.kt` (promovidos), `HomeFeed.kt`
  (borrado), `HazloSearchField.kt` (huecos para la lupa y la equis), `HazloTopAppBar.kt` (hueco para
  el campo, en lugar del título).
- **UI:** `feature/search/ui/SearchResultsScreen.kt` (nueva), `MainScreen.kt` (la capa, la barra y el
  cuerpo), `HomeScreen.kt` (pierde el campo, conserva los chips).
- **Navegación:** `AppLayer.kt` (`Search` y su sitio en el orden).
- **Recursos:** cinco cadenas renombradas, tres nuevas, una borrada por duplicada — en los dos
  idiomas.
- **Tests:** `PublicationPagerTest` (11, mudadas), `SearchViewModelTest` (11, nuevas),
  `HomeFeedViewModelTest` (16 → 10), `AppLayerTest` (9 → 10), `FakeCatalog` (promovido).

### Comandos y resultados

- `.\gradlew.bat :app:shared:jvmTest` → **459 pruebas, 0 fallos** (venían 451).
- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Desviaciones

- **`backgroundScope` no sirvió para las pruebas del paginador.** Las once fallaron todas con la
  petición sin salir: el planificador no llegaba a mover ese scope. Se cambió por un
  `CoroutineScope(dispatcher)` propio, que es el mismo sitio que ocupa `viewModelScope` en las
  pruebas de los ViewModels — el mismo planificador, otro dueño. Hacía falta que no colgara del test
  de todos modos: casi ninguna de esas pruebas suelta lo que retuvo, y `runTest` habría esperado
  para siempre.
- **`assembleDebug` falló al correrlo junto a los otros tres targets** (`copySharedComposeResources
  ToAssets` contra `prepareComposeResourcesTaskForCommonMain`, dos tareas de Gradle sin dependencia
  declarada entre ellas tocando la misma carpeta). En solitario pasa. Es un problema del cableado de
  Compose Resources, no del código, y no se tocó aquí.

### Sin cobertura de host

- **Que la pestaña elegida sobreviva no lo mira ningún test.** `selectedTab` es una variable local
  dentro de `MainScreen`, y sacarla a un tenedor de estado propio para poder probarla habría sido un
  cambio de navegación más grande que el que este slice hace. Lo que sí está probado es el orden de
  las capas y el pelado hacia atrás (`AppLayerTest`, ahora con `Search` dentro), y que **nada del
  código de la búsqueda escribe `selectedTab`** es comprobable leyendo: no hay tal escritura.
- **Nada de las dos pantallas lo compone un test**: ni el campo en la barra, ni los chips, ni las dos
  columnas. Las etiquetas están puestas (`SearchTags`, `PublicationListTags`, `PillarFilterTags`).
- **Sin probar en un teléfono de verdad.** Dos cosas concretas que hay que mirar: que el campo dentro
  de la barra no la deje demasiado alta —`OutlinedTextField` mide 56 dp de mínimo— y que buscar con
  red devuelva lo esperado desde las cinco pestañas.

**Recap.** La búsqueda vive en la barra de arriba y sus resultados en su propia pantalla, alcanzable
desde las cinco pestañas y sin cambiar la elegida. Casi todo el trabajo lo hizo la estructura que ya
estaba: la capa existía, las dos columnas existían, y la pestaña sobrevive porque nadie la escribe.
Lo que sí hubo que hacer fue partir en dos un ViewModel que era dos cosas — y eso destapó que lo que
de verdad compartían no era el estado sino la mecánica de paginar, que ahora vive en un solo sitio
con sus once pruebas.

**Próximos pasos (opciones).** (1) Probarlo en el teléfono, mirando el alto de la barra; (2) el slice
2, decidir qué pasa con el campo que cada pestaña de pilar lleva dentro, ahora que se puede comparar
con el de arriba; (3) el slice 4 del otro roadmap, lo que el buscador dejó a medias — el mapeador sin
fixture, la categoría en crudo y los resultados sin enlace.
