package com.hazlosano.data.repository

import com.hazlosano.domain.model.HazloChallenge
import com.hazlosano.domain.model.HazloChampion
import com.hazlosano.domain.model.HazloProduct
import com.hazlosano.domain.model.SleepContent
import com.hazlosano.domain.repository.SleepRepository

class MockSleepRepository : SleepRepository {

    override suspend fun getSleepContent(): SleepContent = SleepContent(
        heroTitle = "Sueño reparador",
        heroSubtitle = "Cuida tus noches con retos medibles, comunidad y apoyo cercano.",
        heroMetricLabel = "Rutina semanal",
        heroMetricValue = "5/7",
        heroMetricSupport = "noches con horario estable",
        heroProgress = 0.71f,
        heroImageUrl = "https://images.unsplash.com/photo-1540518614846-7eded433c457?q=80&w=1200",
        weeklyChampions = listOf(
            HazloChampion(
                name = "Valeria N.",
                title = "Rutina constante",
                stat = "7 noches",
                imageUrl = "https://api.dicebear.com/7.x/notionists/png?seed=ValeriaSleep&backgroundColor=8b5cf6",
            ),
            HazloChampion(
                name = "Diego P.",
                title = "Descanso profundo",
                stat = "92 pts",
                imageUrl = "https://api.dicebear.com/7.x/notionists/png?seed=DiegoSleep&backgroundColor=8b5cf6",
            ),
            HazloChampion(
                name = "Mara C.",
                title = "Sin cafe tarde",
                stat = "5 dias",
                imageUrl = "https://api.dicebear.com/7.x/notionists/png?seed=MaraSleep&backgroundColor=8b5cf6",
            ),
        ),
        activeChallenges = listOf(
            HazloChallenge(
                title = "Apagar pantallas",
                description = "Cierra pantallas antes de dormir y protege tu descanso.",
                progressText = "4/7 noches",
                progress = 0.57f,
                imageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?q=80&w=800",
            ),
            HazloChallenge(
                title = "Hora fija",
                description = "Acuestate dentro de una ventana estable de 30 minutos.",
                progressText = "5/7 noches",
                progress = 0.71f,
                imageUrl = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?q=80&w=800",
            ),
        ),
    )
}
