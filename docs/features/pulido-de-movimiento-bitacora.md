# Bitácora — Pulido de las pantallas de Movimiento

Roadmap: [`pulido-de-movimiento.md`](pulido-de-movimiento.md) · Spec:
[`pulido_de_movimiento.feature`](../../features/pulido_de_movimiento.feature)

---

## Slice 1 — Importar se ve, y una ruta se maneja donde se mira (2026-09-06)

**Origen.** No es una revisión de escritorio: el usuario usó el app en la calle y reportó que la
carga de un GPX «tarda mucho» y que necesitaba borrar y descargar rutas para poder probar. Pidió
además que esto fuera antes que el pulido estético, y se reordenó el roadmap.

### Lo primero fue medir, y cambió la solución

El usuario pidió «un cargador o algo que indique el porcentaje». Antes de maquetar nada se midió
cuánto tarda de verdad la importación, con un arnés nuevo (`GpxImportBenchmark`) que replica la ruta
completa: parsear el XML, calcular distancia y desnivel, guardar en SQLite y volver a leer.

| puntos | tamaño | parsear | medir | guardar | releer | TOTAL |
|---|---|---|---|---|---|---|
| 2.000 | 256 KB | 36 ms | 3 ms | 51 ms | 17 ms | **107 ms** |
| 5.000 | 642 KB | 80 ms | 5 ms | 137 ms | 26 ms | **248 ms** |
| 10.000 | 1.284 KB | 108 ms | 6 ms | 187 ms | 27 ms | **328 ms** |
| 20.000 | 2.568 KB | 144 ms | 9 ms | 184 ms | 41 ms | **378 ms** |

**El parseo no era el problema.** Un GPX de veinte mil puntos —una ruta de seis horas a un punto por
segundo— se importa entero en 378 ms en escritorio. En un teléfono serán uno a tres segundos.

Lo que se vive como lentitud son dos cosas, y ninguna es el parser:

1. **El selector de archivos de Android.** El Storage Access Framework descarga el archivo si está en
   Drive antes de entregarlo. Está fuera del app, pero es indistinguible de que el app se colgara.
2. **Que no se decía nada.** Una espera sin explicar se siente mucho más larga que la misma espera
   contada.

**De ahí sale la decisión que el usuario no había pedido: no hay porcentaje.** Con 378 ms no hay
trabajo largo que repartir, y una barra que va del 0 al 100 en un parpadeo es un adorno que finge
medir — la misma clase de cifra inventada que B3 y B4 se pasaron cuatro slices quitando de las
distancias y los desniveles de este pilar. Se pone un indicador indeterminado.

**Y la medida decidió también dónde empieza.** Como la espera larga es la del selector y no la del
trabajo propio, el indicador se enciende **al pedir el archivo**, no al recibir los bytes. Encenderlo
al recibirlos habría explicado los 378 ms y dejado a oscuras justo los segundos que se sufren.

### La consecuencia que obligó a tocar la capa de plataforma

Si la espera empieza al pedir el archivo, hay que enterarse de cuándo **no** va a llegar ninguno: sin
eso, cerrar el selector sin elegir deja la pantalla diciendo «importando» para siempre, y el botón de
importar apagado con ella.

`rememberGpxPicker` gana un segundo callback, `onAbandoned`, que cubre los dos casos en que no viene
nada: cerrar el selector (Android lo entrega como una URI nula) y un archivo que no se puede leer.
Tres de los cuatro `actual` no hacen nada en su plataforma, así que el cambio salió barato.

### Lo que el usuario pedía y ya existía

Pidió «eliminar la ruta y botón de descarga». **Ya estaban**, en cada fila de «Mis rutas»: el lápiz
renombra, la flecha descarga, la papelera borra. Que no los encontrara es en sí un dato.

Lo que de verdad faltaba, y es lo que se hizo:

