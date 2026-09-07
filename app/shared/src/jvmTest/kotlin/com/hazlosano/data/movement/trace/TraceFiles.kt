package com.hazlosano.data.movement.trace

import java.io.File

/**
 * Las trazas capturadas en un telefono real, para los arneses de calibracion.
 *
 * Viven fuera del repositorio (`traces/` esta en `.gitignore`) porque son capturas de salidas
 * concretas, con sus coordenadas. Los arneses que las leen se saltan en silencio donde no estan, asi
 * que ninguna maquina falla por no tenerlas.
 *
 * Estaban privados dentro de `TraceReplayHarness`; se movieron aqui al aparecer el segundo arnes.
 */
internal fun traceFiles(): List<File> =
    tracesDirectory().takeIf { it.isDirectory }
        ?.listFiles { file -> file.name.endsWith(".csv") }
        ?.filter { it.name.contains(onlyTrace ?: "") }
        ?.sortedBy { it.name }
        .orEmpty()

/**
 * Con que traza calibrar, cuando se quiere una sola.
 *
 * Calibrar es mirar una salida concreta y decidir sobre ella, asi que replayar las cinco para leer
 * una es ruido. Se pasa por propiedad de sistema:
 *
 *     .\gradlew.bat :app:shared:jvmTest --tests "*TraceReplayHarness" -Dtrace=1788749302780
 *
 * Sin ella se replayan todas, que es lo que hace falta al cambiar una constante del filtro.
 */
private val onlyTrace: String? get() = System.getProperty("trace")?.takeIf { it.isNotBlank() }

/** The repository root's `traces/`, reached from the module the test runs in. */
internal fun tracesDirectory(): File = File("../../traces")
