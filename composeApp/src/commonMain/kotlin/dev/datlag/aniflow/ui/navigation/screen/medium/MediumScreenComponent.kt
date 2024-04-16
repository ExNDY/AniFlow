package dev.datlag.aniflow.ui.navigation.screen.medium

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Optional
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import dev.chrisbanes.haze.HazeState
import dev.datlag.aniflow.LocalHaze
import dev.datlag.aniflow.anilist.MediumQuery
import dev.datlag.aniflow.anilist.MediumStateMachine
import dev.datlag.aniflow.anilist.RatingMutation
import dev.datlag.aniflow.anilist.model.Medium
import dev.datlag.aniflow.anilist.type.MediaFormat
import dev.datlag.aniflow.anilist.type.MediaStatus
import dev.datlag.aniflow.common.nullableFirebaseInstance
import dev.datlag.aniflow.common.onRenderApplyCommonScheme
import dev.datlag.aniflow.common.popular
import dev.datlag.aniflow.common.rated
import dev.datlag.aniflow.model.saveFirstOrNull
import dev.datlag.aniflow.other.Constants
import dev.datlag.aniflow.other.TokenRefreshHandler
import dev.datlag.aniflow.settings.Settings
import dev.datlag.tooling.async.suspendCatching
import dev.datlag.tooling.compose.ioDispatcher
import dev.datlag.tooling.compose.withMainContext
import dev.datlag.tooling.decompose.ioScope
import dev.datlag.tooling.safeCast
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.kodein.di.DI
import org.kodein.di.instance
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory

