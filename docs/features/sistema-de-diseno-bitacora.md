# Bitácora — el app habla el idioma de diseño del sitio

## Slice 1 — La paleta del sitio llega al tema (2026-09-05)

**Objetivo.** Que el app deje de pintar con la paleta que `comida-justa` ya jubiló, y que la razón
por la que la jubiló —una semilla de marca no sirve de tinta— quede expresada en el tipo, no en un
comentario. Spec: [`sistema_de_diseno.feature`](../../features/sistema_de_diseno.feature),
escenarios `@slice-1`.

### Lo que se midió antes de tocar nada

La fórmula de contraste de WCAG sobre los hex de los dos repos. Las dos cifras que el sitio publica
por su cuenta (3.92 para la semilla verde y 3.98 para la naranja) salieron idénticas, que es lo que
dio confianza en las demás:

| Papel | App antes | Contraste | Ahora | Contraste |
|---|---|---|---|---|
| Verde de marca, con texto blanco | `#538F39` | **3.92** ✗ | `#3F6F2A` | 5.97 ✓ |
| Pilar Sueño (relleno) | `#8B5CF6` | **4.24** ✗ | `#7C3AED` | 5.70 ✓ |
| Pilar Alimentación (relleno) | `#F0380E` | **3.98** ✗ | `#DD340D` | 4.59 ✓ |
| Pilar Movimiento (relleno) | `#538F39` | **3.92** ✗ | `#408410` | 4.64 ✓ |
| Pilar Mente (relleno) | `#38BDF8` | **2.14** ✗ | `#0369A1` | 5.93 ✓ |

### Decisiones y por qué

1. **Los valores en crudo pasan a ser `private`, y lo único público son paletas.** Es lo que impide
   que el problema vuelva. Mientras `PillarMovement` fuera una constante pública, la elección
   correcta ni siquiera existía: nueve pantallas escribían cifras con el color de **relleno** del
   pilar porque era el único que se podía pedir. El escenario «ningún color literal de pilar fuera
   del tema» no se comprueba con una prueba que lea el código fuente — se hace **imposible de
   escribir**, que es más barato y no se puede saltar.
2. **`PillarType.toColor()` muere; nace `PillarType.palette()`**, que devuelve `solid`/`soft`/`ink`.
   Devolver tres cosas obliga a decir para cuál de los dos trabajos se quiere el color.
3. **Hallazgo del camino: hay un tercer suelo que no es ni el papel claro ni el oscuro.** Cinco
   sitios pintan el color del pilar **encima de una foto oscurecida o de un velo negro**
   (`LargePillarCard`, `SmallPillarCard`, `NightCard`, `SleepSummaryCard`, `PillarSummaryCard`, y el
   `TrendMetric` del historial de sueño). Ahí la tinta del tema activo es la equivocada: en modo
   claro habría dejado el verde oscuro `#3C7B0F` sobre negro. Se añadió `pillarInkOnImage()`, que
   devuelve siempre la tinta del tema oscuro **con independencia del tema activo**, porque sobre foto
   el suelo siempre es oscuro. No estaba en el plan; salió al asignarle papel a cada sitio.
4. **`PillarBadge` y el chip de `PillarInfoScreen` dejan de fabricarse el fondo con alfa.** Los dos
   hacían `color.copy(alpha = 0.1f)` de fondo y escribían encima con ese mismo color a plena
   opacidad. Un tinte por alfa no garantiza contraste con nada: con el azul de Mente el texto quedaba
   en 2.14:1. Ahora usan la pareja `soft`/`ink`, que es exactamente el caso para el que existe.
5. **`HazloShapes` se renombra por función y no se porta el espejo de Tailwind.** El sitio mantiene
   `sm`/`md`/`lg` junto a `chip`/`control`/`card`/`panel`, pero sólo porque sus utilidades CSS
   referencian esas variables por nombre — un problema que aquí no existe. Se porta la decisión
   (nombrar por lo que se redondea), no el rodeo. Los nueve sitios que usaban la escala vieja se
   migraron uno a uno; el radio cambia de valor en tres de ellos, siempre hacia el de su papel.
