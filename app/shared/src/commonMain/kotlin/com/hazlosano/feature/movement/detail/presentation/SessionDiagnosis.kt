package com.hazlosano.feature.movement.detail.presentation

import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.filter.TraceSummary
import com.hazlosano.feature.movement.presentation.MovementFormat
import kotlin.math.roundToInt

/** One line of the diagnosis, already formatted. */
data class DiagnosisRow(val label: String, val value: String)

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
        add(DiagnosisRow("Lecturas recibidas", readings.toString()))
        add(DiagnosisRow("Aceptadas", withShare(accepted)))
        add(DiagnosisRow("Descartadas", withShare(discarded)))
        discardedBy.entries
            .sortedByDescending { it.value }
            .forEach { (reason, count) ->
                add(DiagnosisRow("· ${reason.label()}", count.toString()))
            }
        add(DiagnosisRow("Precisión media", "${MovementFormat.oneDecimal(averageAccuracyMeters)} m"))
        add(DiagnosisRow("Intervalo real", "${MovementFormat.oneDecimal(samplingIntervalSeconds)} s"))
    },
)

/** The share matters more than the count: 94 rejected readings mean nothing without the total. */
private fun TraceSummary.withShare(count: Int): String {
    if (readings == 0) return count.toString()
    return "$count · ${(count * 100.0 / readings).roundToInt()} %"
}

private fun DiscardReason.label(): String = when (this) {
    DiscardReason.POOR_ACCURACY -> "Precisión insuficiente"
    DiscardReason.IMPLAUSIBLE_SPEED -> "Salto imposible"
    DiscardReason.WITHIN_NOISE -> "Bajo el ruido"
    DiscardReason.UNCONFIRMED_MOVEMENT -> "Movimiento sin confirmar"
}
