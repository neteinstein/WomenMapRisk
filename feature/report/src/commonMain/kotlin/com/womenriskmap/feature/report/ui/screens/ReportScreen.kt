package com.womenriskmap.feature.report.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.SectionTitle
import com.womenriskmap.core.designsystem.components.label
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.OccurredWhen
import com.womenriskmap.core.domain.model.Place
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.rules.ReportPolicy
import com.womenriskmap.core.map.CameraCommand
import com.womenriskmap.core.map.PinPickerMap
import com.womenriskmap.feature.report.ui.components.SupportCard
import kotlinx.coroutines.flow.MutableSharedFlow
import org.jetbrains.compose.resources.stringResource

@Composable
fun ReportScreen(viewModel: ReportViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val camera = remember { MutableSharedFlow<CameraCommand>(extraBufferCapacity = 2) }
    LaunchedEffect(viewModel) {
        var id = 0L
        viewModel.effects.collect { effect ->
            when (effect) {
                is ReportEffect.MoveCamera -> camera.emit(CameraCommand(effect.point, 16.0, id++))
                ReportEffect.Closed -> onClose()
            }
        }
    }
    AnimatedContent(targetState = state.step, transitionSpec = {
        (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut()
    }, label = "step") { step ->
        when (step) {
            ReportStep.FORM -> ReportFormContent(
                state = state,
                map = { PinPickerMap(state.location, camera, viewModel::onLocationChanged, Modifier.fillMaxSize()) },
                onClose = onClose,
                onType = viewModel::onTypeSelected,
                onWhen = viewModel::onWhenSelected,
                onPeriod = viewModel::onDayPeriodSelected,
                onDescription = viewModel::onDescriptionChange,
                onEstablishment = viewModel::onEstablishmentChange,
                onSearch = viewModel::onSearchQueryChange,
                onPlace = viewModel::onPlaceSelected,
                onSubmit = viewModel::submit,
                onDelete = viewModel::delete,
            )
            ReportStep.DONE -> ReportDoneContent(state, onClose)
        }
    }
}

/** Spec §4 Ecrã 5. Fastest path is 3 taps: Reportar → type → Enviar (spec §12: ≤ 6 taps, < 1 min). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportFormContent(
    state: ReportUiState,
    map: @Composable () -> Unit,
    onClose: () -> Unit,
    onType: (ReportType) -> Unit,
    onWhen: (OccurredWhen) -> Unit,
    onPeriod: (DayPeriod) -> Unit,
    onDescription: (String) -> Unit,
    onEstablishment: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    onPlace: (Place) -> Unit,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEdit) Res.string.report_edit_title else Res.string.report_title)) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, stringResource(Res.string.close)) } },
                actions = {
                    if (state.isEdit) {
                        IconButton(onClick = {
                            confirmDelete = true
                        }) { Icon(Icons.Filled.Delete, stringResource(Res.string.delete)) }
                    }
                },
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(Spacing.l), contentAlignment = Alignment.Center) {
                    PrimaryButton(
                        stringResource(Res.string.report_send),
                        onSubmit,
                        Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                        enabled = state.canSubmit,
                        loading = state.submitting,
                        containerColor = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = Spacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp)) {
                SectionTitle(stringResource(Res.string.report_location))
                Box(Modifier.fillMaxWidth().height(220.dp).clip(MaterialTheme.shapes.large)) {
                    map()
                    StreetSearch(state, onSearch, onPlace, Modifier.align(Alignment.TopCenter).padding(Spacing.s))
                }
                Row(
                    Modifier.padding(top = Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Icon(Icons.Filled.Info, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(Res.string.report_location_hint) + " · " + stringResource(Res.string.report_location_approx),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                SectionTitle(stringResource(Res.string.report_type))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    ReportType.entries.forEach { type ->
                        FilterChip(
                            selected = state.type == type,
                            onClick = { onType(type) },
                            label = { Text(stringResource(type.label)) },
                            leadingIcon = if (state.type == type) {
                                { Icon(Icons.Filled.Check, null) }
                            } else {
                                null
                            },
                        )
                    }
                }

                SectionTitle(stringResource(Res.string.report_when))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    OccurredWhen.entries.forEachIndexed { i, value ->
                        SegmentedButton(
                            selected = state.occurredWhen == value,
                            onClick = { onWhen(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, OccurredWhen.entries.size),
                            label = { Text(stringResource(value.label), maxLines = 1, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }

                SectionTitle(stringResource(Res.string.report_period))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    DayPeriod.entries.forEachIndexed { i, value ->
                        SegmentedButton(
                            selected = state.dayPeriod == value,
                            onClick = { onPeriod(value) },
                            shape = SegmentedButtonDefaults.itemShape(i, DayPeriod.entries.size),
                            icon = { Icon(if (value == DayPeriod.NIGHT) AppIcons.Night else AppIcons.Day, null, Modifier.size(18.dp)) },
                            label = { Text(stringResource(value.label)) },
                        )
                    }
                }

                SectionTitle(stringResource(Res.string.report_description))
                OutlinedTextField(
                    value = state.description,
                    onValueChange = onDescription,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp),
                    supportingText = {
                        Row {
                            Text(stringResource(Res.string.report_description_warning), modifier = Modifier.weight(1f))
                            Text(stringResource(Res.string.report_chars, state.description.length, ReportPolicy.MAX_DESCRIPTION_LENGTH))
                        }
                    },
                )
                AnimatedVisibility(state.findings.isNotEmpty()) {
                    Column(Modifier.padding(top = Spacing.xs)) {
                        state.findings.forEach { finding ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Icon(Icons.Filled.Warning, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                Text(
                                    stringResource(finding.label),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = Spacing.m).clickable(role = Role.Checkbox) {
                        onEstablishment(!state.isEstablishment)
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = state.isEstablishment, onCheckedChange = null)
                    Column(Modifier.padding(start = Spacing.s)) {
                        Text(stringResource(Res.string.report_establishment), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(Res.string.report_establishment_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                state.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.padding(top = Spacing.m).fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(error.messageRes()),
                            modifier = Modifier.padding(Spacing.m),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(Res.string.report_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(stringResource(Res.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun StreetSearch(state: ReportUiState, onSearch: (String) -> Unit, onPlace: (Place) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Surface(shape = MaterialTheme.shapes.extraLarge, shadowElevation = 4.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearch,
                placeholder = { Text(stringResource(Res.string.report_search_street)) },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
            )
        }
        if (state.searchResults.isNotEmpty()) {
            Surface(shape = MaterialTheme.shapes.medium, shadowElevation = 4.dp, modifier = Modifier.padding(top = Spacing.xs)) {
                Column {
                    state.searchResults.take(4).forEach { place ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPlace(place) }.padding(Spacing.m),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary)
                            Text(place.name + (place.detail?.let { " · $it" } ?: ""), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Confirmation: "Obrigada, o teu reporte ajuda outras mulheres." (+ support after "agressão"). */
@Composable
fun ReportDoneContent(state: ReportUiState, onDone: () -> Unit) {
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterVertically),
    ) {
        Spacer(Modifier.height(Spacing.xxl))
        Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(96.dp).scale(scale.value))
        Text(
            stringResource(Res.string.report_thanks),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 420.dp),
        )
        if (state.pendingReview) {
            Text(
                stringResource(Res.string.report_pending_review),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(Res.string.report_edit_until),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.showSupport) SupportCard(state.emergencyContacts, Modifier.widthIn(max = 480.dp))
        PrimaryButton(stringResource(Res.string.report_done), onDone, Modifier.widthIn(max = 420.dp).fillMaxWidth())
    }
}

@Preview
@Composable
private fun ReportFormPreview() {
    WomenRiskMapTheme {
        ReportFormContent(ReportUiState(type = ReportType.POORLY_LIT, locationReady = true), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