- **El detalle de una ruta no dejaba hacer nada.** Se abría la ruta, se miraba el trazado en el
  mapa —que es donde se decide si sirve— y había que volver a la lista para cualquier cosa. Ahora
  descargar y borrar están también ahí.
- **Borrar no preguntaba.** Un toque en la papelera y la ruta se iba. En una lista de filas altas con
  los iconos al final, eso convierte un dedo mal puesto en una ruta perdida — y una ruta importada de
  un GPX que ya no se tiene no se recupera. Ahora pregunta, **y dice qué ruta va a borrar**: el
  nombre es lo que permite confirmar sin volver a mirar.

### Decisiones y por qué

1. **La espera se apaga en un solo sitio.** `show()` es el único punto por el que pasan todos los
   desenlaces con algo que decir, así que apagar ahí evita que un desenlace se olvide — y el que más
   se olvida siempre es el del error.
2. **Un duplicado apaga la espera en vez de mantenerla.** Deja de esperarse a un archivo y pasa a
   esperarse a una persona; son dos cosas distintas y la segunda ya tiene su propio diálogo.
3. **El botón de importar se apaga mientras hay una importación.** Dos a la vez dejarían dos rutas de
   un archivo, o una carrera por cuál gana. Apagado dice además, sin texto, que ya hay una en marcha.
4. **Borrar desde el detalle vuelve a la lista.** Quedarse mirando el detalle de una ruta que se
   acaba de borrar no es una pantalla, es un hueco.
5. **Borrado y «no la encuentro» se dicen aparte.** El ViewModel anuncia `deleted` en vez de dejar
   que la pantalla lo deduzca de un detalle vacío: son dos situaciones distintas y merecen dos
   respuestas distintas.
6. **`DeleteRouteDialog` nace en su propio archivo**, porque lo usan la lista y el detalle. Estaba a
   punto de ser lo mismo escrito dos veces.

### La pregunta del usuario que este slice contesta pero no resuelve

Preguntó si hacen falta todos los puntos para guardar una ruta. Se midió también eso, porque el aviso
de desvío recorre todos los segmentos con **cada** lectura del GPS:

| puntos de la ruta | por lectura | por hora de salida |
|---|---|---|
| 500 | 0,055 ms | 0,1 s |
| 20.000 | 0,390 ms | 0,7 s |

**Caben de sobra.** Que *sobren* es otra cosa y probablemente sea cierto: la señal cruda sólo es
buena hasta ~21 m (medido en nueve trazas) y un GPX a 1 Hz guarda puntos que se diferencian en
centímetros. Simplificar a 2 m de tolerancia —diez veces por debajo del ruido medido— quitaría el
grueso de los puntos sin cambiar el dibujo ni el desvío. **No se hace**: no arregla nada que duela
hoy, y tocar la geometría de lo guardado merece su propio encuadre.

### Archivos tocados

- **Presentación:** `RoutesViewModel.kt` (`isImporting`, `importRequested`, `importAbandoned`),
  `RouteDetailViewModel.kt` (reescrito: descargar, borrar y `deleted`).
- **UI:** `RoutesScreen.kt` (indicador, botones apagados, borrado con pregunta),
  `RouteDetailScreen.kt` (fila de acciones, diálogo, vuelta al borrar),
  `DeleteRouteDialog.kt` (nuevo, compartido por las dos).
- **Plataforma:** `GpxFileAccess.kt` y sus cuatro `actual` (`onAbandoned`).
- **Recursos:** `routes_importing`, `routes_delete_title`, `routes_delete_body`,
  `routes_delete_confirm`, en los dos idiomas.
- **Arnés:** `GpxImportBenchmark.kt` (nuevo).
- **Tests:** `RoutesViewModelTest` (+6), `RouteDetailViewModelTest` (nuevo, 5).

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **383 pruebas, 0 fallos** (venían 371).
- `.\gradlew.bat :core:check` y `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check` →
  BUILD SUCCESSFUL los tres.
