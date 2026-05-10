package com.hazlosano.kmp.data.repository

import com.hazlosano.kmp.domain.model.FeedPost
import com.hazlosano.kmp.domain.model.HazloChampion
import com.hazlosano.kmp.domain.model.HomeContent
import com.hazlosano.kmp.domain.model.PillarAction
import com.hazlosano.kmp.domain.model.PillarOverview
import com.hazlosano.kmp.domain.model.PillarType
import com.hazlosano.kmp.domain.repository.HomeRepository

class MockHomeRepository : HomeRepository {

    override suspend fun getHomeContent(): HomeContent = HomeContent(
        headerTitle = "Tu Ecosistema",
        headerSubtitle = "Cultiva tus 4 pilares hoy",
        pillars = listOf(
            PillarOverview(
                pillarType = PillarType.MOVEMENT,
                title = "Movimiento",
                stat = "Recorrer Rutas",
                subtitle = "Inicia tu sesión de tracking",
                imageUrl = "https://images.unsplash.com/photo-1473448912268-2022ce9509d8?q=80&w=800",
                action = PillarAction.TRACKER,
            ),
            PillarOverview(
                pillarType = PillarType.NUTRITION,
                title = "Nutrición",
                stat = "",
                subtitle = "Receta del día",
                imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=400",
            ),
            PillarOverview(
                pillarType = PillarType.MIND,
                title = "Mente",
                stat = "",
                subtitle = "10 min hoy",
                imageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?q=80&w=400",
            ),
        ),
        champions = listOf(
            HazloChampion(
                name = "Ana",
                title = "Sueño",
                stat = "42 hrs",
                imageUrl = "https://api.dicebear.com/7.x/notionists/png?seed=Ana&backgroundColor=transparent",
            ),
            HazloChampion(
                name = "Carlos",
                title = "Movimiento",
                stat = "50 km",
                imageUrl = "https://api.dicebear.com/7.x/notionists/png?seed=Carlos&backgroundColor=transparent",
            ),
            HazloChampion(
                name = "Elena",
                title = "Nutrición",
                stat = "7 Días",
                imageUrl = "https://api.dicebear.com/7.x/notionists/png?seed=Elena&backgroundColor=transparent",
            ),
            HazloChampion(
                name = "Luis",
                title = "Mente",
                stat = "120 min",
                imageUrl = "https://api.dicebear.com/7.x/notionists/png?seed=Luis&backgroundColor=transparent",
            ),
        ),
        feedPosts = listOf(
            FeedPost(
                userName = "Ana M.",
                timeAgo = "Hace 2 horas",
                avatarSeed = "Ana",
                pillarType = PillarType.SLEEP,
                content = "¡Por fin logré mi racha de 7 días durmiendo 8 horas seguidas! Me siento con una energía increíble para arrancar la semana.",
                imageUrl = "https://images.unsplash.com/photo-1541781774459-bb2af2f05b55?q=80&w=800",
                likes = 24,
                comments = 5,
            ),
            FeedPost(
                userName = "David C.",
                timeAgo = "Hace 5 horas",
                avatarSeed = "David",
                pillarType = PillarType.MOVEMENT,
                content = "Día espectacular para un trail running en la montaña. 15km superados, ¡mis pulmones lo agradecen!",
                imageUrl = "https://images.unsplash.com/photo-1551632811-561732d1e306?q=80&w=800",
                likes = 56,
                comments = 12,
            ),
            FeedPost(
                userName = "Elena R.",
                timeAgo = "Ayer",
                avatarSeed = "Elena",
                pillarType = PillarType.NUTRITION,
                content = "Preparando mi bowl regenerativo con quinoa, aguacate y semillas de cáñamo. Alimentando el cuerpo con la tierra.",
                imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=800",
                likes = 38,
                comments = 8,
            ),
        ),
    )
}
