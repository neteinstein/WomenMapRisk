package com.womenriskmap.feature.map.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.InfoBanner
import com.womenriskmap.core.designsystem.components.RiskLegend
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.platform.rememberLocationPermissionRequest
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.PilotCity
import com.womenriskmap.core.map.CameraCommand
import com.womenriskmap.core.map.SafetyMap
import com.womenriskmap.feature.map.ui.components.FiltersSheet
import com.womenriskmap.feature.map.ui.components.MapSearchBar
import com.womenriskmap.feature.map.ui.components.VisitorPrompt
import com.womenriskmap.feature.map.ui.components.ZoneSheet
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Spec §4 Ecrã 3 (main screen). Stateful: collects the ViewModel and routes one-shot effects. */
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    contentPadding: PaddingValues,
    onOpenReport: (GeoPoint?) -> Unit,
    onSignUp: () -> Unit,
    onLogin: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val haptics = LocalHapticFeedback.current
    val camera = remember { MutableSharedFlow<CameraCommand>(extraBufferCapacity = 4) }
    var showVisitorPrompt by remember { mutableStateOf(false) }
    val requestPermission = rememberLocationPermissionRequest(viewModel::onLocationPermissionResult)

    LaunchedEffect(viewModel) {
        var commandId = 0L
        viewModel.effects.collect { effect ->
            when (effect) {
                is MapEffect.MoveCamera -> camera.emit(CameraCommand(effect.point, effect.zoom, commandId++))
                is MapEffect.OpenReport -> onOpenReport(effect.near)
                MapEffect.VisitorPrompt -> showVisitorPrompt = true
                MapEffect.ConfirmedHaptic -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                is MapEffect.Error -> snackbar.showSnackbar(getString(effect.error.messageRes()))
                is MapEffect.Message -> snackbar.showSnackbar(
                    getString(
                        when (effect.kind) {
                            MapMessage.CONFIRMED -> Res.string.zone_confirmed
                            MapMessage.FLAGGED -> Res.string.flag_done
                            MapMessage.ZONE_SAVED -> Res.string.zone_saved
                            MapMessage.OFFLINE_NO_REPORT -> Res.string.map_offline_report_disabled
                            MapMessage.CONFIRM_EMAIL -> Res.string.email_unconfirmed_banner
                        },
                    ),
                )
            }
        }
    }

    MapContent(
        state = state,
        contentPadding = contentPadding,
        map = {
            SafetyMap(
                zones = state.zones,
                initialCenter = PilotCity.center,
                initialZoom = PilotCity.DEFAULT_ZOOM,
                cameraCommands = camera,
                onZoneClick = viewModel::onZoneSelected,
                onViewportChanged = viewModel::onViewportChanged,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
            )
        },
        snackbar = { SnackbarHost(snackbar) },
        onQueryChange = viewModel::onSearchQueryChange,
        onPlaceSelected = viewModel::onPlaceSelected,
        onFilters = viewModel::onOpenFilters,
        onReport = viewModel::onReportTapped,
        onCenterOnMe = { if (viewModel.needsLocationPermission()) requestPermission() else viewModel.onCenterOnMe() },
    )

    state.sheet?.let { sheet ->
        ZoneSheet(
            sheet = sheet,
            canConfirm = viewModel::canConfirm,
            onDismiss = viewModel::onDismissZone,
            onConfirm = viewModel::onConfirm,
            onFlag = viewModel::onFlag,
            onSave = viewModel::onSaveZone,
            onReportHere = viewModel::onReportHere,
        )
    }
    state.filterDraft?.let { draft ->
        FiltersSheet(
            draft,
            viewModel::onFilterDraftChange,
            viewModel::onApplyFilters,
            viewModel::onClearFilters,
            viewModel::onDismissFilters,
        )
    }
    if (showVisitorPrompt) {
        VisitorPrompt(
            onSignUp = {
                showVisitorPrompt = false
                onSignUp()
            },
            onLogin = {
                showVisitorPrompt = false
                onLogin()
            },
            onDismiss = { showVisitorPrompt = false },
        )
    }
}

/** Stateless layout of the map screen; [map] is a slot so previews/tests don't need MapLibre. */
@Composable
fun MapContent(
    state: MapUiState,
    contentPadding: PaddingValues,
    map: @Composable () -> Unit,
    snackbar: @Composable () -> Unit,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (com.womenriskmap.core.domain.model.Place) -> Unit,
    onFilters: () -> Unit,
    onReport: () -> Unit,
    onCenterOnMe: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        map()
        if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth().statusBarsPadding())

        // Top: search + filters + status banners
        Column(
            Modifier.align(
                Alignment.TopCenter,
            ).statusBarsPadding().padding(horizontal = Spacing.l, vertical = Spacing.s).widthIn(max = 640.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            MapSearchBar(state.search, !state.filter.isDefault, onQueryChange, onPlaceSelected, onFilters)
            InfoBanner(visible = state.isStale || !state.isOnline, text = stringResource(Res.string.map_stale))
            InfoBanner(
                visible = state.emailUnconfirmed,
                text = stringResource(Res.string.email_unconfirmed_banner),
                icon = Icons.Filled.Email,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            InfoBanner(
                visible = state.locationDenied,
                text = stringResource(Res.string.map_location_denied, PilotCity.NAME),
                icon = Icons.Filled.Place,
                container = MaterialTheme.colorScheme.surfaceContainerHighest,
                content = MaterialTheme.colorScheme.onSurface,
            )
        }

        // Bottom: empty state, legend, centre-on-me, Reportar
        Column(
            Modifier.align(Alignment.BottomCenter).padding(contentPadding).padding(Spacing.l).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            AnimatedVisibility(
                state.showEmptyState,
                enter = fadeIn() + slideInVertically { it / 2 },
                exit =
                fadeOut() + slideOutVertically { it / 2 },
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    modifier = Modifier.widthIn(max = 420.dp),
                ) {
                    Text(
                        stringResource(Res.string.map_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(Spacing.l),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                RiskLegend(Modifier.weight(1f, fill = false))
                Box(Modifier.weight(1f))
                SmallFloatingActionButton(onClick = onCenterOnMe, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Icon(AppIcons.MyLocation, stringResource(Res.string.map_center_me))
                }
            }
            val colors = MaterialTheme.colorScheme
            ExtendedFloatingActionButton(
                onClick = onReport,
                icon = { Icon(AppIcons.Campaign, null) },
                text = { Text(stringResource(Res.string.map_report), style = MaterialTheme.typography.titleMedium) },
                containerColor = if (state.reportEnabled) colors.secondary else colors.surfaceVariant,
                contentColor = if (state.reportEnabled) colors.onSecondary else colors.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 200.dp),
            )
            snackbar()
        }
    }
}
