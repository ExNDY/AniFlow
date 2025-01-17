package dev.datlag.aniflow.ui.navigation.screen.medium

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.maxkeppeker.sheets.core.models.base.Header
import com.maxkeppeker.sheets.core.models.base.IconSource
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.rating.RatingDialog
import com.maxkeppeler.sheets.rating.models.RatingBody
import com.maxkeppeler.sheets.rating.models.RatingConfig
import com.maxkeppeler.sheets.rating.models.RatingSelection
import com.maxkeppeler.sheets.rating.models.RatingViewStyle
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.datlag.aniflow.LocalHaze
import dev.datlag.aniflow.LocalPaddingValues
import dev.datlag.aniflow.SharedRes
import dev.datlag.aniflow.anilist.type.MediaStatus
import dev.datlag.aniflow.common.*
import dev.datlag.aniflow.other.StateSaver
import dev.datlag.aniflow.ui.custom.EditFAB
import dev.datlag.aniflow.ui.navigation.screen.initial.home.component.GenreChip
import dev.datlag.aniflow.ui.navigation.screen.medium.component.*
import dev.datlag.tooling.compose.ifTrue
import dev.datlag.tooling.compose.onClick
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import dev.icerock.moko.resources.compose.painterResource
import dev.icerock.moko.resources.compose.stringResource
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.map
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun MediumScreen(component: MediumComponent) {
    val appBarState = rememberTopAppBarState()
    val scrollState = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = appBarState
    )
    val coverImage by component.coverImage.collectAsStateWithLifecycle()
    val ratingState = rememberUseCaseState()
    val userRating by component.rating.collectAsStateWithLifecycle()
    val dialogState by component.dialog.subscribeAsState()
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = StateSaver.List.mediaOverview,
        initialFirstVisibleItemScrollOffset = StateSaver.List.mediaOverviewOffset
    )

    dialogState.child?.instance?.render()

    RatingDialog(
        state = ratingState,
        selection = RatingSelection(
            onSelectRating = { rating, _ ->
                component.rate(rating)
            }
        ),
        header = Header.Default(
            title = "Rate this Anime",
            icon = IconSource(Icons.Filled.Star)
        ),
        body = RatingBody.Default(
            bodyText = ""
        ),
        config = RatingConfig(
            ratingOptionsCount = 5,
            ratingOptionsSelected = userRating.takeIf { it > 0 },
            ratingZeroValid = true
        )
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollState.nestedScrollConnection),
        topBar = {
            CollapsingToolbar(
                state = appBarState,
                scrollBehavior = scrollState,
                mediumStateFlow = component.mediumState,
                bannerImageFlow = component.bannerImage,
                coverImage = coverImage,
                titleFlow = component.title,
                isFavoriteFlow = component.isFavorite,
                isFavoriteBlockedFlow = component.isFavoriteBlocked,
                onBack = { component.back() },
                onToggleFavorite = { component.toggleFavorite() }
            )
        },
        floatingActionButton = {
            val alreadyAdded by component.alreadyAdded.collectAsStateWithLifecycle()
            val notReleased by component.status.mapCollect {
                it == MediaStatus.UNKNOWN__ || it == MediaStatus.NOT_YET_RELEASED
            }

            if (!notReleased) {
                EditFAB(
                    displayAdd = !alreadyAdded,
                    bsAvailable = component.bsAvailable,
                    expanded = listState.isScrollingUp(),
                    onBS = {

                    },
                    onRate = {
                        component.rate {
                            ratingState.show()
                        }
                    },
                    onProgress = {
                        // ratingState.show()
                    }
                )
            }
        }
    ) {
        CompositionLocalProvider(
            LocalPaddingValues provides LocalPadding().merge(it)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().haze(state = LocalHaze.current),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = LocalPadding(top = 16.dp)
            ) {
                item {
                    CoverSection(
                        coverImage = coverImage,
                        formatFlow = component.format,
                        episodesFlow = component.episodes,
                        durationFlow = component.duration,
                        statusFlow = component.status,
                        modifier = Modifier.fillParentMaxWidth().padding(horizontal = 16.dp)
                    )
                }
                item {
                    RatingSection(
                        ratedFlow = component.rated,
                        popularFlow = component.popular,
                        scoreFlow = component.score,
                        modifier = Modifier.fillParentMaxWidth().padding(16.dp)
                    )
                }
                item {
                    GenreSection(
                        genreFlow = component.genres,
                        modifier = Modifier.fillParentMaxWidth()
                    )
                }
                item {
                    DescriptionSection(
                        descriptionFlow = component.description,
                        translatedDescriptionFlow = component.translatedDescription,
                        modifier = Modifier.fillParentMaxWidth()
                    ) { translated ->
                        component.descriptionTranslation(translated)
                    }
                }
                item {
                    CharacterSection(
                        characterFlow = component.characters,
                        modifier = Modifier.fillParentMaxWidth()
                    ) { char ->
                        component.showCharacter(char)
                    }
                }
                item {
                    TrailerSection(
                        trailerFlow = component.trailer,
                        modifier = Modifier.fillParentMaxWidth()
                    )
                }
            }
        }
    }

    DisposableEffect(listState) {
        onDispose {
            StateSaver.List.mediaOverview = listState.firstVisibleItemIndex
            StateSaver.List.mediaOverviewOffset = listState.firstVisibleItemScrollOffset
        }
    }
}