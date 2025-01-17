package dev.datlag.aniflow.ui.navigation.screen.medium.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.datlag.aniflow.ui.navigation.screen.initial.home.component.GenreChip
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

@Composable
fun GenreSection(
    genreFlow: StateFlow<Collection<String>>,
    modifier: Modifier = Modifier,
) {
    val genres by genreFlow.collectAsStateWithLifecycle()

    if (genres.isNotEmpty()) {
        LazyRow(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
        ) {
            items(genres.toList()) { genre ->
                GenreChip(label = genre)
            }
        }
    }
}