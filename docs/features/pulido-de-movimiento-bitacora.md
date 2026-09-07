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
