package dev.datlag.aniflow.ui.navigation.screen.initial.home.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.datlag.aniflow.anilist.PopularSeasonStateMachine
import dev.datlag.aniflow.anilist.SeasonQuery
import dev.datlag.aniflow.anilist.TrendingQuery
import dev.datlag.aniflow.anilist.model.Medium
import dev.datlag.aniflow.anilist.state.SeasonState
import dev.datlag.aniflow.common.shimmer
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun PopularSeasonOverview(
    state: StateFlow<SeasonState>,
    onClick: (Medium) -> Unit,
) {
    val loadingState by state.collectAsStateWithLifecycle()

    when (val reachedState = loadingState) {
        is SeasonState.Loading -> {
            Loading()
        }
        is SeasonState.Success -> {
            SuccessContent(
                data = reachedState.data.Page?.mediaFilterNotNull() ?: emptyList(),
                onClick = onClick
            )
        }
        else -> {

        }
    }
}

@Composable
private fun Loading() {
    LazyRow(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        repeat(5) {
            item {
                Box(
                    modifier = Modifier.width(200.dp).height(280.dp).shimmer(CardDefaults.shape)
                )
            }
        }
    }
}

@Composable
private fun SuccessContent(
    data: List<SeasonQuery.Medium>,
    onClick: (Medium) -> Unit
) {
    val listState = rememberLazyListState()
    var highlightedItem by remember { mutableIntStateOf(0) }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            layoutInfo.visibleItemsInfo
                .firstOrNull { it.offset >= layoutInfo.viewportStartOffset }
                ?.index ?: 0
        }.distinctUntilChanged().collect {
            highlightedItem = it
        }
    }

    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(data, key = { _, it -> it.id }) {index, medium ->
            MediumCard(
                medium = Medium(medium),
                isHighlighted = index == highlightedItem,
                lazyListState = listState,
                modifier = Modifier.width(200.dp).height(280.dp),
                onClick = onClick
            )
        }
    }
}
