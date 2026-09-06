# Feature: el app habla el idioma de diseño del sitio

Roadmap de slices. Escrito el **2026-09-05**.
Spec: [`features/sistema_de_diseno.feature`](../../features/sistema_de_diseno.feature).
Fuente de verdad del diseño: el repo hermano `comida-justa`, en
`src/presentation/design_system/tokens/`.

Va **antes** que [`puertas-de-movimiento.md`](puertas-de-movimiento.md) por decisión del usuario
(2026-09-05): así las pantallas nuevas de Movimiento nacen ya en el idioma nuevo y no hay que
repintarlas un slice después.

## Problema / Savings / Why

- **Problema:** el app y el sitio son la misma marca sobre la misma base de datos, y no se parecen.
  El app se quedó con la paleta que el sitio ya jubiló, y no es solo cuestión de gusto: **tres de los
  cuatro colores de pilar no cumplen AA** con texto encima, y el verde de marca tampoco. Además el
  app tiene 121 cadenas en español y **54 en inglés**, 46 textos escritos en duro dentro de
  Composables, y ninguna forma de elegir tema ni idioma — el sitio ya tiene las dos.
- **Savings:** deja de haber dos paletas que mantener, y deja de crecer la deuda: hoy cada pantalla
  nueva hereda un verde que no se lee. Un usuario que prefiere el modo claro deja de depender de lo
  que decida su teléfono, y quien no habla español deja de encontrarse media app sin traducir.
- **Why:** el catálogo del app ya se lee del sitio (slice 3 de `datos-en-postgres`) y el login lo
  atará a la misma cuenta. Si el contenido es el mismo y la cuenta va a ser la misma, parecer dos
  productos distintos es un defecto, no una decisión.

## Lo que se comprobó antes de planificar

Medido con la fórmula de contraste de WCAG sobre los hex de los dos repos. Las dos cifras que el
sitio documenta por su cuenta (3.92 para la semilla verde, 3.98 para la naranja) salen idénticas, lo
que da confianza en el resto:

| Papel | App hoy | Contraste | Web hoy | Contraste |
|---|---|---|---|---|
| Verde de marca (relleno, texto blanco) | `#538F39` | **3.92** ✗ | `#3F6F2A` | 5.97 ✓ |
| Pilar Movimiento (relleno) | `#538F39` | **3.92** ✗ | `#408410` | 4.64 ✓ |
| Pilar Movimiento (tinta sobre papel) | — no existe | — | `#3C7B0F` | 5.21 ✓ |
| Pilar Alimentación (relleno) | `#F0380E` | **3.98** ✗ | `#DD340D` | 4.59 ✓ |
| Pilar Sueño (relleno) | `#8B5CF6` | **4.24** ✗ | `#7C3AED` | 5.70 ✓ |
| Pilar Mente (relleno) | `#38BDF8` | **2.14** ✗ | `#0369A1` | 5.93 ✓ |

El fallo de fondo no son los hex: es que **el app usa un solo color por pilar para dos trabajos
distintos** —rellenar una insignia y escribir una cifra— y el sitio ya descubrió que ninguna semilla
de marca sirve para las dos cosas. Su respuesta fue una rampa de tres papeles por pilar (`solid`
para rellenar, `soft` para el chip tenue, `ink` para escribir encima de `soft`), y es lo que hay que
traer.

Otros hechos verificados:

- **`Theme.kt` declara `primary = #538F39` con `onPrimary = White`**: exactamente el par que el sitio
  documentó como roto. Alcanza a todos los botones primarios del app.
- **Las superficies son frías**: `#FFFBFF` de fondo, contra el `#FAF7F1` de papel cálido del sitio.
  El propio comentario del sitio explica por qué: un gris azulado junto a un verde y un naranja
  cálidos los apaga.
- **`Type.kt` define un solo estilo** (`bodyLarge`) sobre `FontFamily.Default`. El sitio tiene dos
  voces con trabajos distintos: Newsreader para lo que la marca *afirma* y Plus Jakarta Sans para lo
  que la interfaz *opera*. No hay ninguna fuente empaquetada en `composeResources`.
- **`HazloShapes` existe pero no coincide con nada**: el sitio nombra sus radios por lo que redondean
  (`chip` 8, `control` 12, `card` 18, `panel` 26) precisamente para no tener que decidir un número en
  cada componente.
- **Las sombras del sitio no son negras**: llevan el verde del papel (`rgba(31,40,24,…)`), porque un
  negro puro sobre fondo cálido tira a gris y ensucia.
- **Compose Multiplatform 1.11.0 trae `LocalComposeEnvironment` y `LanguageQualifier`**, así que el
  idioma se puede cambiar en caliente sin dependencias nuevas ni reiniciar la app.
- **No hay ningún almacén de preferencias** en el repo: ni DataStore ni multiplatform-settings. La
  base SQLDelight ya existe y funciona en los cinco targets, así que tema e idioma se guardan ahí en
  vez de sumar una dependencia.

## Decisiones tomadas antes de empezar

1. **Los tokens se copian con su significado, no con su nombre CSS.** En Compose no hay variables en
   cascada; lo que hay es un `ColorScheme` de Material 3 más un objeto propio para lo que Material no
   modela (las rampas de pilar). Traer `--surface-elevation-1` como `surface` es la traducción
   correcta; inventar un `HazloTokens.surfaceElevation1` paralelo a `MaterialTheme.colorScheme` sería
   una segunda fuente para lo mismo.
