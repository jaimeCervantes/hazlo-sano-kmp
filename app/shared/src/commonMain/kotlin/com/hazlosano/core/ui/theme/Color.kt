package com.hazlosano.core.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Los tokens de color de Hazlo Sano, portados del repo hermano `comida-justa`
 * (`src/presentation/design_system/tokens/colors.css`), que es donde se decide el diseño.
 *
 * Los valores en crudo son privados **a propósito**. Antes vivían aquí como constantes públicas
 * (`PillarMovement`, `HazloSanoGreen`, …) y por eso nueve pantallas acabaron pintando cifras con el
 * color de relleno de un pilar: si lo único que se puede pedir es "el color del pilar", nadie tiene
 * cómo distinguir rellenar de escribir. Ahora lo único que sale de este archivo son paletas, y una
 * paleta obliga a decir para qué se quiere el color.
 *
 * La lección de fondo la aprendió el sitio antes que nosotros: **una semilla de marca no es una
 * tinta**. Los dos tonos del logo identifican bien y ninguno de los dos sostiene texto blanco encima
 * (#538F39 da 3.92:1 y #F0380E da 3.98:1, ambos por debajo del 4.5:1 de AA). Por eso cada color de
 * este archivo pertenece a una rampa donde cada paso tiene un trabajo, y no a un tono suelto que
 * hace de todo.
 */

// ── Marca ────────────────────────────────────────────────────────────────────────────────────────
// 600 es la semilla: es el logo, identifica y no lleva texto encima. 700 es lo que consume la
// interfaz. 800 el estado activo. 900 la tinta sobre el chip tenue del mismo color.
private val BrandGreen600 = Color(0xFF538F39)
private val BrandGreen700 = Color(0xFF3F6F2A)
private val BrandGreen800 = Color(0xFF355D23)
private val BrandGreen900 = Color(0xFF2F5320)
private val BrandGreenSoft = Color(0xFFE8F0DF)
private val BrandGreenSoftDark = Color(0xFF1B2D0F)
private val BrandGreenDark = Color(0xFF6BA34A)
private val BrandGreen800Dark = Color(0xFF7CB85A)
private val BrandGreen900Dark = Color(0xFFDCEFCB)

private val BrandClay500 = Color(0xFFF0380E)
private val BrandClay600 = Color(0xFFDD340D)
private val BrandClay700 = Color(0xFFC52E0B)
private val BrandClaySoft = Color(0xFFFDE3DD)
private val BrandClaySoftDark = Color(0xFF36150D)
private val BrandOrangeDark = Color(0xFFF4522E)
private val BrandClay700Dark = Color(0xFFF77A5C)

private val BrandHoney500 = Color(0xFFF2B705)
private val BrandHoneySoft = Color(0xFFFDF3D6)
private val BrandHoneySoftDark = Color(0xFF3A2F08)
private val BrandHoneyInk = Color(0xFF7A5A03)
private val BrandHoneyInkDark = Color(0xFFF6E0A8)

private val BrandLightGreen = Color(0xFF5DBF17)
private val BrandGray = Color(0xFF3D4636)
private val BrandGrayDark = Color(0xFF2A3126)
private val BrandWhite = Color(0xFFFFFFFF)

// ── Neutrales ────────────────────────────────────────────────────────────────────────────────────
// Llevan el matiz del papel en vez del azul de `slate`: junto a un verde y un naranja cálidos, un
// gris azulado los apaga. Es la razón por la que el fondo deja de ser el #FFFBFF que traía el app.
private val TextBase = Color(0xFF1B1E18)
private val TextBaseDark = Color(0xFFECEADF)
private val TextSupport = Color(0xFF5C6857)
private val TextSupportDark = Color(0xFFA2AD98)
private val TextMuted = Color(0xFF656E5C)
private val TextMutedDark = Color(0xFF8A9480)

private val SurfaceBackground = Color(0xFFFAF7F1)
private val SurfaceBackgroundDark = Color(0xFF101410)
private val SurfaceElevation1 = Color(0xFFFFFFFF)
private val SurfaceElevation1Dark = Color(0xFF1A1F18)
private val SurfaceElevation2 = Color(0xFFF1ECE1)
private val SurfaceElevation2Dark = Color(0xFF232920)

private val Separator = Color(0xFFEDE8DC)
private val SeparatorDark = Color(0xFF333B2E)
private val BorderLight = Color(0xFFE3DDCE)
private val BorderDark = Color(0xFF46503D)

// El límite de un campo sí delimita un control, así que WCAG 1.4.11 le pide 3:1 — a diferencia de
// `border` y `separator`, que son decorativos.
private val BorderFieldLight = Color(0xFF8B8874)
private val BorderFieldDark = Color(0xFF667559)

private val ButtonPrimaryTextLight = Color(0xFFFFFFFF)
private val ButtonPrimaryTextDark = Color(0xFF0D1109)

private val FeedbackSuccessInk = Color(0xFF24301C)
private val FeedbackSuccessInkDark = Color(0xFFDCEFCB)
private val FeedbackWarningInk = Color(0xFF3F3208)
private val FeedbackErrorInk = Color(0xFF4A1405)
private val FeedbackErrorInkDark = Color(0xFFF9CFC4)

// ── Los cuatro pilares ───────────────────────────────────────────────────────────────────────────
// `solid` no cambia entre temas: lleva texto blanco encima en los dos y ya cumple AA. Lo que se
// invierte es la pareja `soft`/`ink`.
private val PillarSleepSolid = Color(0xFF7C3AED)
private val PillarSleepSoft = Color(0xFFF5F3FF)
private val PillarSleepSoftDark = Color(0xFF2E1065)
private val PillarSleepInk = Color(0xFF7C3AED)
private val PillarSleepInkDark = Color(0xFFC4B5FD)

private val PillarNutritionSolid = Color(0xFFDD340D)
private val PillarNutritionSoft = Color(0xFFFDE3DD)
private val PillarNutritionSoftDark = Color(0xFF36150D)
private val PillarNutritionInk = Color(0xFFC52E0B)
private val PillarNutritionInkDark = Color(0xFFF4522E)

private val PillarMovementSolid = Color(0xFF408410)
private val PillarMovementSoft = Color(0xFFE8F6DF)
private val PillarMovementSoftDark = Color(0xFF1B2D0F)
private val PillarMovementInk = Color(0xFF3C7B0F)
private val PillarMovementInkDark = Color(0xFF5DBF17)

private val PillarMindSolid = Color(0xFF0369A1)
private val PillarMindSoft = Color(0xFFF0F9FF)
private val PillarMindSoftDark = Color(0xFF0C2A3B)
private val PillarMindInk = Color(0xFF0369A1)
private val PillarMindInkDark = Color(0xFF38BDF8)

/**
 * Los tres papeles de un pilar.
 *
 * Existe porque un pilar tiene que rellenar una insignia **y** escribir una cifra, y ningún tono
 * hace las dos cosas: el que rellena bien es demasiado claro para leerse sobre papel, y el que se
 * lee sobre papel es demasiado oscuro para que el blanco destaque encima. Pedir "el color del
 * pilar" era justamente lo que dejaba elegir mal sin enterarse.
 */
data class PillarPalette(
    /** Relleno saturado — insignia, indicador, punto de color. Lleva texto blanco encima. */
    val solid: Color,
    /** El tinte de fondo de una superficie del pilar. No lleva texto blanco: lleva [ink]. */
    val soft: Color,
    /** Tinta: texto, iconos y bordes sobre [soft] o sobre el papel del tema. */
    val ink: Color,
)

/** Todo lo que el tema sabe de color, más allá de lo que Material 3 modela por su cuenta. */
data class HazloPalette(
    val brandSeed: Color,
    val brandGreen: Color,
    val brandGreenActive: Color,
    val brandGreenInk: Color,
    val brandGreenSoft: Color,
    val brandOrange: Color,
    val brandOrangeActive: Color,
    val brandOrangeSoft: Color,
    val brandHoney: Color,
    val brandHoneySoft: Color,
    val brandHoneyInk: Color,
    val brandGray: Color,
    val brandWhite: Color,
    val textBase: Color,
    val textSupport: Color,
    val textMuted: Color,
    val surfaceBackground: Color,
    val surfaceElevation1: Color,
    val surfaceElevation2: Color,
    val separator: Color,
    val border: Color,
    val borderField: Color,
    val buttonPrimaryText: Color,
    val feedbackSuccess: Color,
    val feedbackSuccessSoft: Color,
    val feedbackSuccessInk: Color,
    val feedbackWarning: Color,
    val feedbackWarningSoft: Color,
    val feedbackWarningInk: Color,
    val feedbackError: Color,
    val feedbackErrorSoft: Color,
    val feedbackErrorInk: Color,
    /** Tinta de acento: cifras marcadas, enlaces, lo que quiere destacar sin ser un pilar. */
    val highlight: Color,
    val sleep: PillarPalette,
    val nutrition: PillarPalette,
    val movement: PillarPalette,
    val mind: PillarPalette,
)

val LightHazloPalette: HazloPalette = HazloPalette(
    brandSeed = BrandGreen600,
    brandGreen = BrandGreen700,
    brandGreenActive = BrandGreen800,
    brandGreenInk = BrandGreen900,
    brandGreenSoft = BrandGreenSoft,
    brandOrange = BrandClay600,
    brandOrangeActive = BrandClay700,
    brandOrangeSoft = BrandClaySoft,
    brandHoney = BrandHoney500,
    brandHoneySoft = BrandHoneySoft,
    brandHoneyInk = BrandHoneyInk,
    brandGray = BrandGray,
    brandWhite = BrandWhite,
    textBase = TextBase,
    textSupport = TextSupport,
    textMuted = TextMuted,
    surfaceBackground = SurfaceBackground,
    surfaceElevation1 = SurfaceElevation1,
    surfaceElevation2 = SurfaceElevation2,
    separator = Separator,
    border = BorderLight,
    borderField = BorderFieldLight,
    buttonPrimaryText = ButtonPrimaryTextLight,
    feedbackSuccess = BrandLightGreen,
    feedbackSuccessSoft = BrandGreenSoft,
    feedbackSuccessInk = FeedbackSuccessInk,
    feedbackWarning = BrandHoney500,
    feedbackWarningSoft = BrandHoneySoft,
    feedbackWarningInk = FeedbackWarningInk,
    feedbackError = BrandClay600,
    feedbackErrorSoft = BrandClaySoft,
    feedbackErrorInk = FeedbackErrorInk,
    highlight = BrandGreen700,
    sleep = PillarPalette(PillarSleepSolid, PillarSleepSoft, PillarSleepInk),
    nutrition = PillarPalette(PillarNutritionSolid, PillarNutritionSoft, PillarNutritionInk),
    movement = PillarPalette(PillarMovementSolid, PillarMovementSoft, PillarMovementInk),
    mind = PillarPalette(PillarMindSolid, PillarMindSoft, PillarMindInk),
)

/**
 * En oscuro la rampa se invierte: el relleno se aclara hasta poder llevar tinta oscura encima, y las
 * superficies del pilar se oscurecen para seguir diferenciándose del papel.
 *
 * `brandSeed` y las semillas de arcilla y miel **no** cambian: son el logo, y el logo es el mismo de
 * noche. Lo que cambia es lo que la interfaz consume.
 */
val DarkHazloPalette: HazloPalette = HazloPalette(
    brandSeed = BrandGreen600,
    brandGreen = BrandGreenDark,
    brandGreenActive = BrandGreen800Dark,
    brandGreenInk = BrandGreen900Dark,
    brandGreenSoft = BrandGreenSoftDark,
    brandOrange = BrandOrangeDark,
    brandOrangeActive = BrandClay700Dark,
    brandOrangeSoft = BrandClaySoftDark,
    brandHoney = BrandHoney500,
    brandHoneySoft = BrandHoneySoftDark,
    brandHoneyInk = BrandHoneyInkDark,
    brandGray = BrandGrayDark,
    brandWhite = BrandWhite,
    textBase = TextBaseDark,
    textSupport = TextSupportDark,
    textMuted = TextMutedDark,
    surfaceBackground = SurfaceBackgroundDark,
    surfaceElevation1 = SurfaceElevation1Dark,
    surfaceElevation2 = SurfaceElevation2Dark,
    separator = SeparatorDark,
    border = BorderDark,
    borderField = BorderFieldDark,
    buttonPrimaryText = ButtonPrimaryTextDark,
    feedbackSuccess = BrandLightGreen,
    feedbackSuccessSoft = BrandGreenSoftDark,
    feedbackSuccessInk = FeedbackSuccessInkDark,
    feedbackWarning = BrandHoney500,
    feedbackWarningSoft = BrandHoneySoftDark,
    feedbackWarningInk = BrandHoneyInkDark,
    feedbackError = BrandOrangeDark,
    feedbackErrorSoft = BrandClaySoftDark,
    feedbackErrorInk = FeedbackErrorInkDark,
    highlight = BrandLightGreen,
    sleep = PillarPalette(PillarSleepSolid, PillarSleepSoftDark, PillarSleepInkDark),
    nutrition = PillarPalette(PillarNutritionSolid, PillarNutritionSoftDark, PillarNutritionInkDark),
    movement = PillarPalette(PillarMovementSolid, PillarMovementSoftDark, PillarMovementInkDark),
    mind = PillarPalette(PillarMindSolid, PillarMindSoftDark, PillarMindInkDark),
)

/**
 * El color de fondo de una barra sobre el mapa y sitios parecidos, donde hace falta dejar ver algo
 * por detrás. Equivale al `--glass-background` del sitio.
 */
fun HazloPalette.glass(): Color = surfaceElevation1.copy(alpha = 0.9f)