6. **La prueba de paridad entre temas no es la del sitio, y es a propósito.** Allí compara las dos
   copias del bloque oscuro que CSS obliga a duplicar. Aquí hay un `data class` con dos instancias,
   así que el compilador ya cubre lo que aquella prueba cubría; lo que no ve es un token copiado del
   claro por descuido. Eso es lo que se mide: qué debe cambiar entre temas, qué no debe cambiar
   (`solid`), y que los cuatro hex jubilados no puedan volver por una fusión.

### Desviaciones respecto al roadmap

- **`Motion.kt` entró, pero todavía no lo consume nadie.** Las curvas y duraciones están portadas y
  disponibles; ninguna animación las usa aún porque este slice no anima nada. Es la única pieza del
  alcance que queda sin consumidor.
- **Las sombras (`HazloElevation`) también entraron sin consumidor.** Compose no tiene un color de
  sombra multiplataforma equivalente al `box-shadow` del sitio: `tonalElevation` tiñe con el color
  primario y `shadowElevation` sólo acepta un color en Android. Se dejan declaradas con su tinte
  verde y se aplicarán componente a componente cuando toque; declararlas y no cablearlas es honesto,
  y cablearlas a medias habría dejado sombras verdes junto a sombras negras, que es justo el defecto
  que el sitio nombra.
- **`HazloProductCardDefaults.accentColor` se borró** en vez de migrarse: era el naranja jubilado
  escrito a mano como valor por defecto, y el par de composables que lo heredaban ahora usan
  `MaterialTheme.colorScheme.primary`, que ya sale de esta misma paleta.

### Archivos tocados

- **Tema** (`app/shared/.../core/ui/theme/`): `Color.kt` reescrito (tokens privados + `PillarPalette`
  + `HazloPalette` + las dos instancias), `Theme.kt` (mapeo a Material 3 y `LocalHazloPalette`),
  `Spacing.kt` (`HazloShapes` por función, `HazloElevation` nuevo), `Motion.kt` (nuevo).
- **Modelo de UI**: `core/ui/model/PillarVisuals.kt` (`palette()`, `of()`, `pillarInkOnImage()`).
- **Componentes**: `PillarBadge.kt` (pasa a recibir la paleta), `NightCard.kt`, `HazloSkeleton.kt`,
  `PillarBoardSkeleton.kt`, `HazloProductCard.kt`, `HazloChampionCard.kt`, `HazloChallengeCard.kt`.
- **Pantallas**: `MainScreen.kt`, `HomeScreen.kt`, `HomeSkeleton.kt`, `PillarCatalogScreen.kt`,
  `PillarInfoScreen.kt`, `SleepScreen.kt`, `SleepHistoryScreen.kt`, `MovementHistoryScreen.kt`,
  `SessionDetailScreen.kt`, `RoutesScreen.kt`, `RouteDetailScreen.kt`.
- **Tests**: `commonTest/core/ui/theme/Contrast.kt` (nuevo, la fórmula),
  `HazloPaletteContrastTest.kt` (nuevo), `HazloPaletteThemeParityTest.kt` (nuevo),
  `jvmTest/.../BottomTabTest.kt` (compara la paleta entera en vez de un color).
- **Docs**: `docs/features/sistema-de-diseno.md`, `features/sistema_de_diseno.feature`.

### Comandos y resultados

- `.\gradlew.bat :app:shared:jvmTest` → **294 pruebas, 0 fallos** (subiendo de las 283 con las que
  cerró el slice 8 del tablero; +11 de contraste y paridad).
- `.\gradlew.bat :app:shared:check` → BUILD SUCCESSFUL (JVM, JS e iOS; `iosSimulatorArm64Test` se
  salta en Windows, como siempre).
