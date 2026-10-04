package com.womenriskmap.feature.moderation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.EmptyState
import com.womenriskmap.core.designsystem.components.label
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.components.relativeTime
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import com.womenriskmap.feature.moderation.domain.ModerationAction
import com.womenriskmap.feature.moderation.domain.ModerationCounters
import com.womenriskmap.feature.moderation.domain.ModerationItem
import com.womenriskmap.feature.moderation.domain.QueueKind
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ModerationScreen(viewModel: ModerationViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ModerationEffect.Done -> snackbar.showSnackbar(getString(Res.string.moderation_done))
                is ModerationEffect.Error -> snackbar.showSnackbar(getString(effect.error.messageRes()))
            }
        }
    }
    ModerationContent(
        state = state,
        snackbar = snackbar,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onTab = viewModel::onTab,
        onAction = viewModel::onAction,
        onReasonChange = viewModel::onReasonChange,
        onConfirm = viewModel::confirmAction,
        onDismiss = viewModel::onDismissAction,
        onChain = viewModel::showChain,
        onHideChain = viewModel::hideChain,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationContent(
    state: ModerationUiState,
    snackbar: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onTab: (QueueKind) -> Unit,
    onAction: (ModerationItem, ModerationAction) -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onChain: (ModerationItem) -> Unit,
    onHideChain: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.moderation_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back)) }
                },
                actions = {
                    if (state.isModerator) {
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Filled.Refresh, stringResource(Res.string.moderation_refresh))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!state.isModerator) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(AppIcons.Gavel, stringResource(Res.string.moderation_not_allowed))
            }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
            if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            Column(Modifier.widthIn(max = 960.dp).fillMaxWidth()) {
                state.counters?.let { CountersRow(it) }
                PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
                    Tab(state.tab == QueueKind.FLAGGED, {
                        onTab(QueueKind.FLAGGED)
                    }, text = { Text(stringResource(Res.string.moderation_tab_flagged)) })
                    Tab(state.tab == QueueKind.ESTABLISHMENT, {
                        onTab(QueueKind.ESTABLISHMENT)
                    }, text = { Text(stringResource(Res.string.moderation_tab_establishments)) })
                }
                if (!state.loading && state.visibleItems.isEmpty()) {
                    EmptyState(AppIcons.Gavel, stringResource(Res.string.moderation_empty))
                }
                LazyColumn(contentPadding = PaddingValues(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    items(state.visibleItems, key = { it.report.id }) { item -> QueueCard(item, onAction, onChain, Modifier.animateItem()) }
                }
            }
        }
    }
    state.pending?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(pending.action.label)) },
            text = {
                OutlinedTextField(
                    value = pending.reason,
                    onValueChange = onReasonChange,
                    label = { Text(stringResource(Res.string.moderation_reason)) },
                    supportingText = { Text(stringResource(Res.string.moderation_reason_required)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirm, enabled = pending.canConfirm && !state.acting) {
                    Text(stringResource(pending.action.label))
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        )
    }
    state.chain?.let { chain ->
        AlertDialog(
            onDismissRequest = onHideChain,
            title = { Text(stringResource(Res.string.moderation_inviter_chain)) },
            text = {
                val links = chain.links
                if (links == null) {
                    CircularProgressIndicator()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        links.forEach { link ->
                            Text(
                                "${"↳ ".repeat(
                                    link.depth,
                                )}${link.alias} · ${link.status.name.lowercase()} · ${link.invitedCount} / ${link.blockedInvitees}✕",
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onHideChain()
                    onAction(chain.item, ModerationAction.REVOKE_INVITES)
                }) { Text(stringResource(Res.string.moderation_revoke_invites)) }
            },
            dismissButton = { TextButton(onClick = onHideChain) { Text(stringResource(Res.string.close)) } },
        )
    }
}

private val ModerationAction.label: StringResource
    get() = when (this) {
        ModerationAction.APPROVE -> Res.string.moderation_approve
        ModerationAction.REMOVE -> Res.string.moderation_remove
        ModerationAction.BLOCK_USER -> Res.string.moderation_block
        ModerationAction.REVOKE_INVITES -> Res.string.moderation_revoke_invites
    }

@Composable
private fun CountersRow(counters: ModerationCounters) {
    Row(Modifier.fillMaxWidth().padding(Spacing.l), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        Counter(Res.string.moderation_new_reports, counters.newReports, Modifier.weight(1f))
        Counter(Res.string.moderation_pending_flags, counters.pendingFlags, Modifier.weight(1f))
        Counter(Res.string.moderation_pending_establishments, counters.pendingEstablishments, Modifier.weight(1f))
    }
}

@Composable
private fun Counter(label: StringResource, value: Int, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(Spacing.l)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(stringResource(label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QueueCard(
    item: ModerationItem,
    onAction: (ModerationItem, ModerationAction) -> Unit,
    onChain: (ModerationItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(item.report.type.label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${relativeTime(
                            item.report.createdAt,
                        )} · ${stringResource(item.report.dayPeriod.label)} · ${stringResource(item.report.status.label)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.openFlags > 0) {
                    AssistChip(onClick = {
                    }, label = { Text(pluralStringResource(Res.plurals.moderation_flags_count, item.openFlags, item.openFlags)) })
                }
            }
            item.report.description?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            if (item.flagReasons.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    item.flagReasons.forEach { AssistChip(onClick = {}, label = { Text(stringResource(it.label)) }) }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                FilledTonalButton(onClick = {
                    onAction(item, ModerationAction.APPROVE)
                }) { Text(stringResource(Res.string.moderation_approve)) }
                OutlinedButton(onClick = { onAction(item, ModerationAction.REMOVE) }) { Text(stringResource(Res.string.moderation_remove)) }
                OutlinedButton(onClick = { onAction(item, ModerationAction.BLOCK_USER) }) {
                    Text(stringResource(Res.string.moderation_block), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { onChain(item) }) { Text(stringResource(Res.string.moderation_inviter_chain)) }
            }
        }
    }
}

@Preview
@Composable
private fun ModerationNotAllowedPreview() {
    WomenRiskMapTheme { ModerationContent(ModerationUiState(), SnackbarHostState(), {}, {}, {}, { _, _ -> }, {}, {}, {}, {}, {}) }
}
