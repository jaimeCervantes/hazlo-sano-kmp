package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.filter.TraceSummary
import com.hazlosano.feature.movement.presentation.MovementFormat
import kotlin.math.roundToInt

/**
 * De qué habla una línea del diagnóstico.
 *
 * Es un tipo cerrado y no un `String` porque el rótulo es copia: escrito aquí, ninguna traducción lo
 * alcanzaría. [DiscardedBy] lleva dentro el motivo del filtro —que ya es un tipo cerrado de `core`—
 * en vez de su nombre en español.
 */
sealed interface DiagnosisLabel {
    data object Readings : DiagnosisLabel
    data object Accepted : DiagnosisLabel
    data object Discarded : DiagnosisLabel
    data class DiscardedBy(val reason: DiscardReason) : DiagnosisLabel
    data object AverageAccuracy : DiagnosisLabel
    data object SamplingInterval : DiagnosisLabel
}

/** Una línea del diagnóstico: de qué habla, y la cifra ya formateada. */
data class DiagnosisRow(val label: DiagnosisLabel, val value: String)

/**
 * What the filter did during a recording, as rows ready to render.
 *
 * Built as rows rather than as named fields because the interesting part is which reasons fired at
 * all: a session that rejected nothing for a given reason simply has no line for it, which reads
 * better than a column of zeros and is honest about a reason never having applied.
 */
data class SessionDiagnosisUi(val rows: List<DiagnosisRow>)

fun TraceSummary.toDiagnosisUi(): SessionDiagnosisUi = SessionDiagnosisUi(
    buildList {
        add(DiagnosisRow(DiagnosisLabel.Readings, readings.toString()))
        add(DiagnosisRow(DiagnosisLabel.Accepted, withShare(accepted)))
        add(DiagnosisRow(DiagnosisLabel.Discarded, withShare(discarded)))
        discardedBy.entries
            .sortedByDescending { it.value }
            .forEach { (reason, count) ->
                add(DiagnosisRow(DiagnosisLabel.DiscardedBy(reason), count.toString()))
            }
        add(
            DiagnosisRow(
                DiagnosisLabel.AverageAccuracy,
                "${MovementFormat.oneDecimal(averageAccuracyMeters)} m",
            ),
        )
        add(
            DiagnosisRow(
                DiagnosisLabel.SamplingInterval,
                "${MovementFormat.oneDecimal(samplingIntervalSeconds)} s",
            ),
        )
    },
)

/** The share matters more than the count: 94 rejected readings mean nothing without the total. */
private fun TraceSummary.withShare(count: Int): String {
    if (readings == 0) return count.toString()
    return "$count · ${(count * 100.0 / readings).roundToInt()} %"
}
