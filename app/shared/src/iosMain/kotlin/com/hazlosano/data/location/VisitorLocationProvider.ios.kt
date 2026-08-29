package com.hazlosano.data.location

import com.hazlosano.domain.model.VisitorLocation

/**
 * Pendiente, y declarado en vez de fingido.
 *
 * `CLLocationManager` da la última posición conocida sin encender el receptor, igual que el
 * proveedor de Android, pero exige la entrada de uso en el `Info.plist` del host de Xcode — que es
 * configuración del proyecto iOS, no código común. Hasta entonces el catálogo sale por fecha, que
 * es exactamente lo que hace cualquier target sin ubicación.
 */
actual suspend fun readVisitorLocation(): VisitorLocation? = null
