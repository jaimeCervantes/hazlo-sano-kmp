package com.hazlosano.core.ui

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Los dos catálogos de cadenas, comparados clave por clave.
 *
 * Es la única prueba del proyecto que lee el árbol de fuentes, y tiene su motivo: el catálogo en
 * inglés se quedó en **54 cadenas contra 121** durante meses sin que nada fallara, porque una clave
 * que falta no rompe la compilación — Compose Resources cae en el idioma por defecto y la pantalla
 * sale en español a medias. El fallo sólo se ve mirando la app en inglés, y por eso nadie lo veía.
 *
 * También comprueba los marcadores de formato: `%1$s` de más o de menos en una traducción revienta
 * en tiempo de ejecución, y sólo en el idioma que nadie estaba probando.
 */
class StringCatalogTest {

    private val catalogs = File("src/commonMain/composeResources")
    private val spanish = catalogs.resolve("values/strings.xml")
    private val english = catalogs.resolve("values-en/strings.xml")

    private val entry = Regex("""<string name="([^"]+)">(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
    private val placeholder = Regex("""%(\d+)\$[a-zA-Z]""")

    private fun entriesOf(file: File): Map<String, String> {
        assertTrue(file.isFile, "no se encontró ${file.absolutePath}")
        return entry.findAll(file.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test
    fun `english covers every key spanish has`() {
        val missing = entriesOf(spanish).keys - entriesOf(english).keys

        assertTrue(
            missing.isEmpty(),
            "${missing.size} cadenas sin traducir al inglés; la pantalla saldría en español a " +
                "medias: ${missing.sorted()}",
        )
    }

    /**
     * Al revés también importa: una clave que sólo existe en inglés es una que se borró del español
     * sin barrer, o una que se escribió mal. Las dos acaban en un recurso que nadie lee.
     */
    @Test
    fun `english has no key spanish does not`() {
        val orphans = entriesOf(english).keys - entriesOf(spanish).keys

        assertTrue(orphans.isEmpty(), "claves huérfanas en el catálogo en inglés: ${orphans.sorted()}")
    }

    @Test
    fun `both catalogues agree on every placeholder`() {
        val spanishEntries = entriesOf(spanish)
        val englishEntries = entriesOf(english)

        for ((key, spanishValue) in spanishEntries) {
            val englishValue = englishEntries[key] ?: continue
            assertEquals(
                placeholder.findAll(spanishValue).map { it.value }.toSet(),
                placeholder.findAll(englishValue).map { it.value }.toSet(),
                "«$key» no lleva los mismos marcadores en los dos idiomas, así que reventaría al " +
                    "formatearse en uno de ellos",
            )
        }
    }

    /**
     * Una clave declarada dos veces en el mismo archivo gana la última en silencio, y la primera
     * —la que alguien está leyendo al buscar por qué el texto no cambia— deja de valer.
     */
    @Test
    fun `no catalogue declares the same key twice`() {
        for (file in listOf(spanish, english)) {
            val keys = entry.findAll(file.readText()).map { it.groupValues[1] }.toList()
            val duplicated = keys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

            assertTrue(duplicated.isEmpty(), "${file.name} declara dos veces: ${duplicated.sorted()}")
        }
    }
}
