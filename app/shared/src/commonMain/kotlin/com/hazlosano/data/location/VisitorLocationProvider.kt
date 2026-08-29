package com.hazlosano.data.location

import com.hazlosano.domain.model.VisitorLocation

/**
 * Desde dónde se está mirando el catálogo, **sin encender el GPS**.
 *
 * Devuelve la última posición que el sistema ya conocía. Para ordenar publicaciones por cercanía,
 * una posición de hace un rato vale igual que una recién medida, y pedir un fix nuevo cada vez que
 * se abre una pestaña costaría segundos de espera y batería para afinar una distancia que se pinta
 * redondeada.
 *
 * `null` no es un fallo: es un escritorio sin GPS, un permiso no concedido o un teléfono que
 * todavía no se ha ubicado. El catálogo entonces sale ordenado por fecha, que es lo que el sitio
 * hace con cualquier visitante que no comparte dónde está.
 */
expect suspend fun readVisitorLocation(): VisitorLocation?
