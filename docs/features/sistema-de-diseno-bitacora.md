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

---

## Corrección del slice 2 — La barra inferior partía el nombre de un pilar (2026-09-06)

**Cómo se supo.** Por una captura del usuario, no por una prueba. En el teléfono, "Mente y
Espíritu" partía en dos renglones y dejaba esa pestaña más alta que las otras cuatro, con su
indicador desalineado respecto al resto de la barra.

**Causa.** Es una regresión del slice 2. Plus Jakarta Sans es más ancha que la fuente del sistema con
la que el app venía, así que un rótulo que antes entraba justo dejó de entrar. Los otros cuatro
—"Inicio", "Sueño", "Alimentación", "Movimiento"— siguen cabiendo; el de Mente es el único cuyo
nombre son tres palabras.

**Por qué ninguna prueba lo vio.** `BottomTabTest` comparaba el rótulo de la pestaña con
`pillarLabel(pillar)`, o sea con **el valor que rompía la barra**. Una prueba que afirma que dos
cosas son iguales no puede detectar que una de las dos no cabe.

**Decisiones y por qué.**

1. **Dos rótulos, no uno.** La pestaña enseña `pillarShortLabel()` —"Mente"— y anuncia
   `pillarLabel()` —"Mente y Espíritu"— por `contentDescription`. Acortar por falta de sitio es una
   decisión visual y no hay razón para que le llegue a quien no está mirando la barra.
2. **Sólo Mente tiene forma corta.** Los demás devuelven su nombre completo, así que quien necesite
   la versión compacta puede pedirla siempre sin preguntar de qué pilar se trata.
3. **El rótulo baja a `labelSmall` (10 sp), a un renglón fijo**, con `maxLines = 1` y elipsis. La
   altura de la barra no puede depender de lo largo que sea el nombre de un pilar — es la regla de
   dimensiones estables de `AGENTS.md`, que este fallo incumplía.
4. **La prueba nueva fija una anchura, no una igualdad.** Ningún rótulo puede pasar de 12 caracteres,
   que es lo que mide "Alimentación", el más largo que sí entra. Es lo que habría cazado esto.

**Archivos tocados.** `MainScreen.kt` (`label()` corto, `accessibleLabel()` entero, estilo del
rótulo), `core/ui/model/PillarVisuals.kt` (`pillarShortLabel()`), `values/strings.xml`
(`pillar_mind_short`), `jvmTest/.../BottomTabTest.kt` (+2 pruebas).

---

## Hallazgo aparte — Un test de Movimiento tenía una carrera (2026-09-06)

Salió al revalidar lo anterior, y **no tiene relación con ningún cambio de esta feature**:
`SessionDetailIntegrationTest.openingASessionBringsTheHistorySummaryInLineWithItsRoute` falló una vez
esperando "390 m" y leyendo "1.25 km".

**Causa.** `SessionDetailViewModel` emite el detalle **antes** de esperar a
`refreshSessionSummary(detail)` —enseñar la sesión sin bloquearse en una escritura es lo correcto
para quien la abre— y ese refresco entra en `Dispatchers.Default`, que `runTest` no controla. El test
leía el historial justo después: una carrera que se gana casi siempre.

**Decisión.** Se arregla el **test**, no el ViewModel: el comportamiento de producción es el bueno.
El test pasa a esperar el efecto observable en el flujo de SQLDelight en vez de leer la fila una vez.
Si el refresco no llegara nunca, `runTest` corta por su propio tiempo límite — un fallo más feo que
una aserción, pero fallo, y sin intermitencia.

**Verificación.** Tres pasadas seguidas de `:app:shared:jvmTest --rerun-tasks`: **299 pruebas, 0
fallos, 0 errores** en las tres.

**Error de método que costó una vuelta, anotado para no repetirlo.** La primera verificación de
intermitencia no valía: se lanzó una segunda tanda de pasadas sin esperar a que terminara la primera,
así que corrieron **dos Gradle a la vez** sobre el mismo proyecto pisándose los directorios de build,
y una pasada informó 147 fallos que no significaban nada. Además se contaban líneas con la palabra
`FAILED`, que incluye la de la tarea y la del build: "3 fallos" podía ser un solo test. Las cuentas
salen de los XML de `build/test-results`, y las pasadas van en serie.