- `.\gradlew.bat :app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL. Se corrió a propósito:
  el `actual` de iOS cambió de firma y iOS no se puede construir entero en Windows, así que ésta es
  la comprobación que sí cabe hacer aquí.
- `.\gradlew.bat :app:shared:jvmTest --tests "*GpxImportBenchmark*"` → las dos tablas de arriba.

### Sin cobertura de host

- **El indicador en pantalla no lo mira ningún test**, sólo el estado que lo produce. Las etiquetas
  están puestas (`RoutesTags.IMPORTING`, `RoutesTags.IMPORT`, `RouteDetailTags.EXPORT/DELETE`,
  `DeleteRouteTags`), así que el andamiaje para el test de pantalla de `RoutesScreen` —deuda desde el
  slice 13— queda listo, pero la deuda **sigue abierta**.
- **El diálogo de borrado tampoco**: que aparezca, que cancele y que confirme se comprueba abriendo.
- **El `onAbandoned` de Android no se puede probar aquí**: hace falta el selector real del sistema.
  Lo que sí está probado es que el ViewModel apaga la espera cuando se lo dicen.

**Recap.** Importar un GPX ahora se ve, y se ve **desde que se pide el archivo**, que es donde estaba
la espera de verdad. La forma la decidió una medida y no una intuición: 378 ms para veinte mil puntos
significa que no hay porcentaje que dar, así que no se da uno inventado. Y una ruta ya se puede
descargar y borrar desde donde se está mirando, con una pregunta antes de borrar que dice el nombre
de lo que se va.

**Próximos pasos.** El slice 2: que la barra superior pinte su propio fondo, que es el fallo que el
usuario reportó primero —el header de la ruta que no respeta el tema— y que hoy puede repetirlo
cualquier pantalla nueva por olvido.

---

## Slice 1, segunda pasada — lo que el uso real corrigió (2026-09-06)

El usuario probó el APK y devolvió tres cosas. Una era un bug de verdad, y las otras dos mejoran el
diseño que había entregado.

### El bug: el indicador desaparecía al navegar

**Lo reportado:** «puse a cargar una ruta, luego entré al detalle de otra y el indicador dejó de
mostrarse». El usuario preguntó si serían demasiados puntos para una ruta de 14 km, o si hacía falta
una bandeja en segundo plano.

**No era ninguna de las dos.** 14 km a un punto por segundo son entre 2.800 y 10.000 puntos, o sea
107-328 ms medidos. La causa estaba en la navegación: `MainScreen` llamaba a
`rememberRoutesViewModel()` **dentro** de la rama del `when` de Rutas. Al abrir el detalle de una
ruta esa rama deja de componerse, el `remember` se olvida, y al volver nace un ViewModel nuevo con la
importación a cero.

Lo llamativo es que **la importación sí terminaba**: el trabajo vive en `viewModelScope` y, al
construirse con `remember` en vez de con `viewModel()`, nadie llama a `onCleared()`, así que la
corrutina seguía y guardaba la ruta. Lo que se perdía era quien la estaba mirando — el indicador y
el mensaje de «importada».

**Arreglo:** el ViewModel sube por encima del `when`, a la composición de `MainScreen`. Ahora
sobrevive a toda la navegación de dentro del app, que es lo que el caso pedía.

**Lo que no se hizo, y por qué:** la bandeja en segundo plano que el usuario proponía. Con 378 ms
para veinte mil puntos no hay una espera que justifique una superficie propia, y el único hueco
verdaderamente largo —que Android baje el GPX de Drive— ocurre con el selector del sistema en primer
plano, donde no se puede navegar por el app de todos modos.

### La ruta entra en la lista, y no en un aviso encima

**Lo pedido:** «debería agregarse a la lista y con un cargador en la card de la nueva ruta».

Tiene razón, y es mejor que lo que había. Lo que se espera al importar es **ver aparecer la ruta**,
no leer que algo está pasando. El banner se sustituye por una tarjeta con la forma de las demás, en
la cabecera de la lista, que es justo donde va a quedarse la ruta ya guardada: cuando termina, la
fila deja de girar y se llena, sin salto.

Eso obligó a que el estado supiera **qué** está entrando y no sólo que algo entra: `isImporting:
Boolean` pasa a `importing: ImportingRoute?`, con el nombre del archivo. Hay un hueco en el que hay
importación pero todavía no se sabe de qué —entre pedir el archivo y que el selector lo entregue— y
ahí la fila lo dice en vez de inventarse un nombre. **Se enseña el nombre del archivo, no el de la
ruta**: el de verdad sale de dentro del GPX y puede no parecerse, así que la tarjeta muestra lo que
se sabe y no una promesa de lo que va a salir.

El estado vacío tuvo que aprender a apartarse: la primera importación se pide desde ahí, y si no se
aparta la ruta que llega no tiene dónde aparecer.

### Las acciones del detalle se van a los tres puntos

**Lo pedido:** en la lista, los botones de editar, eliminar y descargar (ya estaban, y se quedan); en
el detalle, que las opciones vivan en el header, en el menú desplegable de los tres puntos.

Es mejor que la fila de acciones que había entregado, y por una razón que el usuario vio antes que
yo: el detalle es un mapa a pantalla completa con una hoja de cifras encima, y cualquier cosa que se
ponga entre medias le quita sitio a lo único que se ha venido a ver.

Para eso `HazloTopAppBar` gana un hueco opcional, `menuContent`, que convierte los tres puntos en un
menú de verdad. **La barra no sabe qué hay dentro**: recibe el contenido ya escrito, que es lo que le
permite seguir siendo atómica —sin dominio y sin recursos— como manda `AGENTS.md`. Quien no pasa el
hueco se queda con el callback suelto de siempre, así que ninguna pantalla existente cambia.

Cerrar el menú es cosa del menú y no de cada opción: si cada una tuviera que acordarse, la que se
olvidara lo dejaría abierto sobre la pantalla que acaba de cambiar.

**De paso, esto adelanta media razón de ser del slice 3**: en el detalle de una ruta los tres puntos
dejan de pintarse sin hacer nada. Sin ruta cargada no hay menú, así que tampoco ofrecen algo que no
puedan cumplir.

### Archivos tocados

- **Presentación:** `RoutesViewModel.kt` (`ImportingRoute`, `importing` en vez de `isImporting`).
- **UI:** `MainScreen.kt` (el ViewModel sube por encima del `when`), `RoutesScreen.kt` (la tarjeta
  que entra sustituye al banner; el estado vacío se aparta), `RouteDetailScreen.kt` (las acciones se
  van al menú), `HazloTopAppBar.kt` (`menuContent`).
- **Recursos:** `routes_importing_unnamed`, en los dos idiomas.
- **Tests:** `RoutesViewModelTest` (+1: la fila que entra toma el nombre del archivo, y no lo tiene
  antes de que el selector entregue nada).

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **384 pruebas, 0 fallos**.
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Sin cobertura de host

El bug del indicador **no lo habría cogido ningún test de los que hay ni de los que se podían
escribir a ese nivel**: no estaba en el ViewModel, que se comportaba bien, sino en dónde se
construía. Lo cogió usar el app. Un test de pantalla que navegue a un detalle y vuelva sí lo
cogería, y es otra razón para saldar la deuda del test de `RoutesScreen`.

Tampoco lo miran los tests: el menú de los tres puntos, la tarjeta que entra, ni el diálogo de
borrado. Las etiquetas están puestas.

**Recap.** La ruta que se importa ahora aparece en la lista desde el primer momento, con su cargador,
en el sitio donde se va a quedar. Las acciones del detalle viven en el menú de la barra, que deja de
estar pintado por gusto. Y el indicador ya no se pierde al navegar: el ViewModel estaba construido
dentro de una rama del `when` que la navegación desmonta — la importación terminaba, pero se quedaba
sin nadie mirándola.

---

## Slice 2 — La barra superior deja de depender de quien la llame (2026-09-07)

**El fallo reportado.** Al abrir una ruta desde «Mis rutas», el header no respetaba el tema. Fue lo
primero que el usuario dijo al usar el app.

**La causa no estaba donde se veía.** `RouteDetailScreen` era el único de los ocho llamantes de
`HazloTopAppBar` cuya raíz no pintaba fondo. Pero eso es el síntoma: la barra era un `Row`
**transparente** que delegaba su fondo en quien la compusiera, y las otras siete pantallas repetían a
mano la misma línea. Con ocho llamantes copiando la misma línea, que uno la olvidara era estadística
— y cada pantalla nueva traía una ocasión más.

Así que el slice no arregla esa pantalla: arregla que una pantalla **pueda** nacer rota por olvido.
La barra pinta lo suyo, y a partir de aquí ninguna puede repetir el fallo.

### Dónde se pinta, que no da igual

El fondo va **antes** del `windowInsetsPadding(statusBars)` en la cadena de modificadores. Puesto
después, el color llegaría hasta donde empieza el contenido y dejaría una franja del color del
sistema bajo el reloj y la batería — el mismo fallo, más estrecho.

### Las siete que ya lo pintaban se quedan como están

No se les quita la línea, y es deliberado: la pintaban para **su cuerpo**, no para la barra. Quitarla
dejaría el cuerpo transparente, que es cambiar un fallo por otro. La barra pinta encima el mismo
color, así que ninguna cambia de aspecto — y hay una prueba que lo dice.

`RouteDetailScreen`, la octava, gana la línea que le faltaba: con la ruta cargando o sin ruta que
encontrar no hay mapa que tape el cuerpo, y sin fondo se veía el color de la ventana.

### Las pruebas, y por qué son dos y no ocho

El criterio de aceptación que había escrito decía «un test compone cada pantalla de Movimiento». Al
escribirlo se vio que era contar llamantes en vez de cubrir el defecto: una vez la barra pinta lo
suyo, afirmar lo mismo cinco veces no añade nada, y no cubriría la sexta pantalla que alguien escriba
mañana. Se prueba el **componente**, que es donde vive el invariante, más la pantalla que tenía el
fallo.

**Las dos usan un fondo hostil (magenta), y ahí está el valor.** Sobre un padre del color del tema,
una barra transparente y una pintada se ven idénticas: la prueba pasaría con el fallo dentro.

**Las dos se verificaron quitando el arreglo**, y la segunda no pasó a la primera:

- La de la barra falló sin el fondo, como se esperaba.
- La de la pantalla **pasó igualmente sin su arreglo**, porque muestreaba el píxel de la esquina
  superior izquierda — que desde este mismo slice es la barra, no el cuerpo. Corregida a muestrear
  abajo del todo, falla sin el arreglo y pasa con él. Queda escrito en el propio test, porque es la
  clase de error que se vuelve a cometer.

Es la primera vez que este proyecto afirma un **color** en una prueba. `captureToImage()` más
`toPixelMap()` funcionan en `jvmTest` con `compose.uiTest` y `compose.desktop.currentOs`, que ya
estaban.

### Archivos tocados

- **UI:** `HazloTopAppBar.kt` (pinta su fondo; `HazloTopAppBarTags`), `RouteDetailScreen.kt` (fondo en
  la raíz; `RouteDetailTags.SCREEN`).
- **Tests:** `HazloTopAppBarBackgroundTest` (nuevo, 2), `RouteDetailScreenTest` (+1).

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **387 pruebas, 0 fallos** (venían 384).
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Sin cobertura de host

El tema **oscuro** no lo mira ninguna prueba: las dos corren sobre el esquema por defecto de
`MaterialTheme`, y lo que afirman es que el color pintado es el del tema **sea cual sea**, no que sea
uno concreto. Eso cubre el defecto —heredar el fondo de detrás— en cualquier tema, pero que el
oscuro se vea bien sigue siendo una comprobación de ojo.

**Recap.** El header de la ruta ya respeta el tema, y no porque se le haya puesto un parche a esa
pantalla: la barra pinta su propio fondo, así que el fallo deja de ser posible. Las siete pantallas
que se acordaban de pintarlo no cambian de aspecto. Y las dos pruebas nuevas se verificaron quitando
el arreglo, que es lo que separa una prueba de un adorno — una de ellas pasaba sin él y hubo que
corregirla.

**Próximos pasos.** El slice 3: que la barra sólo pinte controles que hacen algo. Los tres puntos ya
hacen algo en el detalle de una ruta desde la segunda pasada del slice 1; queda la campana, el icono
de perfil, y llevar Ajustes a las pantallas donde hoy no se alcanza.

---

## Slice 3 — La barra sólo pinta controles que hacen algo (2026-09-07)

**Lo que había.** `HazloTopAppBar` pintaba siempre tres iconos —perfil, campana y tres puntos— porque
sus acciones tenían `{}` como valor por defecto. La pantalla principal conectaba el ⋮ a Ajustes;
**las otras siete pantallas no conectaban ninguno**. Se pintaban, respondían al toque con su ondita y
no pasaba nada. Un control que miente sobre lo que hace enseña a desconfiar de la interfaz entera, y
es el mismo principio que este pilar aplica a sus cifras: si no lo sabes, no lo digas.

### El cambio es de tipo, no de condición

Las acciones pasan de `() -> Unit = {}` a `(() -> Unit)? = null`, y el control se pinta si la suya no
es nula. **Que sea el tipo el que lo diga es lo que hace la regla comprobable en vez de una
intención**: con la lambda vacía por defecto no hay forma de distinguir «no me dieron nada» de «me
dieron algo que no hace nada», así que no se puede afirmar. Nulable, no se puede pintar un botón sin
tener a quién llamar.

De paso desaparece el `if/else` que repetía el `IconButton` del perfil dos veces —una con
`leadingIcon` y otra con el muñeco— y queda un `leadingIcon ?: Icons.Filled.Person`.

### Qué desaparece, y qué se gana

- **El icono de perfil**, que no hacía nada en ninguna pantalla. Se retira hasta que haya perfil. En
  la pantalla principal eso deja el sitio de la izquierda vacío y el título se va al principio, que
  es como se lee cualquier barra sin navegación hacia atrás.
- **La campana**, por lo mismo: no hay avisos que dar todavía.
- **Los tres puntos** siguen en la pantalla principal, y **aparecen ahora en las cinco pantallas de
  Movimiento**, llevando a Ajustes.

### Ajustes se alcanza sin salir del pilar, y el orden importa

Antes sólo se llegaba por el ⋮ de la pantalla principal: quien estaba grabando una salida y quería
cambiar el tema o el idioma tenía que abandonar el pilar entero.

El arreglo tiene una parte que no se ve y sin la cual no funciona: **la comprobación de `showSettings`
sube por encima del `when` de movimiento**. Estaba debajo, así que el `when` devolvía la pantalla de
movimiento y Ajustes no llegaba a pintarse nunca por mucho que se pusiera el booleano. Con el orden
corregido, volver pone `showSettings` a false y se vuelve a caer en el `when`, que sigue teniendo su
destino — así que se vuelve a donde estabas y no a la pantalla principal, que era el otro criterio.

### Dónde vive «Ajustes», y por qué no en la barra

`AppSettingsMenuItem` es nuevo y vive en `core/ui/components/`, **no en `atomic/`**: lee el catálogo,
y un componente atómico tiene que poder componerse desde cualquier sitio —previews y tests incluidos—
donde no hay entorno de recursos. Por eso tampoco puede escribirlo la barra, que sí es atómica: la
barra recibe el hueco del menú ya escrito y no sabe qué hay dentro.

Y se define una vez en lugar de en cada pantalla: cinco copias de la misma opción son cinco sitios
donde la redacción se puede separar.

En el detalle de una ruta el menú junta las dos clases de opción que puede haber en una barra: **lo
que se le hace a lo que estás mirando** (descargar, borrar) y **lo del app** (Ajustes), con una raya
entre medias. Ajustes está también cuando no hay ruta que enseñar, porque no depende de que la ruta
exista — un detalle que no encontró nada tampoco puede dejarte sin salida.

### Las pruebas, y cuál es la que importa

Los cuatro tests que ya había de la barra **fallaron al hacer el cambio**, y por la razón correcta:
componían la barra sin darle nada que hacer y afirmaban que los iconos aparecían. Reescritos para
pasar una acción, más cuatro nuevos para el lado negativo.

**El lado negativo es el que cubre el defecto.** Probar sólo que un control con acción aparece
dejaría pasar exactamente lo que había. Se afirma con `assertDoesNotExist()` y no con
`assertIsNotDisplayed()`: no es que el control esté escondido, es que no llega a existir — y la
segunda aserción falla cuando el nodo no está, así que la primera es además la única que compila
diciendo la verdad.

### Limpieza que el cambio permite

`top_app_bar_profile` y `top_app_bar_notifications` se quedaron sin un solo uso, así que salen de los
dos catálogos junto con sus imports muertos. Dejar copia sin dueño en el catálogo cuesta una
traducción por cada idioma que se añada. Vuelven con el control el día que haya perfil; git las
tiene.

### Archivos tocados

- **UI:** `HazloTopAppBar.kt` (acciones nulables, etiquetas), `AppBarMenu.kt` (nuevo),
  `MainScreen.kt` (Ajustes por encima del `when`, la barra pierde perfil y campana, las cinco
  pantallas reciben la puerta), `TrackerScreen.kt`, `MovementHistoryScreen.kt`, `RoutesScreen.kt`,
  `SessionDetailScreen.kt`, `RouteDetailScreen.kt` (menú con Ajustes).
- **Recursos:** fuera `top_app_bar_profile` y `top_app_bar_notifications`, en los dos idiomas.
- **Tests:** `HazloTopAppBarTest` (4 reescritos, +4), `RouteDetailMenuTest` (nuevo, 4).

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **153 pruebas, 0 fallos**.
- `.\gradlew.bat :app:shared:jvmTest` → **395 pruebas, 0 fallos** (venían 387).
- `.\gradlew.bat :core:check`, `:app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check`,
  `:app:shared:compileIosMainKotlinMetadata` → BUILD SUCCESSFUL los cuatro.

### Sin cobertura de host

**Lo de «volver de Ajustes devuelve a donde estabas» no lo mira ningún test**, y es el criterio con
más riesgo de los tres: depende del orden de dos bloques dentro de `MainScreen`, que es justo la
clase de cosa que un refactor mueve sin darse cuenta. Un test que componga `MainScreen` entero lo
cogería, pero `MainScreen` construye ViewModels con repositorios reales y no se puede componer en la
JVM sin desmontarlo primero. Queda anotado, con nombre: **es la misma deuda que dejó pasar el bug del
indicador de importación**, que también era de dónde se construye algo y no de qué hace.

Tampoco se prueba que la pantalla principal se vea bien con el título al principio, ahora que no hay
icono a la izquierda.

**Recap.** Un control de la barra existe porque alguien le dio algo que hacer, y eso es ahora una
propiedad del tipo y no una intención: las acciones son nulables. El muñeco del perfil y la campana
—que no llevaban a ningún sitio en ninguna pantalla— se retiran hasta que haya adónde llevar. Y
Ajustes se alcanza desde las cinco pantallas de Movimiento, lo que exigió subir su comprobación por
encima del `when` que devolvía antes de llegar a ella.

**Próximos pasos.** El slice 4: el botón «atrás» de Android, que sigue sin existir en todo el
proyecto. Ajustes ya está en la pila, que era la razón de ponerlo después de éste.
