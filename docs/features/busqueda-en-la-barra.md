# La búsqueda en la barra de arriba

Spec: [`busqueda_en_la_barra.feature`](../../features/busqueda_en_la_barra.feature) · Antecedente:
[`publicaciones-en-el-home.md`](publicaciones-en-el-home.md)

## Encuadre

- **Problem:** el buscador de verdad está enterrado dentro de una pestaña. Hay que estar en Inicio y
  bajar más allá de los pilares, los campeones y el encabezado del feed para llegar a él — y lo que
  contesta no tiene nada que ver con dónde está: `/api/search` devuelve publicaciones de cualquier
  pilar y aparecen dentro del feed del Inicio. A la vez, **las cuatro pestañas de pilar tienen su
  propio campo**, con la misma forma redonda y el mismo componente (`HazloSearchField`), que hace
  otra cosa: acota localmente lo que esa pestaña ya cargó. Dos campos iguales, dos promesas
  distintas.
- **Savings:** una búsqueda en vez de dos que se parecen; el buscador alcanzable desde las cinco
  pestañas sin bajar nada; y los resultados dejan de fingir que son el feed del Inicio.
- **Why:** los cuatro pilares son la promesa, pero sólo encuentras una publicación si ya sabes en qué
  pilar está. La barra de arriba es el único sitio del app que no obliga a saberlo de antemano.

## Lo que ya existe, y por eso esto es más pequeño de lo que parece

| Pieza | Estado |
|---|---|
| Cliente de `/api/search`, mapeador, paginación por `total` | **Existe** — slice 2 de publicaciones |
| Espera al teclear (350 ms) y cancelación de la anterior | **Existe** — `HomeFeedViewModel` |
| Acotar la búsqueda por pilar | **Existe** — viaja en el parámetro `pillar` |
| Dos por fila en vertical | **Existe** — `homeFeed` reparte con `chunkExploreSectionItems`, `FEED_COLUMNS = 2` |
| Una capa encima del cuerpo, con vuelta atrás y sin tocar la pestaña | **Existe** — `AppLayer`, `topLayer`, `depth`, `BackHandler`; `SleepHistory` ya se comporta así |
| Barra superior compartida por las cinco pestañas | **Existe** — `HazloTopAppBar` en la rama `AppLayer.SleepHistory, null` de `MainScreen` |
| El campo dentro de la barra | **No existe** |
| Una pantalla de resultados | **No existe** |
| Un ViewModel de búsqueda separado del feed | **No existe** — hoy es el mismo, y el término decide a qué endpoint pregunta |

**La referencia es la barra del sitio hermano, y la app de Mercado Libre para la forma:** el campo
ocupa la fila entera de arriba y está siempre visible, desde cualquier sección.

## Decisiones tomadas en la puerta de alineación (2026-09-10)

1. **El campo sustituye al título.** Una sola fila (~56 dp, lo que la barra ya mide), el máximo de
   ancho para escribir, cero altura robada a las cinco pestañas. Se pierde «Hazlo Sano» del cuerpo
   principal; sigue en Ajustes, la ficha de un pilar y el pilar de Movimiento, que tienen su propia
   barra.
2. **El arreglo va primero.** Los cuatro fallos de `@slice-3` de `publicaciones_en_el_home.feature`
   viven en `HomeFeedViewModel`, la clase que este trabajo parte en dos. Mudar una búsqueda que se
   queda cargando sería estrenar el fallo en su sitio nuevo. Dos commits, no uno con dos «y».
3. **La búsqueda es global**, entre desde donde entre: buscar «miel» desde Sueño devuelve también lo
   de Nutrición. Acotarla a un pilar es una decisión que se toma **después**, con los chips de la
   pantalla de resultados.
4. **El Inicio conserva su feed y sus chips**, y pierde sólo el campo. Acotar el feed por pilar no es
   buscar: es la lista que se recorre sin preguntar nada.
5. **El filtro local de las pestañas de pilar se queda por ahora**, dicho por escrito como slice 2.
   Quitarlo en el mismo cambio haría esto irrevisable.

## Slices

### Slice 0 — el arreglo que ya está diagnosticado (prerrequisito)

No es un slice de este roadmap: es el **slice 3 de `publicaciones-en-el-home.md`**, con sus
escenarios ya escritos y su diagnóstico hecho. Se ejecuta primero, en su propio commit, y allí se
anota su bitácora. Sólo se menciona aquí para que el orden quede dicho.

### Slice 1 — el campo en la barra y los resultados en su pantalla

**Alcance.**

- `HazloTopAppBar` aprende a llevar un campo en lugar del título. Entra como parámetro opcional y por
  defecto no está, así que las ocho pantallas que la usan hoy no cambian. **Sigue sin leer recursos
  ni conocer el dominio**: el texto de ayuda entra por parámetro, como ya hace `HazloSearchField`.
