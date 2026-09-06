package com.hazlosano.core.ui.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.roundToLong

object HazloProductCardDefaults {
    val containerShape: RoundedCornerShape = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 8.dp,
        bottomStart = 8.dp,
        bottomEnd = 24.dp,
    )
    val imageHeight: Dp = 120.dp
    const val unavailableDistanceLabel: String = "--"
    const val favoriteContentDescription: String = "Favorite"
    const val distanceContentDescription: String = "Distance"
}

/**
 * [price] es nulable desde que el catálogo trae los cuatro tipos de publicación: un anuncio no se
 * vende y un evento gratis tampoco tiene precio. Cuando falta se pinta [priceFallbackLabel], que el
 * llamante resuelve del catálogo de cadenas — la tarjeta no lee recursos para seguir siendo
 * dibujable desde una preview o un test sin entorno.
 */
@Composable
fun HazloProductCard(
    title: String,
    description: String,
    price: Double?,
    isFavorite: Boolean,
    distanceMeters: Double?,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier,
    priceFallbackLabel: String? = null,
    /**
     * Una línea corta encima del título: cuándo ocurre un evento, cuánto dura un servicio.
     *
     * La resuelve el llamante y no la tarjeta, por la misma razón que [priceFallbackLabel]: así
     * esto se sigue pudiendo dibujar sin entorno de recursos.
     */
    overlineLabel: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    favoriteContentDescription: String = HazloProductCardDefaults.favoriteContentDescription,
    distanceContentDescription: String = HazloProductCardDefaults.distanceContentDescription,
    imageContent: @Composable BoxScope.() -> Unit = {
        HazloProductCardImagePlaceholder(
            title = title,
            accentColor = accentColor,
        )
    },
) {
    Surface(
        modifier = modifier,
        shape = HazloProductCardDefaults.containerShape,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HazloProductCardDefaults.imageHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                imageContent()

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp),
                ) {
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Icon(
                            imageVector = if (isFavorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = favoriteContentDescription,
                            tint = if (isFavorite) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                if (overlineLabel != null) {
                    Text(
                        text = overlineLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = price?.let(::formatHazloProductPrice) ?: priceFallbackLabel.orEmpty(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = distanceContentDescription,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(modifier = Modifier.size(2.dp))
                        Text(
                            text = distanceMeters?.let(::formatHazloProductDistance)
                                ?: HazloProductCardDefaults.unavailableDistanceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HazloProductCardImagePlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    val initials = remember(title) { hazloProductInitials(title) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.14f),
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                )
            }
        }
    }
}

fun formatHazloProductPrice(price: Double): String {
    val scaledPrice = (price * 100).roundToLong()
    val absoluteScaledPrice = scaledPrice.absoluteValue
    val wholePart = absoluteScaledPrice / 100
    val cents = absoluteScaledPrice % 100
    val sign = if (scaledPrice < 0) "-" else ""

    return "$sign$$wholePart.${cents.toString().padStart(length = 2, padChar = '0')}"
}

fun formatHazloProductDistance(distanceMeters: Double): String {
    val normalizedMeters = distanceMeters.coerceAtLeast(0.0)

    return if (normalizedMeters >= 1000.0) {
        val kilometersInTenths = (normalizedMeters / 100.0).roundToInt()
        val wholeKilometers = kilometersInTenths / 10
        val decimalKilometers = kilometersInTenths.absoluteValue % 10
        "$wholeKilometers.$decimalKilometers" + "km"
    } else {
        "${normalizedMeters.roundToInt()}m"
    }
}

fun hazloProductInitials(title: String): String {
    val normalizedWords = title
        .split(Regex("\\s+"))
        .map { word -> word.filter(Char::isLetterOrDigit) }
        .filter { word -> word.isNotBlank() }

    if (normalizedWords.isEmpty()) {
        return "--"
    }

    val firstLetters = normalizedWords
        .take(2)
        .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }

    return if (firstLetters.size == 2) {
        firstLetters.joinToString(separator = "")
    } else {
        normalizedWords.first().take(2).uppercase()
    }
}
