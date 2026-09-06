package com.hazlosano.feature.movement.ui

import androidx.compose.runtime.Composable
import com.hazlosano.domain.feature.movement.filter.DiscardReason
import com.hazlosano.domain.feature.movement.model.RouteProblem
import com.hazlosano.feature.movement.detail.presentation.DiagnosisLabel
import com.hazlosano.feature.movement.detail.presentation.SaveRouteMessage
import com.hazlosano.feature.movement.detail.presentation.SessionMetric
import com.hazlosano.feature.movement.routes.presentation.RoutesMessage
import hazlosano.app.shared.generated.resources.Res
import hazlosano.app.shared.generated.resources.diagnosis_accepted
import hazlosano.app.shared.generated.resources.diagnosis_average_accuracy
import hazlosano.app.shared.generated.resources.diagnosis_discarded
import hazlosano.app.shared.generated.resources.diagnosis_readings
import hazlosano.app.shared.generated.resources.diagnosis_reason_implausible_speed
import hazlosano.app.shared.generated.resources.diagnosis_reason_poor_accuracy
import hazlosano.app.shared.generated.resources.diagnosis_reason_unconfirmed_movement
import hazlosano.app.shared.generated.resources.diagnosis_reason_within_noise
import hazlosano.app.shared.generated.resources.diagnosis_sampling_interval
import hazlosano.app.shared.generated.resources.movement_metric_ascent
import hazlosano.app.shared.generated.resources.movement_metric_descent
import hazlosano.app.shared.generated.resources.movement_metric_distance
import hazlosano.app.shared.generated.resources.movement_metric_duration
import hazlosano.app.shared.generated.resources.movement_metric_max_altitude
import hazlosano.app.shared.generated.resources.movement_metric_min_altitude
import hazlosano.app.shared.generated.resources.movement_metric_moving_time
import hazlosano.app.shared.generated.resources.movement_metric_pace
import hazlosano.app.shared.generated.resources.route_problem_name_required
import hazlosano.app.shared.generated.resources.route_problem_no_points
import hazlosano.app.shared.generated.resources.route_problem_not_enough_points
import hazlosano.app.shared.generated.resources.route_problem_not_found
import hazlosano.app.shared.generated.resources.route_problem_unreadable_gpx
import hazlosano.app.shared.generated.resources.routes_message_deleted
import hazlosano.app.shared.generated.resources.routes_message_imported
import hazlosano.app.shared.generated.resources.routes_message_renamed
import hazlosano.app.shared.generated.resources.routes_message_replace_failed
import hazlosano.app.shared.generated.resources.routes_message_replaced
import hazlosano.app.shared.generated.resources.routes_message_saved
import org.jetbrains.compose.resources.stringResource

/*
 * Con qué palabras se cuenta lo que el pilar de Movimiento tiene que decir.
 *
 * Este archivo es la mitad de UI de una separación que empieza en `core`: los casos de uso devuelven
 * un `RouteProblem`, los ViewModels devuelven un mensaje cerrado, y la traducción a español —o a lo
 * que se elija en ajustes— ocurre **aquí**, que es la única capa que puede leer recursos.
 *
 * Está en un solo sitio a propósito. El mismo `RouteProblem` lo enseñan la pantalla de rutas y la de
 * detalle de una salida, y sin esto cada una tendría su propia redacción del mismo fallo.
 */

@Composable
fun RouteProblem.text(): String = stringResource(
    when (this) {
        RouteProblem.UNREADABLE_GPX -> Res.string.route_problem_unreadable_gpx
        RouteProblem.ROUTE_NOT_FOUND -> Res.string.route_problem_not_found
        RouteProblem.ROUTE_HAS_NO_POINTS -> Res.string.route_problem_no_points
        RouteProblem.NAME_REQUIRED -> Res.string.route_problem_name_required
        RouteProblem.NOT_ENOUGH_POINTS -> Res.string.route_problem_not_enough_points
    },
)

@Composable
fun RoutesMessage.text(): String = when (this) {
    is RoutesMessage.Imported -> stringResource(Res.string.routes_message_imported, routeName)
    is RoutesMessage.Replaced -> stringResource(Res.string.routes_message_replaced, routeName)
    RoutesMessage.Renamed -> stringResource(Res.string.routes_message_renamed)
    RoutesMessage.Deleted -> stringResource(Res.string.routes_message_deleted)
    RoutesMessage.NameRequired -> stringResource(Res.string.route_problem_name_required)
    RoutesMessage.ReplaceFailed -> stringResource(Res.string.routes_message_replace_failed)
    is RoutesMessage.Failed -> problem.text()
}

@Composable
fun SaveRouteMessage.text(): String = when (this) {
    is SaveRouteMessage.Saved -> stringResource(Res.string.routes_message_saved, routeName)
    is SaveRouteMessage.Failed -> problem.text()
}

@Composable
fun SessionMetric.label(): String = stringResource(
    when (this) {
        SessionMetric.DISTANCE -> Res.string.movement_metric_distance
        SessionMetric.DURATION -> Res.string.movement_metric_duration
        SessionMetric.MOVING_TIME -> Res.string.movement_metric_moving_time
        SessionMetric.PACE -> Res.string.movement_metric_pace
        SessionMetric.ASCENT -> Res.string.movement_metric_ascent
        SessionMetric.DESCENT -> Res.string.movement_metric_descent
        SessionMetric.MAX_ALTITUDE -> Res.string.movement_metric_max_altitude
        SessionMetric.MIN_ALTITUDE -> Res.string.movement_metric_min_altitude
    },
)

@Composable
fun DiagnosisLabel.text(): String = stringResource(
    when (this) {
        DiagnosisLabel.Readings -> Res.string.diagnosis_readings
        DiagnosisLabel.Accepted -> Res.string.diagnosis_accepted
        DiagnosisLabel.Discarded -> Res.string.diagnosis_discarded
        DiagnosisLabel.AverageAccuracy -> Res.string.diagnosis_average_accuracy
        DiagnosisLabel.SamplingInterval -> Res.string.diagnosis_sampling_interval
        is DiagnosisLabel.DiscardedBy -> when (reason) {
            DiscardReason.POOR_ACCURACY -> Res.string.diagnosis_reason_poor_accuracy
            DiscardReason.IMPLAUSIBLE_SPEED -> Res.string.diagnosis_reason_implausible_speed
            DiscardReason.WITHIN_NOISE -> Res.string.diagnosis_reason_within_noise
            DiscardReason.UNCONFIRMED_MOVEMENT -> Res.string.diagnosis_reason_unconfirmed_movement
        }
    },
)
