package com.hazlosano.data.location

import com.hazlosano.domain.model.VisitorLocation

/**
 * Un escritorio no sabe dónde está, y adivinarlo por IP mentiría con confianza.
 *
 * El catálogo sale ordenado por fecha, igual que para cualquier visitante del sitio que no comparte
 * su ubicación.
 */
actual suspend fun readVisitorLocation(): VisitorLocation? = null