---

## Slice 3 — El tema se elige (2026-09-06)

**Objetivo.** Que el tema deje de decidirlo el teléfono. Hasta aquí `HazloSanoTheme` leía
`isSystemInDarkTheme()` y no había forma de contradecirlo; el sitio hermano ya tiene su conmutador.
Spec: escenarios `@slice-3`.

### Decisiones y por qué

1. **La tabla de ajustes es clave/valor, no una columna por ajuste.** Los ajustes se añaden de uno
   en uno y por slices distintos —el tema ahora, el idioma en el slice 5—, y una columna por ajuste
   convierte cada uno en una migración. Con clave/valor, sumar un ajuste es una constante en Kotlin
   y ninguna migración. El precio —el valor viaja como `TEXT` y alguien tiene que interpretarlo— se
   paga en un solo sitio: `ThemePreference.fromStoredValue`.
2. **`SYSTEM` es un valor, no la ausencia de valor.** Seguir al teléfono es una elección con
   contenido y se puede volver a ella después de haber fijado claro u oscuro; modelarla como `null`
   habría hecho imposible distinguir «nunca eligió» de «eligió seguir al sistema», que es justo lo
   que hay que poder deshacer.
3. **La única decisión de verdad vive en `core`:** `ThemePreference.resolvesToDark(systemIsDark)`.
   El resto es fontanería. Separarla la hace comprobable sin levantar una composición, y es donde
   está el caso que justifica la pantalla entera: elegir claro con el teléfono en oscuro.
4. **Un valor guardado que no se reconoce vuelve a seguir al sistema en vez de estallar.** Puede
   llegar de una versión futura que añadió una opción, o de una base editada a mano. Un ajuste no
   puede tumbar el arranque.
5. **El respaldo de la web es en memoria, no un no-op.** `DatabaseProvider.initialize` se llama en
   Android, escritorio e iOS, y **en ningún sitio de `jsMain`**. Con el patrón no-op del pilar de
   movimiento, el selector habría quedado pintado y muerto en la web: se toca oscuro y no pasa nada.
   En memoria, el ajuste funciona durante la sesión y sólo se pierde al recargar — una limitación
   honesta en vez de un control roto.
6. **Un flujo por ajuste, no un objeto de preferencias.** Es lo que evita que cambiar el idioma
   recomponga a quien observa el tema cuando el slice 5 sume el segundo.
7. **Se toca la fila entera, no sólo el círculo.** Un objetivo de 24 dp es incómodo en un teléfono y
   no hay nada más en la fila que pueda querer el toque. Va con `selectableGroup`, así que un lector
   de pantalla anuncia «1 de 3» en vez de tres interruptores sueltos que resultan excluirse.

### Tres tropiezos del camino, por si vuelven

- **El dialecto de SQLite de este proyecto es 3.18 y el `UPSERT` llegó en la 3.24.** `ON CONFLICT DO
  UPDATE` no compila. Se usa `INSERT OR REPLACE`, que aquí hace lo mismo —la fila se identifica sólo
  por su clave y no cuelga nada de ella— en vez de mover el dialecto de todo el proyecto por un
  ajuste.
- **La columna se llama `settingValue` y no `value`**: `value` choca con la palabra reservada de
  Kotlin y SQLDelight genera un parámetro `value_`. Una fuga del generador en la firma que lee todo
  el mundo no vale el ahorro de cinco letras.
- **Kotlin/Native no admite comas en los nombres de test con acentos graves.** Falló sólo en
  `compileTestKotlinIosSimulatorArm64`, o sea después de que la JVM pasara en verde: los nombres de
  `commonTest` se compilan para todos los targets.

### Deuda que este slice paga de paso

`columnsOf` vivía como copia privada en `CatalogCacheMigrationTest` y en
`MovementSessionMigrationTest`. Al necesitarlo un tercer test de migración se **movió** a
`InMemoryHazloSanoDatabase.kt`, el helper compartido, y las dos copias se borraron — no se hizo una
tercera. Entró con él `tableNames()`.

