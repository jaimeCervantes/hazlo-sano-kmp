# Pulido de las pantallas de Movimiento

## Encuadre

- **Problem:** el pilar con más profundidad del app es también el que más se usa, y sus pantallas
  tienen fallos que se notan en cada uso. Abrir una ruta desde «Mis rutas» rompe el tema; importar un
  GPX parece que no hace nada; el botón «atrás» de Android no existe en ninguna parte del proyecto; y
  el tracker enseña dos cifras mientras la sesión terminada enseña ocho.
- **Savings:** cada pregunta de campo que queda cuesta **una salida en bici**. Salir con el app
  estorbando gasta salidas en fricción en vez de en datos. Y son fallos que se arreglan una vez y se
  dejan de pagar en cada uso, en vez de esquivarse cada vez.
- **Why:** «recorrer rutas» es la promesa central del pilar. Por dentro está entregada —el filtro, el
  aviso de desvío, las cifras que se callan cuando no las midieron—. Por fuera está a medias, y lo
  que se ve es lo único que el usuario puede juzgar.

**Origen.** Este roadmap no sale de una revisión de escritorio: sale de que el usuario usó el app en
la calle y reportó dos cosas concretas (el header de la ruta y la carga del GPX). Los otros tres
slices son lo que apareció al ir a buscar la causa de esos dos.

## Lo que se encontró, con su causa

| Qué se ve | Dónde | Causa |
|---|---|---|
| Al abrir una ruta, el header no respeta el tema | `RouteDetailScreen.kt` | Es **el único de los ocho llamantes de la barra cuya raíz no pinta fondo**. Los otros siete repiten a mano la misma línea |
| …y por qué era cuestión de tiempo | `HazloTopAppBar.kt` | La barra es un `Row` **transparente**: delega su fondo en quien la llame. Con ocho llamantes copiando la misma línea, que uno la olvidara era estadística |
| La campana y el ⋮ no hacen nada | `HazloTopAppBar.kt` | `onNotificationsClick` y `onMenuClick` tienen `{}` por defecto. `MainScreen` conecta el ⋮; **las otras siete pantallas no conectan ninguno** |
| El perfil no hace nada en ningún sitio | `HazloTopAppBar.kt` | `onProfileClick` no lo pasa nadie |
| Importar un GPX no da señal | `RoutesViewModel.kt` | `RoutesUiState` no tiene ningún campo de «importando». El trabajo sí sale del hilo principal; simplemente no se cuenta |
| …y la espera larga no es la que se cree | selector de Android | El parseo mide **378 ms para 20.000 puntos**. Lo que tarda es el Storage Access Framework, que descarga el archivo si está en Drive — fuera del app, pero se vive como que el app se colgó |
| El detalle de una ruta no deja hacer nada | `RouteDetailScreen.kt` | Enseña mapa y cifras. Borrar, descargar y renombrar sólo existen en las filas de la lista |
| Borrar una ruta no pide confirmación | `RoutesScreen.kt` | Un toque en la papelera y la ruta desaparece. Probando, un dedo mal puesto cuesta una ruta |
| «Atrás» de Android no hace lo que debe | todo el proyecto | **Cero `BackHandler`** en `app/`. `MovementNavState` ya tiene la pila; nadie la conecta al gesto |
| El tracker enseña dos cifras | `TrackerScreen.kt` | `SessionMetric` define ocho (distancia, duración, tiempo en movimiento, ritmo, ascenso, descenso, altitud máx/mín) y el detalle las enseña. Durante la salida se ven distancia y tiempo |

## Slices

### Slice 1 — importar un GPX se ve, y una ruta se maneja desde donde se mira

**Orden pedido por el usuario**, que es quien está probando: necesita importar, mirar, descargar y
borrar rutas sin pelearse, y lo necesita antes que nada estético.

**Lo que ya se midió, antes de maquetar** (`GpxImportBenchmark`, arnés nuevo):

| puntos | tamaño | parsear | medir | guardar | releer | TOTAL |
|---|---|---|---|---|---|---|
| 2.000 | 256 KB | 36 ms | 3 ms | 51 ms | 17 ms | **107 ms** |
| 5.000 | 642 KB | 80 ms | 5 ms | 137 ms | 26 ms | **248 ms** |
| 10.000 | 1.284 KB | 108 ms | 6 ms | 187 ms | 27 ms | **328 ms** |
| 20.000 | 2.568 KB | 144 ms | 9 ms | 184 ms | 41 ms | **378 ms** |

**El parseo no es el problema.** 378 ms para una ruta de seis horas a un punto por segundo, en
escritorio; entre uno y tres segundos en un teléfono. Lo que se vive como lentitud es el **selector
de archivos de Android**, que descarga el archivo si está en Drive antes de entregarlo — fuera del
app, pero indistinguible de que el app se haya colgado.

**Decisión que sale del número: no hay porcentaje.** Con 378 ms no hay trabajo largo que repartir, y
una barra que va del 0 al 100 en un parpadeo es un adorno que finge medir. Se pone un indicador
indeterminado, y **empieza antes**: en cuanto se pide el archivo, para cubrir la espera del selector,
que es la que de verdad se sufre.

