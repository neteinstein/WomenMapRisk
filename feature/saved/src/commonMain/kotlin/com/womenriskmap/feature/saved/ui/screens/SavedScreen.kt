package com.womenriskmap.feature.saved.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.EmptyState
import com.womenriskmap.core.designsystem.components.RiskSymbol
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.components.style
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.RiskLevel
import com.womenriskmap.core.domain.model.SavedZone
import com.womenriskmap.feature.saved.domain.SavedZoneItem
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun SavedScreen(viewModel: SavedViewModel, contentPadding: PaddingValues, onOpenZone: (SavedZone) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SavedEffect.Deleted -> {
                    val result = snackbar.showSnackbar(
                        getString(Res.string.saved_deleted),
                        getString(Res.string.undo),
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(effect.zone)
                }
                is SavedEffect.Error -> snackbar.showSnackbar(getString(effect.error.messageRes()))
            }
        }
    }
    Box(Modifier.fillMaxSize().padding(contentPadding)) {
        SavedContent(state, onOpenZone, viewModel::delete)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun SavedContent(state: SavedUiState, onOpenZone: (SavedZone) -> Unit, onDelete: (SavedZone) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(Res.string.saved_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(Spacing.l),
        )
        when {
            state.isVisitor -> EmptyState(AppIcons.BookmarkBorder, stringResource(Res.string.saved_visitor))
            state.loading && state.items.isEmpty() -> CircularProgressIndicator(Modifier.padding(Spacing.xxl))
            state.items.isEmpty() -> EmptyState(AppIcons.BookmarkBorder, stringResource(Res.string.saved_empty))
            else -> LazyColumn(
                modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.l, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.s),
            ) {
                item {
                    Text(
                        stringResource(Res.string.saved_swipe_hint),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(state.items, key = { it.zone.id }) { item ->
                    SwipeToDeleteRow(item, onOpenZone, onDelete, Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun SwipeToDeleteRow(
    item: SavedZoneItem,
    onOpenZone: (SavedZone) -> Unit,
    onDelete: (SavedZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) onDelete(item.zone)
    }
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val color by animateColorAsState(
                if (dismissState.targetValue ==
                    SwipeToDismissBoxValue.Settled
                ) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
                label = "swipe",
            )
            Box(
                Modifier.fillMaxSize().background(color, MaterialTheme.shapes.medium).padding(horizontal = Spacing.l),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Filled.Delete, stringResource(Res.string.delete), tint = MaterialTheme.colorScheme.onErrorContainer)
            }
        },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onOpenZone(item.zone) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Row(
                Modifier.padding(Spacing.l),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                val risk = item.risk
                if (risk != null) RiskSymbol(risk, size = 28.dp) else CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Column(Modifier.weight(1f)) {
                    Text(item.zone.name, style = MaterialTheme.typography.titleMedium)
                    if (risk !=
                        null
                    ) {
                        Text(
                            stringResource(risk.style().label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(AppIcons.Map, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Preview
@Composable
private fun SavedPreview() {
    WomenRiskMapTheme {
        SavedContent(
            SavedUiState(
                items = listOf(SavedZoneItem(SavedZone("1", "z1_1", GeoPoint(41.14, -8.61), "Rua das Flores"), RiskLevel.YELLOW)),
                loading = false,
            ),
            {},
            {},
        )
    }
}
