package dev.datlag.aniflow.anilist

import com.mayakapps.kache.InMemoryKache
import com.mayakapps.kache.KacheStrategy
import dev.datlag.aniflow.anilist.model.Character
import dev.datlag.aniflow.anilist.model.Medium
import dev.datlag.tooling.async.suspendCatching
import kotlin.time.Duration.Companion.hours

internal object Cache {
    private val trendingAnime = InMemoryKache<TrendingQuery, TrendingQuery.Data>(
        maxSize = 5L * 1024 * 1024,
    ) {
        strategy = KacheStrategy.LRU
        expireAfterWriteDuration = 2.hours
    }

    private val airing = InMemoryKache<AiringQuery, AiringQuery.Data>(
        maxSize = 5L * 1024 * 1024
    ) {
        strategy = KacheStrategy.LRU
        expireAfterWriteDuration = 1.hours
    }

    private val season = InMemoryKache<SeasonQuery, SeasonQuery.Data>(
        maxSize = 5L * 1024 * 1024
    ) {
        strategy = KacheStrategy.LRU
        expireAfterWriteDuration = 2.hours
    }

    private val medium = InMemoryKache<MediumQuery, Medium>(
        maxSize = 10L * 1024 * 1024
    ) {
        strategy = KacheStrategy.LRU
        expireAfterWriteDuration = 2.hours
    }

    private val character = InMemoryKache<CharacterQuery, Character>(
        maxSize = 5L * 1024 * 1024
    ) {
        strategy = KacheStrategy.LRU
        expireAfterWriteDuration = 2.hours
    }

    suspend fun getTrending(key: TrendingQuery): TrendingQuery.Data? {
        return suspendCatching {
            trendingAnime.getIfAvailable(key)
        }.getOrNull()
    }

    suspend fun setTrending(key: TrendingQuery, data: TrendingQuery.Data): TrendingQuery.Data {
        return suspendCatching {
            trendingAnime.put(key, data)
        }.getOrNull() ?: data
    }

    suspend fun getAiring(key: AiringQuery): AiringQuery.Data? {
        return suspendCatching {
            airing.getIfAvailable(key)
        }.getOrNull()
    }

    suspend fun setAiring(key: AiringQuery, data: AiringQuery.Data): AiringQuery.Data {
        return suspendCatching {
            airing.put(key, data)
        }.getOrNull() ?: data
    }

    suspend fun getSeason(key: SeasonQuery): SeasonQuery.Data? {
        return suspendCatching {
            season.getIfAvailable(key)
        }.getOrNull()
    }

    suspend fun setSeason(key: SeasonQuery, data: SeasonQuery.Data): SeasonQuery.Data {
        return suspendCatching {
            season.put(key, data)
        }.getOrNull() ?: data
    }

    suspend fun getMedium(key: MediumQuery): Medium? {
        return suspendCatching {
            medium.getIfAvailable(key)
        }.getOrNull()
    }

    suspend fun setMedium(key: MediumQuery, data: Medium): Medium {
        return suspendCatching {
            medium.put(key, data)
        }.getOrNull() ?: data
    }

    suspend fun getCharacter(key: CharacterQuery) : Character? {
        return suspendCatching {
            character.getIfAvailable(key)
        }.getOrNull()
    }

    suspend fun setCharacter(key: CharacterQuery, data: Character): Character {
        return suspendCatching {
            character.put(key, data)
        }.getOrNull() ?: data
    }
}