**Alcance.**
- La pantalla dice que está importando, desde que se pide el archivo hasta que la ruta está guardada.
- No se puede arrancar una segunda importación mientras hay una en curso.
- El detalle de una ruta gana las acciones que hoy sólo están en la lista: descargar y borrar.
- Borrar pide confirmación, en la lista y en el detalle.

**Criterios de aceptación.**
- Mientras se importa, la pantalla lo dice; al terminar —bien o mal— deja de decirlo.
- Un GPX que falla no deja el indicador encendido.
- Elegir un segundo archivo mientras el primero se importa no arranca dos importaciones.
- Desde el detalle de una ruta se puede descargarla y borrarla.
- Borrar pide confirmación y se puede cancelar; cancelar no borra nada.
- Borrar desde el detalle vuelve a la lista, porque lo que se estaba mirando ya no existe.

### Slice 2 — la barra superior deja de depender de quien la llame

**Alcance.** `HazloTopAppBar` pinta su propio fondo. Las siete pantallas que hoy repiten
`.background(MaterialTheme.colorScheme.background)` en su raíz dejan de necesitarlo *para la barra*
(lo conservan para su cuerpo, que es otra cosa). `RouteDetailScreen`, la octava, gana el fondo que le
falta.

**Por qué.** Es lo que el usuario reportó primero, y **cierra la clase de fallo entera** en vez de
ese caso: después de este slice, una pantalla nueva no puede nacer con el header roto por olvido.

**Criterios de aceptación.**
- Un test compone cada pantalla de Movimiento y afirma que la barra superior **tiene fondo propio**,
  sin depender de lo que pinte la pantalla.
- `RouteDetailScreen` se ve igual que las demás en tema claro y oscuro.
- Ninguna pantalla existente cambia de aspecto: el fondo que la barra pinta es el que ya tenía
  debajo.

### Slice 3 — la barra sólo pinta controles que hacen algo

**Alcance.** La campana y el ⋮ dejan de aparecer donde no hacen nada. El ⋮ pasa a llevar a Ajustes
**desde cualquier pantalla**, no sólo desde la principal — hoy, si estás en el tracker y quieres
cambiar el tema o el idioma, tienes que salir del pilar. El icono de perfil, que no hace nada en
ningún sitio, se retira hasta que haya perfil.

**Por qué.** Un control pintado que responde al toque y no hace nada enseña a desconfiar de la
interfaz entera. Es el mismo principio que el pilar aplica a las cifras: si no lo sabes, no lo digas.

**Criterios de aceptación.**
- Un test afirma que la barra no pinta un control sin acción.
- Ajustes se alcanza desde el tracker, «Mis salidas», «Mis rutas» y los dos detalles.
- Volver de Ajustes devuelve a donde estabas, no a la pantalla principal.

### Slice 4 — el botón «atrás» de Android

**Alcance.** El gesto de volver atrás recorre `MovementNavState`, que ya es una pila. Desde el fondo
del pilar, cierra el pilar; desde la pantalla principal, se comporta como siempre.

**Por qué aquí.** Ajustes entra en la pila en el slice 3, así que conectar el gesto antes obligaría a
tocarlo dos veces.

**Criterios de aceptación.**
- Atrás desde el detalle de una ruta devuelve a «Mis rutas», no a la pantalla principal.
- Atrás desde el tracker con una grabación en curso **no** descarta la grabación sin avisar.
- La pila del gesto y la del botón de la barra son la misma: no hay dos historias distintas.

### Slice 5 — moverse por el pilar se hace siempre igual

**Pedido por el usuario probando el app**, con tres quejas concretas: al entrar al tracker no se
alcanzan «Mis rutas» ni «Mis salidas»; en «Mis salidas» la puerta a «Mis rutas» está metida dentro
del contenido; y desde el detalle de una ruta no se puede salir a seguirla.

**La regla que homogeneiza, y de la que sale todo lo demás:**

> **Abajo se va a sitios. En el ⋮ se hacen cosas.**

Hoy están mezclados: el tracker tiene un «Mis salidas» delineado junto al botón de Iniciar —una
navegación al lado de una acción—, «Mis salidas» tiene un `TextButton` a «Mis rutas» flotando bajo la
barra, y «Mis rutas» no ofrece ninguna de las otras dos.

**Alcance.**
- Una **barra inferior del pilar** con sus tres lugares: Grabar, Mis rutas, Mis salidas. Va en las
  tres pantallas que *son* un lugar, idéntica en las tres.
- **No va en los dos detalles.** Un detalle es algo que abriste *desde* un lugar y del que se sale
  con atrás; darle destinos hermanos invita a perderse en vez de a volver.
- Las navegaciones que hoy viven dentro del contenido se van: el «Mis salidas» del tracker y el
  «Mis rutas» del historial.
- **El detalle de una ruta gana su acción principal:** un botón abajo que empieza una salida
  siguiendo esa ruta. Es el eslabón que faltaba entre «Mis rutas» y el tracker — hasta ahora había
  que ir al tracker y buscar la ruta en un diálogo.

