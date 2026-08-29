package com.hazlosano.data.location

import com.hazlosano.domain.model.VisitorLocation

/**
 * El navegador sí sabría, pero preguntarlo abre un permiso del sistema nada más entrar.
 *
 * En web el sitio ya resuelve la ubicación por su cuenta con su propia cookie, así que pedirla otra
 * vez desde aquí sería un segundo diálogo para el mismo dato. Se deja sin implementar a propósito.
 */
actual suspend fun readVisitorLocation(): VisitorLocation? = null
