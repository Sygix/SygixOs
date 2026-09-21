package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem

interface HeroContentProvider {
    suspend fun load(): List<HeroItem>
}
