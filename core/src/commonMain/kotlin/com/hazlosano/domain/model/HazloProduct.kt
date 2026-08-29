package com.hazlosano.domain.model

/**
 * Una publicación del catálogo.
 *
 * El nombre dice `Product` por su origen —cuando el catálogo era una lista sembrada de productos—
 * pero desde que se lee del sitio esto representa los cuatro tipos de [PublicationKind]. El
 * renombrado a `HazloPublication` está anotado como deuda: es mecánico, toca catorce archivos y no
 * cabía en el slice que trajo el catálogo remoto.
 *
 * [price] es **nulable a propósito**: un anuncio no se vende y un evento gratis tampoco tiene
 * precio. Antes era `Double` no nulo, que obligaba a inventar un 0.0 indistinguible de algo que de
 * verdad es gratis.
 */
data class HazloProduct(
    val id: String,
    val name: String,
    val description: String = "",
    val price: Double? = null,
    val imageUrl: String = "",
    val isFavorite: Boolean = false,
    val distanceMeters: Double? = null,
    val category: String = "",
    val subCategory: String? = null,
    val tags: List<String> = emptyList(),
    val productUrl: String? = null,
    val sellerId: String? = null,
    val isAvailable: Boolean = true,
    val kind: PublicationKind = PublicationKind.ANNOUNCEMENT,
    /** El pilar al que pertenece, o `null` si su categoría no cuelga de ninguno. */
    val pillar: PillarType? = null,
    /** Cuándo ocurre. Solo un evento la trae. Epoch millis, como el resto del dominio. */
    val startsAtEpochMillis: Long? = null,
    /** Cuándo termina. Opcional incluso en un evento: sin ella, caduca en su hora de inicio. */
    val endsAtEpochMillis: Long? = null,
    /** Cuánto dura, en minutos. Solo un servicio la trae. */
    val durationMinutes: Int? = null,
)