2. **Los tests de contraste se portan, no se confía en la copia.** El sitio tiene
   `brandPalette.contrast.test.ts` y `pillarPalette.contrast.test.ts` midiendo pares semánticos. Sin
   su equivalente en Kotlin, la paleta vuelve a divergir en silencio — que es exactamente cómo
   llegamos aquí.
3. **`PillarType.toColor()` desaparece.** Devolver un `Color` suelto es lo que hace posible usar el
   mismo tono para rellenar y para escribir. Pasa a `PillarType.palette()`, que devuelve los tres
   papeles y obliga a elegir cuál se quiere.
4. **Las fuentes se empaquetan, no se piden a un servidor.** El app puede estar sin red en el monte;
   una fuente que no carga es una pantalla sin texto.
5. **Tema e idioma son ajustes del usuario, con "seguir al sistema" como opción y como valor de
   partida.** El sitio hace lo mismo: su `data-theme` gana sobre `prefers-color-scheme` en ambas
   direcciones, y no tenerlo puesto significa seguir al sistema.
6. **El inglés se completa antes de ofrecer el selector de idioma.** Un selector que lleva a media
   app sin traducir es peor que no tenerlo.

## Slices

### Slice 1 — La paleta del sitio llega al tema

Escenarios `@slice-1`.

**Alcance:**

- `core/ui/theme/Color.kt`: se reescribe con la rampa de marca (`brandGreen600/700/800/900/soft`, la
  rampa de arcilla, la de miel), los neutrales cálidos (`surfaceBackground`, `surfaceElevation1`,
  `surfaceElevation2`, `textBase`, `textSupport`, `textMuted`, `separator`, `border`, `borderField`)
  y las cuatro rampas de pilar de tres papeles, en claro y en oscuro.
- `core/ui/theme/PillarPalette.kt` (nuevo): `data class PillarPalette(solid, soft, ink)` y
  `PillarType.palette(): PillarPalette`, resuelto según el tema activo.
- `core/ui/theme/Theme.kt`: `lightColorScheme`/`darkColorScheme` se re-mapean a los tokens
  (`primary` ← `brandGreen`, `onPrimary` ← `buttonPrimaryText`, `background` ← `surfaceBackground`,
  `surface` ← `surfaceElevation1`, `surfaceVariant` ← `surfaceElevation2`, `outline` ←
  `borderField`, `outlineVariant` ← `border`, `error`/`errorContainer`/`onErrorContainer` ← la
  familia `feedback-error`).
- `core/ui/theme/Spacing.kt`: `HazloShapes` pasa a nombrar por función (`chip` 8, `control` 12,
  `card` 18, `panel` 26, `pill`), y entra `HazloElevation` con las cinco sombras teñidas.
- `core/ui/theme/Motion.kt` (nuevo): `easeStandard`, `easeNatural`, `durationFast` 150 ms,
  `durationBase` 260 ms.
- Los llamadores de `toColor()` pasan a `palette()` eligiendo papel: `PillarMovement` en duro
  desaparece de `MovementHistoryScreen`, `RoutesScreen` y `TrackerScreen`.

**Criterios de aceptación:**

- Los cuatro pilares y el verde de marca cumplen AA (≥ 4.5:1) en su par relleno/texto, en claro y en
  oscuro.
- Cada pilar expone tres papeles y ninguno se usa para escribir sobre sí mismo.
- El fondo del app es el papel cálido del sitio, no el blanco frío.
- Ningún archivo fuera del tema declara un color literal de pilar.

**Tests:** `commonTest` con la fórmula de contraste de WCAG midiendo los pares semánticos, portada
de `brandPalette.contrast.test.ts` y `pillarPalette.contrast.test.ts`; test de paridad claro/oscuro
que afirma que ningún papel queda definido en un solo tema, portado de `darkThemeParity.test.ts`.

### Slice 2 — Las dos voces de la marca

Escenarios `@slice-2`. Newsreader y Plus Jakarta Sans entran como `FontResource` en
`composeResources/font/`; la escala tipográfica del sitio (10 / 12 / 14 / 16 / 18 / 20 / 26 / 40 /
56) se mapea sobre `Typography` de Material 3, con la serif reservada a titulares y nombres de pilar
y la sans a todo lo que la interfaz opera.

### Slice 3 — El tema se elige

Escenarios `@slice-3`. Pantalla de ajustes alcanzable desde el icono de menú que `HazloTopAppBar` ya
pinta sin hacer nada. Claro / oscuro / seguir al sistema, guardado en una tabla de ajustes de la base
SQLDelight y aplicado sin reiniciar.

### Slice 4 — Ningún texto vive fuera del catálogo

Escenarios `@slice-4`. Los 46 textos en duro pasan a `Res.string.*`; `RoutesViewModel` deja de
exponer un `message: String` ya redactado y pasa a un tipo sellado de mensajes, como pide
`AGENTS.md`; y `values-en` se completa de 54 a las 121 que tiene el español. Salda la deuda de i18n
que las bitácoras de Movimiento arrastran desde el slice 13.

### Slice 5 — El idioma se elige

Escenarios `@slice-5`. Español / inglés / seguir al sistema, en la misma pantalla de ajustes,
resuelto con `LocalComposeEnvironment` y persistido igual que el tema.

## Fuera de alcance

- Rediseñar pantallas. Este roadmap cambia el **idioma visual**, no la disposición; lo que se
  reordena entra en [`puertas-de-movimiento.md`](puertas-de-movimiento.md).
- Un tercer idioma. La mecánica queda puesta para que sumar uno sea un archivo de recursos.
- Portar los componentes del sitio (`Button`, `Badge`, `Alert`…) uno a uno. Se traen sus tokens; los
  componentes del app ya existen y se quedan.
