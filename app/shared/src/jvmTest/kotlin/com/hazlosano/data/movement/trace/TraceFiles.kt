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
        ?.sortedBy { it.name }
        .orEmpty()

/** The repository root's `traces/`, reached from the module the test runs in. */
internal fun tracesDirectory(): File = File("../../traces")
