package com.hazlosano.data.repository

import com.hazlosano.domain.model.HazloChallenge
import com.hazlosano.domain.model.HazloChampion
import com.hazlosano.domain.model.PillarHighlights
import com.hazlosano.domain.model.PillarType

/**
 * Los campeones y retos de muestra de los cuatro pilares.
 *
 * Está aparte del repositorio porque es **dato**, no comportamiento: el repositorio se lee de una
 * ojeada y esta tabla crece o se borra sin tocarlo. Cuando el backend publique los suyos, este
 * archivo desaparece entero y la interfaz no se mueve.
 *
 * Los de Sueño son los que ya enseñaba `MockSleepRepository`: se mudaron aquí para que los cuatro
 * pilares lean de un solo sitio. Los otros tres vienen del proyecto Android de referencia.
 */
internal object SamplePillarHighlights {

    fun of(pillar: PillarType): PillarHighlights = when (pillar) {
        PillarType.MOVEMENT -> movement
        PillarType.NUTRITION -> nutrition
        PillarType.MIND -> mind
        PillarType.SLEEP -> sleep
    }

    private val movement = PillarHighlights(
        champions = listOf(
            champion("Mateo R.", "Top Trekker", "42.5 km", "Mateo", MOVEMENT_ACCENT),
            champion("Sofía L.", "Corredora", "12 hrs", "Sofia", MOVEMENT_ACCENT),
            champion("Carlos M.", "Caminante", "8.2 km", "Carlos", MOVEMENT_ACCENT),
        ),
        challenges = listOf(
            HazloChallenge(
                title = "Montañero 50k",
                description = "Conquista las cimas. Acumula 50 km de elevación en los próximos 30 días.",
                progressText = "12 km / 50 km",
                progress = 0.24f,
                imageUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?q=80&w=800",
            ),
            HazloChallenge(
                title = "Carrera al Amanecer",
                description = "Despierta con el bosque. Completa 5 carreras antes de las 8 AM.",
                progressText = "4 / 5 carreras",
                progress = 0.8f,
                imageUrl = "https://images.unsplash.com/photo-1533107862482-0e6974b06ec4?q=80&w=800",
            ),
        ),
    )

    private val nutrition = PillarHighlights(
        champions = listOf(
            champion("Mateo R.", "Top Chef", "12 recetas", "Mateo", NUTRITION_ACCENT),
            champion("Sofía L.", "Local Foodie", "25 lugares", "Sofia", NUTRITION_ACCENT),
        ),
        challenges = listOf(
            HazloChallenge(
                title = "Sin Azúcar Añadida",
                description = "Evita el azúcar procesada durante 7 días.",
                progressText = "5/7 días",
                progress = 0.7f,
                imageUrl = "https://images.unsplash.com/photo-1550081699-7a329be608be?q=80&w=800",
            ),
            HazloChallenge(
                title = "Consume 5 Colores",
                description = "Come frutas y verduras de 5 colores distintos hoy.",
                progressText = "3/5 colores",
                progress = 0.6f,
                imageUrl = "https://images.unsplash.com/photo-1490645935967-10de6ba17061?q=80&w=800",
            ),
        ),
    )

    private val mind = PillarHighlights(
        champions = listOf(
            champion("Ana V.", "Respiración diaria", "7 pausas", "AnaMind", MIND_ACCENT),
            champion("Luis G.", "Foco profundo", "180 min", "LuisMind", MIND_ACCENT),
            champion("Nora S.", "Diario emocional", "6 días", "NoraMind", MIND_ACCENT),
        ),
        challenges = listOf(
            HazloChallenge(
                title = "Respira 4-7-8",
                description = "Haz una pausa guiada de respiración antes de dormir.",
                progressText = "5/7 días",
                progress = 0.71f,
                imageUrl = "https://images.unsplash.com/photo-1499209974431-9dddcece7f88?q=80&w=800",
            ),
            HazloChallenge(
                title = "Diario de calma",
                description = "Registra una emoción y una acción de cuidado cada día.",
                progressText = "4/7 entradas",
                progress = 0.57f,
                imageUrl = "https://images.unsplash.com/photo-1455390582262-044cdead277a?q=80&w=800",
            ),
        ),
    )

    private val sleep = PillarHighlights(
        champions = listOf(
            champion("Valeria N.", "Rutina constante", "7 noches", "ValeriaSleep", SLEEP_ACCENT),
            champion("Diego P.", "Descanso profundo", "92 pts", "DiegoSleep", SLEEP_ACCENT),
            champion("Mara C.", "Sin café tarde", "5 días", "MaraSleep", SLEEP_ACCENT),
        ),
        challenges = listOf(
            HazloChallenge(
                title = "Apagar pantallas",
                description = "Cierra pantallas antes de dormir y protege tu descanso.",
                progressText = "4/7 noches",
                progress = 0.57f,
                imageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=800",
            ),
            HazloChallenge(
                title = "Hora fija",
                description = "Acuéstate dentro de una ventana estable de 30 minutos.",
                progressText = "5/7 noches",
                progress = 0.71f,
                imageUrl = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=800",
            ),
        ),
    )

    /** El avatar se genera con el acento del pilar, para que la fila se lea como parte del tablero. */
    private fun champion(
        name: String,
        title: String,
        stat: String,
        avatarSeed: String,
        accentHex: String,
    ) = HazloChampion(
        name = name,
        title = title,
        stat = stat,
        imageUrl = "https://api.dicebear.com/7.x/notionists/png?seed=$avatarSeed&backgroundColor=$accentHex",
    )
}

private const val MOVEMENT_ACCENT = "538f39"
private const val NUTRITION_ACCENT = "f0380e"
private const val MIND_ACCENT = "38bdf8"
private const val SLEEP_ACCENT = "8b5cf6"
