package com.hazlosano.domain.model

/**
 * Qué es una publicación del catálogo.
 *
 * Eje ortogonal al pilar: un producto de nutrición y un evento de movimiento son la misma clase de
 * cosa vista por dos ejes distintos.
 *
 * `key` es el valor que viaja por la API, en español porque así está en `posts.kind`. Se guarda
 * explícito en vez de derivarlo de `name.lowercase()` para que renombrar la constante de Kotlin no
 * rompa en silencio la lectura del catálogo.
 */
enum class PublicationKind(val key: String) {
    /** Contenido o aviso: no se vende ni ocurre. Es el que cae por omisión. */
    ANNOUNCEMENT("anuncio"),

    /** Algo que se entrega. Trae precio. */
    PRODUCT("producto"),

    /** Algo que ocurre: una rodada, un taller. Trae fechas; el precio es opcional. */
    EVENT("evento"),

    /** Algo que se hace: una consulta, un masaje. Trae precio y duración. */
    SERVICE("servicio"),
    ;

    companion object {
        /**
         * Un tipo desconocido cae en [ANNOUNCEMENT] en vez de descartar la publicación.
         *
         * `posts.kind` es `text` **sin `CHECK`** en la base, así que el sitio puede publicar un
         * tipo que esta versión del app no conoce. Esconder esa publicación sería peor que
         * pintarla sin las decoraciones del tipo: el título y la imagen siguen siendo válidos.
         */
        fun fromKey(key: String?): PublicationKind =
            entries.firstOrNull { it.key == key } ?: ANNOUNCEMENT
    }
}