### Archivos tocados

- **core:** `domain/settings/ThemePreference.kt` (nuevo, con `resolvesToDark`),
  `domain/settings/AppSettingsRepository.kt` (nuevo).
- **Base:** `sqldelight/.../AppSetting.sq` (nuevo), `7.sqm` (nuevo, versión 7 → 8).
- **Datos:** `data/settings/SqlDelightAppSettingsRepository.kt`,
  `data/settings/AppSettingsRepositoryProvider.kt` (ambos nuevos).
- **Presentación:** `feature/settings/presentation/SettingsViewModel.kt` y su factoría (nuevos).
- **UI:** `feature/settings/ui/SettingsScreen.kt` (nuevo), `MainScreen.kt` (el icono de menú, que
  llevaba desde siempre sin hacer nada, abre ajustes), `App.kt` (el tema elegido llega a la raíz).
- **Recursos:** siete cadenas de ajustes en `values/strings.xml`.
- **Tests:** `core/commonTest/.../ThemePreferenceTest.kt` (5),
  `jvmTest/data/db/AppSettingsMigrationTest.kt` (6),
  `commonTest/.../SettingsViewModelTest.kt` (4), `jvmTest/.../SettingsScreenTest.kt` (4).
- **Helpers de test:** `InMemoryHazloSanoDatabase.kt` (+`columnsOf`, +`tableNames`),
  `CatalogCacheMigrationTest.kt` y `MovementSessionMigrationTest.kt` (pierden su copia privada).

### Comandos y resultados

- `.\gradlew.bat :core:jvmTest` → **129 pruebas, 0 fallos** (venían 124).
- `.\gradlew.bat :app:shared:jvmTest` → **313 pruebas, 0 fallos** (venían 299).
- `.\gradlew.bat :core:check` y `:app:shared:check` → BUILD SUCCESSFUL, incluido
  `verifyCommonMainHazloSanoDatabaseMigration`, que es lo que comprueba que la migración deja el
  esquema donde el `.sq` dice.
- `.\gradlew.bat :app:androidApp:assembleDebug`, `:app:desktopApp:check`, `:app:webApp:check` →
  BUILD SUCCESSFUL los tres.

### Sin cobertura de host (dicho explícitamente)

- **Que el app se repinte al elegir no lo comprueba ningún test.** Se comprueba que el ViewModel
  publica el cambio y que `resolvesToDark` decide bien; que `App.kt` esté leyendo ese flujo y no otro
  se ve al abrir la app. Comprobación manual: poner el teléfono en oscuro, elegir claro en ajustes y
  ver que el app queda en claro; cerrarlo y abrirlo, y que siga.
- **El respaldo en memoria de la web no está probado en un navegador**, sólo razonado desde dónde se
  llama a `DatabaseProvider.initialize`.
- **La migración se prueba con SQLite en memoria por JDBC**, no contra una base real de un teléfono
  que venía de una versión anterior.

### Seguimientos

- **La pantalla de licencias sigue pendiente.** El slice 2 dejó los dos OFL en el repo y anotó que
  el app no los enseña; ahora ya hay dónde ponerlos, pero meterlo aquí habría sido ensanchar el
  slice. Es el candidato natural para lo siguiente que toque ajustes.
- El slice 5 sumará el idioma a esta misma pantalla, y con él la sección Apariencia deja de ser la
  única.

**Recap.** El tema deja de decidirlo el teléfono: hay una pantalla de ajustes —alcanzable por el
icono de menú, que llevaba desde el principio sin hacer nada— con claro, oscuro y seguir al sistema,
guardada en la base y aplicada sin reiniciar. La única decisión de verdad vive en `core` y está
probada aparte; lo demás es fontanería con su migración verificada. En la web, donde no hay base, el
ajuste funciona durante la sesión en vez de quedar muerto.

**Próximos pasos (opciones).** (1) El slice 4, que saca los 46 textos en duro al catálogo y completa
el inglés de 54 a 121 — es lo que hace falta antes de poder ofrecer el selector de idioma;
(2) la pantalla de licencias, que ahora es barata; (3) saltar a las puertas de Movimiento.