- `.\gradlew.bat :core:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug` → BUILD SUCCESSFUL.

**Nota de tooling:** lanzar `:app:shared:check` y `:app:androidApp:assembleDebug` en la **misma**
invocación falla en `copySharedComposeResourcesToAssets` — las dos tareas escriben el mismo
directorio de assets. Por separado pasan las dos. No es del código; queda anotado para no volver a
diagnosticarlo.

### Sin cobertura de host (dicho explícitamente)

- **Que la pantalla se vea bien no lo comprueba nada.** Los tests miden contraste entre pares de
  color, que es una propiedad de la paleta, no del render: nadie afirma que un texto concreto use el
  papel que le toca. Comprobación manual pendiente al instalar: abrir los cuatro pilares en claro y
  en oscuro y mirar que ninguna cifra se pierda contra su fondo.
- **`pillarInkOnImage()` depende de que la foto de debajo sea oscura.** Lo es porque
  `AsyncImageBackground` y las tarjetas aplican un velo negro, pero si alguien quita ese velo la
  regla deja de valer en silencio.
- **Los radios nuevos cambian de valor en tres sitios** (la tesela de icono de Inicio, el avatar del
  feed y las tarjetas de 16 a 18). Ningún test mira radios; se ve o no se ve al abrir la app.

### Seguimientos

- `HazloElevation` y `HazloMotion` esperan consumidor. Candidato natural: el slice 4 de
  `puertas-de-movimiento`, que rehace las pantallas de rutas.
- `LeafCard` sigue con su recorte de hoja escrito a mano (24/8/8/24) sin pasar por `HazloShapes`. Es
  una forma con identidad propia, no un radio de la escala; queda anotado por si conviene nombrarla.
- `MapTheme.kt` sigue con un color de marcador literal y un comentario que menciona una constante ya
  borrada (`GreenGrey80`).

**Recap.** El app pinta ya con la paleta verificada del sitio, en claro y en oscuro, y con la
separación que la hacía funcionar: cada pilar entrega un relleno, un papel tenue y una tinta en vez
de un tono que hacía de todo. Los cuatro colores con los que el app llevaba meses —tres de ellos por
debajo de AA, y el de Mente en 2.14:1— ya no se pueden escribir: los valores en crudo son privados y
lo único que sale del tema son paletas. Once pruebas nuevas miden los pares en los dos temas, así que
la próxima divergencia falla en la consola y no en la pantalla de alguien.

**Próximos pasos (opciones).** (1) El slice 2, las dos voces de la marca —Newsreader y Plus Jakarta
empaquetadas—, que es lo siguiente del roadmap; (2) cablear `HazloElevation` a los componentes de
tarjeta, que quedó declarado sin consumidor; (3) saltar al slice 3, el selector de tema, si prefieres
ver antes algo que se pueda tocar.

---

## Slice 2 — Las dos voces de la marca (2026-09-05)

**Objetivo.** Que el app deje de sonar a panel de administración. El sitio reparte su tipografía en
dos trabajos —Newsreader para lo que la marca **afirma**, Plus Jakarta Sans para lo que la interfaz
**opera**— y el app iba con `FontFamily.Default` y **un solo estilo declarado** (`bodyLarge`), con
los otros catorce en los de Material. Spec: escenarios `@slice-2`.

### Decisiones y por qué

1. **Las fuentes van empaquetadas, no pedidas a un servidor.** El pilar de Movimiento existe para
   usarse en el monte; una fuente que no carga es una pantalla sin texto. Son 627 KB entre las dos.
2. **Se empaquetan las variables, y no las estáticas, porque las estáticas ya no se distribuyen.**
   El endpoint `fonts.google.com/download?family=…` devuelve hoy HTML en vez de un zip, y
   `google/fonts` sólo publica `Newsreader[opsz,wght].ttf` y `PlusJakartaSans[wght].ttf`. Se
   comprobó que Compose Multiplatform 1.11 acepta `Font(resource, weight, style, variationSettings)`,
   así que cada peso se registra fijando el eje `wght`.
