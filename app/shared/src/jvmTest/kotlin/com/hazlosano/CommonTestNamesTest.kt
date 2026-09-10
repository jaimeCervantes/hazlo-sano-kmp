package com.hazlosano

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Los nombres de prueba de `commonTest` tienen que ser legales en **todos** los targets.
 *
 * Kotlin/Native rechaza la coma dentro de un nombre entre acentos graves, y `commonTest` se compila
 * también para iOS. Es un error trivial de cometer —una coma en una frase en español es lo natural—
 * y **ya se ha cometido cuatro veces** en este proyecto.
 *
 * Lo que hace insoportable ese error no es el error: es dónde se caza. Sólo aparece en
 * `compileTestKotlinIosSimulatorArm64`, que corre dentro de `:app:shared:check` y tarda varios
 * minutos, así que se descubre al final de la validación y obliga a repetirla entera. Esta prueba lo
 * caza en `:app:shared:jvmTest`, que son segundos y es lo primero que se corre.
 *
 * Mira las fuentes en vez de los nombres compilados a propósito: lo que hay que revisar es lo que
 * está escrito, y en la JVM esos nombres son perfectamente legales — no hay nada que observar en
 * tiempo de ejecución.
 */
class CommonTestNamesTest {

    @Test
    fun `no common test name carries a character Kotlin Native rejects`() {
        val offenders = commonTestSources()
            .flatMap { file -> file.backtickedTestNames().map { file.name to it } }
            .filter { (_, name) -> name.any { it in ILLEGAL_IN_NATIVE } }

        assertTrue(
            offenders.isEmpty(),
            buildString {
                appendLine("Kotlin/Native no admite estos caracteres en un nombre de prueba:")
                appendLine("  ${ILLEGAL_IN_NATIVE.joinToString(" ") { "«$it»" }}")
                appendLine("Compilarían en la JVM y fallarían en iOS, al final de :app:shared:check.")
                offenders.forEach { (file, name) -> appendLine("  $file → `$name`") }
            },
        )
    }

    /** Se salta en silencio si no encuentra las fuentes: un arnés que no puede mirar no acusa. */
    private fun commonTestSources(): List<File> = listOf(
        File("src/commonTest/kotlin"),
        File("../../core/src/commonTest/kotlin"),
    ).filter { it.isDirectory }
        .flatMap { it.walkTopDown().filter { file -> file.extension == "kt" }.toList() }

    private fun File.backtickedTestNames(): List<String> =
        TEST_NAME.findAll(readText()).map { it.groupValues[1] }.toList()

    private companion object {
        val TEST_NAME = Regex("""fun `([^`]*)`""")

        /**
         * Sólo la coma, porque es la única que este proyecto ha pisado y la única cuya ausencia se ha
         * comprobado. Añadir aquí una lista larga sacada de la documentación sería afirmar cosas que
         * nadie ha verificado, que es justo lo que este repositorio evita en sus cifras.
         */
        val ILLEGAL_IN_NATIVE = listOf(',')
    }
}