class MediumScreenComponent(
    componentContext: ComponentContext,
    override val di: DI,
    override val initialMedium: Medium,
    private val onBack: () -> Unit
) : MediumComponent, ComponentContext by componentContext {

    private val aniListClient by di.instance<ApolloClient>(Constants.AniList.APOLLO_CLIENT)
    private val aniListFallbackClient by di.instance<ApolloClient>(Constants.AniList.FALLBACK_APOLLO_CLIENT)
    private val mediumStateMachine = MediumStateMachine(
        client = aniListClient,
        fallbackClient = aniListFallbackClient,
        crashlytics = di.nullableFirebaseInstance()?.crashlytics,
        id = initialMedium.id
    )
    private val mediumState = mediumStateMachine.state.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = mediumStateMachine.currentState
    )
    private val mediumSuccessState = mediumState.mapNotNull { it.safeCast<MediumStateMachine.State.Success>() }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    override val bannerImage: StateFlow<String?> = mediumSuccessState.mapNotNull {
        it?.data?.bannerImage
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = initialMedium.bannerImage
    )

    override val coverImage: StateFlow<Medium.CoverImage> = mediumSuccessState.mapNotNull {
        it?.data?.coverImage
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = initialMedium.coverImage
    )

    override val title: StateFlow<Medium.Title> = mediumSuccessState.mapNotNull {
        it?.data?.title
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = initialMedium.title
    )

    override val description: StateFlow<String?> = mediumSuccessState.map {
        it?.data?.description?.ifBlank { null }
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    override val translatedDescription: MutableStateFlow<String?> = MutableStateFlow(null)

    override val genres: StateFlow<Set<String>> = mediumSuccessState.mapNotNull {
        it?.data?.genres?.ifEmpty { null }
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = initialMedium.genres
    )

    override val format: StateFlow<MediaFormat> = mediumSuccessState.mapNotNull {
        it?.data?.format
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = MediaFormat.UNKNOWN__
    )

    private val nextAiringEpisode: StateFlow<MediumQuery.NextAiringEpisode?> = mediumSuccessState.mapNotNull {
        it?.data?.nextAiringEpisode
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    override val episodes: StateFlow<Int> = combine(
        mediumSuccessState.mapNotNull {
            it?.data?.episodes
        },
        nextAiringEpisode
    ) { episodes, airing ->
        if (episodes > -1) {
            episodes
        } else if (airing != null) {
            if (Instant.fromEpochSeconds(airing.airingAt.toLong()) <= Clock.System.now()) {
                airing.episode
            } else {
                airing.episode - 1
            }
        } else {
            episodes
        }
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = run {
            val airing = nextAiringEpisode.value ?: return@run -1

            if (Instant.fromEpochSeconds(airing.airingAt.toLong()) <= Clock.System.now()) {
                airing.episode
            } else {
                airing.episode - 1
            }
        }
    )

    override val duration: StateFlow<Int> = mediumSuccessState.mapNotNull {
        it?.data?.avgEpisodeDurationInMin
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = -1
    )

    override val status: StateFlow<MediaStatus> = mediumSuccessState.mapNotNull {
        it?.data?.status
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = MediaStatus.UNKNOWN__
    )

    override val rated: StateFlow<Medium.Ranking?> = mediumSuccessState.mapNotNull {
        it?.data?.rated()
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    override val popular: StateFlow<Medium.Ranking?> = mediumSuccessState.mapNotNull {
        it?.data?.popular()
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    override val score: StateFlow<Int?> = mediumSuccessState.mapNotNull {
        val received = it?.data?.averageScore
        if (received == null || received == -1) {
            null
        } else {
            received
        }
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = run {
            val initial = initialMedium.averageScore

            if (initial == -1) {
                null
            } else {
                initial
            }
        }
    )

    override val characters: StateFlow<Set<Medium.Character>> = mediumSuccessState.mapNotNull {
        it?.data?.characters
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = emptySet()
    )

    private val mediaId: StateFlow<Int> = mediumSuccessState.mapNotNull {
        it?.data?.id
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = initialMedium.id
    )

    private val changedRating: MutableStateFlow<Int> = MutableStateFlow(-1)
    override val rating: StateFlow<Int> = combine(
        mediumSuccessState.mapNotNull {
            it?.data?.entry?.score?.toInt()
        },
        changedRating
    ) { t1, t2 ->
        if (t2 > -1) {
            t2
        } else {
            t1
        }
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = changedRating.value
    )

    override val trailer: StateFlow<Medium.Full.Trailer?> = mediumSuccessState.mapNotNull {
        it?.data?.trailer
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = null
    )

    override val alreadyAdded: StateFlow<Boolean> = mediumSuccessState.mapNotNull {
        it?.data?.entry != null
    }.flowOn(
        context = ioDispatcher()
    ).stateIn(
        scope = ioScope(),
        started = SharingStarted.WhileSubscribed(),
        initialValue = false
    )

    private val userSettings by di.instance<Settings.PlatformUserSettings>()
    private val apolloClient by di.instance<ApolloClient>(Constants.AniList.APOLLO_CLIENT)

    @Composable
    override fun render() {
        val state = HazeState()

        CompositionLocalProvider(
            LocalHaze provides state
        ) {
            onRenderApplyCommonScheme(initialMedium.id) {
                MediumScreen(this)
            }
        }
    }

    override fun back() {
        onBack()
    }

    private suspend fun login(): Boolean {
        val factory by di.instance<CodeAuthFlowFactory>()
        val client by di.instance<OpenIdConnectClient>(Constants.AniList.Auth.CLIENT)
        val flow = withMainContext {
            factory.createAuthFlow(client)
        }

        val tokenResult = suspendCatching {
            flow.getAccessToken()
        }

        tokenResult.getOrNull()?.let {
            userSettings.setAniListTokens(
                access = it.access_token,
                refresh = it.refresh_token,
                id = it.id_token
            )
        }

        return tokenResult.isSuccess
    }

    override fun rate(onLoggedIn: () -> Unit) {
        launchIO {
            val isLoggedIn = userSettings.isAniListLoggedIn.saveFirstOrNull() ?: false

            if (!isLoggedIn) {
                if (login()) {
                    withMainContext {
                        onLoggedIn()
                    }
                }
            } else {
                withMainContext {
                    onLoggedIn()
                }
            }
        }
    }

    override fun rate(value: Int) {
        val mutation = RatingMutation(
            mediaId = Optional.present(mediaId.value),
            rating = Optional.present(value * 20)
        )
        launchIO {
            apolloClient.mutation(mutation).execute().data?.SaveMediaListEntry?.score?.let {
                changedRating.emit(it.toInt())
            }
        }
    }

    override fun descriptionTranslation(text: String?) {
        launchIO {
            translatedDescription.emit(text)
        }
    }
}