3. **Consecuencia declarada: Android 7 no verá los pesos.** Los ejes variables piden API 26 y el
   `minSdk` del proyecto es **24**. En 24 y 25 las dos caras salen en su instancia por defecto (400)
   y el sistema sintetiza la negrita. Es la degradación que costaba menos: la alternativa era
   empaquetar siete archivos que ya no existen río arriba.
4. **La frontera entre las dos voces es de trabajo, no de tamaño.** `display` y `headline` hablan con
   la serif; de `title` hacia abajo manda la sans, aunque `titleLarge` (22) sea mayor que
   `headlineSmall` (20). Un `titleMedium` es el nombre de una sección, y una sección la opera la
   interfaz.
5. **Los quince estilos de Material se declaran, ninguno se deja al azar.** Es lo que el test
   comprueba: que ninguno se quede en la fuente del sistema, que era exactamente el estado anterior.

### Archivos tocados

- **Fuentes:** `app/shared/src/commonMain/composeResources/font/newsreader.ttf` (451 KB),
  `plus_jakarta_sans.ttf` (176 KB).
- **Licencias:** `app/shared/licenses/Newsreader-OFL.txt`, `PlusJakartaSans-OFL.txt`. La OFL pide que
  la licencia acompañe a la fuente.
- **Tema:** `core/ui/theme/Type.kt` reescrito (`hazloDisplayFamily`, `hazloUiFamily`,
  `hazloTypography`), `Theme.kt` (una línea: pasa a `hazloTypography()`).
- **Tests:** `jvmTest/core/ui/theme/HazloTypographyTest.kt` (nuevo, 3 pruebas).

### Comandos y resultados

- `.\gradlew.bat :app:shared:jvmTest` → **297 pruebas, 0 fallos** (+3 sobre el slice 1).
- `.\gradlew.bat :app:shared:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:androidApp:assembleDebug` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:desktopApp:check` → BUILD SUCCESSFUL.
- `.\gradlew.bat :app:webApp:check` → BUILD SUCCESSFUL.

### Sin cobertura de host (dicho explícitamente)

- **Que las fuentes se vean no lo comprueba nada.** El test mide tamaños y familias del `Typography`,
  no el render: nadie afirma que el glifo que aparece sea Newsreader. Comprobación manual al
  instalar, y en particular en la web, donde la carga de una fuente empaquetada pasa por otro camino.
- **El eje `opsz` de Newsreader se queda en su valor por defecto.** Es una serif de óptico variable:
  fijando sólo `wght`, un titular de 56 usa el mismo dibujo que uno de 20. Se ve, aunque poco; queda
  como afinado posible.
- **La degradación de Android 24-25 no está probada en dispositivo**, sólo razonada desde el `minSdk`.

### Seguimientos

- **Una pantalla de licencias.** Los dos OFL viven en el repo, que cumple la atribución para quien
  lea el código, pero el app no los enseña. Candidato natural: la pantalla de ajustes del slice 3.
- El eje `opsz`, si algún titular grande se ve tosco.

**Recap.** El app tiene ya las dos voces del sitio, empaquetadas y con su licencia: la serif
editorial para lo que la marca afirma y la sans humanista para lo que la interfaz opera, sobre la
escala de nueve tamaños del sitio en vez de los quince valores por defecto de Material. Lo único que
quedaba escrito antes era un `bodyLarge`; ahora los quince estilos están declarados y una prueba
impide que alguno vuelva a caer en la fuente del sistema.

**Próximos pasos (opciones).** (1) El slice 3, el selector de tema, que es lo siguiente del roadmap y
además le da casa a la pantalla de licencias; (2) afinar el eje `opsz` de los titulares; (3) saltar a
las puertas de Movimiento si prefieres ver antes lo que pediste primero.