- **`AppLayer.Search`**, una capa más en la lista que ya existe. Se pinta en la misma rama que
  `SleepHistory`: la barra de arriba se queda puesta, el cuerpo por debajo cambia, la barra de abajo
  no se pinta y `selectedTab` no se toca.
- **Una pantalla de resultados** en `feature/search/ui/`, con sus chips de pilar y la misma lista de
  dos columnas que ya usa el feed.
- **`HomeFeedViewModel` se parte en dos**: el feed del Inicio deja de saber qué es un término, y un
  ViewModel de búsqueda nuevo se queda con el término, el pilar y la paginación por `total`.
- **La lista de publicaciones se promueve, no se copia.** `homeFeed` vive hoy en
  `feature/home/ui/HomeFeed.kt`; en cuanto la usan dos pantallas le toca
  `core/ui/components/sections/`, con un estado común para las dos en vez de `HomeFeedUiState`. Es la
  regla de `AGENTS.md`: promover es mover, y un segundo componente casi idéntico es un fallo de
  diseño.
- Escribir la primera letra abre la pantalla en el acto; la petición sigue esperando los 350 ms.
- Volver —flecha, gesto o borrar el término— cierra los resultados, olvida el término y deja la
  pestaña donde estaba.

**Lo que NO entra:** abrir un resultado (el buscador no devuelve `to`; sigue siendo el slice 4 del
otro roadmap), las búsquedas recientes, y tocar el campo de las pestañas de pilar.

**Criterios de aceptación.**

- Se busca desde las cinco pestañas y desde ninguna de ellas cambia la pestaña elegida.
- Los resultados vienen de cualquier pilar, no del pilar desde el que se buscó.
- Los chips acotan los resultados sin borrar el término.
- Bajar trae más, contando sobre el `total` y no leyendo un campo.
- Volver de las tres formas devuelve a la misma pestaña y deja la barra vacía.
- Ajustes sigue alcanzable con los resultados abiertos, y volver de Ajustes devuelve a los
  resultados.
- El Inicio conserva feed y chips, y ya no tiene campo.
- En un teléfono en vertical los resultados van de dos en dos, y la última fila incompleta no
  ensancha su tarjeta.

**Cobertura, dicha de antemano.** Lo comprobable sin Compose UI test es casi todo lo que puede estar
mal: el ViewModel de búsqueda (término, pilar, páginas, `total`, red caída) en `commonTest`, y
`AppLayer` —el orden de capas y el pelado hacia atrás— ampliando `AppLayerTest`, que ya existe. Lo
que **no** mira ningún test es que la pestaña elegida sobreviva, porque `selectedTab` es una variable
local dentro de `MainScreen`; si sacarla a un tenedor de estado como `MovementNavState` —que sí tiene
prueba— sale barato, se saca y se prueba. Si no, se dice y se comprueba a mano.

### Slice 2 — el campo de las pestañas de pilar deja de competir

Con una búsqueda de verdad arriba, el campo que cada pestaña de pilar lleva dentro pasa a ser el
segundo control con la misma forma y otra promesa. Se decide entonces si se quita o si dice lo que
hace. Coarse a propósito: decidirlo hoy sería adivinar sin haber visto la barra funcionando.

### Slice 3 — lo que se buscó antes

Las últimas búsquedas al enfocar la barra, como hace Mercado Libre, y poder borrarlas. Coarse.

## Preguntas abiertas

1. **¿La marca vuelve a la barra en alguna forma?** Con el campo ocupando la fila, «Hazlo Sano» sale
   del cuerpo principal. Un logo pequeño a la izquierda cabría, pero hoy no existe ese recurso; se
   mira cuando haya.
2. **¿Los chips de resultados arrancan en «Todos» siempre?** Sí, por la decisión 3. Si al usarlo
   resulta que buscar desde Nutrición pide casi siempre Nutrición, es un cambio de una línea.

## Estado

| Slice | Estado |
|---|---|
| 0 — el arreglo de la búsqueda colgada (slice 3 del otro roadmap) | **Hecho** — ver su [bitácora](publicaciones-en-el-home-bitacora.md) |
| 1 — el campo en la barra y los resultados en su pantalla | **Hecho** — ver la [bitácora](busqueda-en-la-barra-bitacora.md) |
| 2 — el campo de las pestañas de pilar | Pendiente |
| 3 — búsquedas recientes | Pendiente |

**Pendiente de probar en un teléfono:** el alto de la barra con el campo dentro (`OutlinedTextField`
mide 56 dp de mínimo) y que buscar con red conteste bien desde las cinco pestañas.