**De las tres opciones que el usuario dio —barra superior, ⋮, o barra inferior— se elige la barra
inferior**, y las otras dos se descartan por lo mismo: esconder los destinos hermanos en un menú es
lo que hacía difícil moverse. El ⋮ se queda para las acciones, que es lo que ya lleva desde el
slice 3.

**Un nombre cambia respecto a lo pedido.** El usuario pidió que el destino del tracker se llamara
«Iniciar». En esa pantalla el botón grande ya dice «Iniciar» y hace otra cosa —empezar a grabar—, así
que dos «Iniciar» distintos en la misma pantalla serían justo la confusión que el encargo quiere
evitar. El destino se llama **«Grabar»**; la acción sigue siendo «Iniciar» y «Detener».

**Criterios de aceptación.**
- Las tres pantallas de lugar enseñan la misma barra, con el sitio donde estás marcado.
- Los dos detalles no la enseñan.
- Ninguna navegación entre lugares queda dentro del contenido.
- Desde el detalle de una ruta se empieza una salida siguiéndola, y el tracker abre ya con esa ruta
  cargada.
- Volver desde el tracker abierto así devuelve al detalle de la ruta, que es de donde se vino.

### Slice 6 — el tracker durante la salida

**Alcance.** Las cifras que ya se calculan y hoy sólo se ven al terminar, visibles mientras vas.
Cuáles y con qué jerarquía se decide con el sistema de diseño delante, no metiendo ocho números en
una esquina del mapa.

**Regla heredada, que aquí importa.** Una cifra que no se midió **no se enseña**: el desnivel se
calla cuando la altitud se quedó rancia, y eso vale igual en marcha que al terminar. El tracker no
puede enseñar un ascenso que el detalle luego se niega a dar.

**Criterios de aceptación.**
- El ritmo y el tiempo en movimiento se ven durante la salida.
- Una cifra sin medida enseña «—», no un cero.
- Las cifras no tapan el trazado ni el aviso de desvío.
- La barra de cifras no cambia de altura según lo que valgan los números.

## Lo que este roadmap deja fuera, a propósito

- **La pantalla de Inicio y sus datos inventados** (`MockHomeRepository`: «Ana», «Carlos», «42 hrs»,
  fotos de Unsplash, y textos que además no se traducen porque viven en `data` y no en un
  `@Composable`). Es un problema real y probablemente el siguiente, pero no es una pantalla de
  Movimiento y mezclarlo haría este roadmap irrevisable.
- **Nutrición y Mente sin herramienta propia.** Es un asunto de producto, no de pulido.
- **La pantalla de licencias** de las fuentes, pendiente desde el slice 2 del sistema de diseño.
  Cabría en el slice 2 de éste; se deja anotada para no ensancharlo.
- **El test de pantalla de `RoutesScreen`**, deuda desde el slice 13. Los slices 1 y 2 la van a
  rozar; si el andamiaje queda puesto, se salda ahí y se dice.

## Preguntas abiertas

1. ~~**¿Cuánto tarda de verdad un GPX grande?**~~ **Medido: 378 ms para 20.000 puntos** en
   escritorio. Decidió la forma del slice 1: indicador indeterminado, sin porcentaje.
2. **¿Hacen falta todos los puntos de un GPX?** Caben de sobra —el aviso de desvío cuesta 0,39 ms por
   lectura con una ruta de 20.000 puntos, 0,7 s por hora de salida— así que **hoy no duele nada**.
   Pero probablemente sobran: la señal cruda sólo es buena hasta ~21 m (medido en nueve trazas) y un
   GPX a 1 Hz guarda puntos que se diferencian en centímetros. Simplificar a 2 m de tolerancia —diez
   veces por debajo del ruido— quitaría el grueso de los puntos sin cambiar el dibujo ni el desvío.
   **No se hace en este roadmap**: no arregla nada que duela, y tocar la geometría de lo guardado
   merece su propio encuadre.
3. **¿Qué cifras quiere ver el usuario mientras pedalea?** El slice 5 propone ritmo y tiempo en
   movimiento por ser las que más se piden en las apps del ramo, pero esto lo contesta mejor una
   salida que un roadmap.

## Estado

| Slice | Estado |
|---|---|
| 1 — la importación se ve, y la ruta se maneja desde el detalle | **Hecho** — ver la [bitácora](pulido-de-movimiento-bitacora.md) |
| 2 — la barra pinta su fondo | **Hecho** — ver la [bitácora](pulido-de-movimiento-bitacora.md) |
| 3 — controles honestos y Ajustes alcanzable | **Hecho** — ver la [bitácora](pulido-de-movimiento-bitacora.md) |
| 4 — el botón «atrás» del sistema | **Hecho** — ver la [bitácora](pulido-de-movimiento-bitacora.md) |
| 5 — moverse por el pilar se hace siempre igual | **Hecho** — ver la [bitácora](pulido-de-movimiento-bitacora.md) |
| 6 — el tracker durante la salida | Pendiente |
