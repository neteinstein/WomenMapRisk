package com.womenriskmap.feature.invites.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.label
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.platform.rememberShareText
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import com.womenriskmap.core.domain.model.Invite
import com.womenriskmap.core.domain.model.InviteState
import com.womenriskmap.core.domain.rules.InviteCode
import com.womenriskmap.core.domain.rules.InviteEligibility
import com.womenriskmap.core.domain.rules.InviteEligibility.Status
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@Composable
fun InvitesScreen(viewModel: InvitesViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val share = rememberShareText()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is InvitesEffect.Share -> share(getString(Res.string.invites_share_message, effect.code, effect.link))
            }
        }
    }
    InvitesContent(
        state = state,
        onBack = onBack,
        onCreate = viewModel::onCreateTapped,
        onConfirmChange = viewModel::onWomenOnlyConfirmedChange,
        onConfirmCreate = viewModel::confirmCreate,
        onDismissCreate = viewModel::onDismissCreate,
        onShare = viewModel::share,
        onRevoke = viewModel::revoke,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvitesContent(
    state: InvitesUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onConfirmChange: (Boolean) -> Unit,
    onConfirmCreate: () -> Unit,
    onDismissCreate: () -> Unit,
    onShare: (Invite) -> Unit,
    onRevoke: (Invite) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.invites_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { WomenOnlyCard() }
            item {
                when (val status = state.status) {
                    null -> if (state.loading) CircularProgressIndicator() else Unit
                    Status.NotEnabled -> Text(
                        stringResource(Res.string.invites_not_enabled),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    is Status.Locked -> LockedCard(status)
                    is Status.Unlocked -> UnlockedCard(status, state.canCreate, state.creating, onCreate)
                }
            }
            state.error?.let { item { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) } }
            if (state.status is Status.Unlocked && state.invites.isEmpty()) {
                item { Text(stringResource(Res.string.invites_none), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(state.invites, key = { it.id }) { invite -> InviteRow(invite, onShare, onRevoke, Modifier.animateItem()) }
        }
    }
    if (state.confirmingCreate) {
        AlertDialog(
            onDismissRequest = onDismissCreate,
            icon = { Icon(AppIcons.Shield, null) },
            title = { Text(stringResource(Res.string.invites_create)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    Text(stringResource(Res.string.invites_women_only))
                    Row(
                        Modifier.fillMaxWidth().clickable(role = Role.Checkbox) { onConfirmChange(!state.womenOnlyConfirmed) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = state.womenOnlyConfirmed, onCheckedChange = null)
                        Text(stringResource(Res.string.invites_confirm_checkbox), modifier = Modifier.padding(start = Spacing.s))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmCreate, enabled = state.womenOnlyConfirmed) {
                    Text(stringResource(Res.string.invites_create))
                }
            },
            dismissButton = { TextButton(onClick = onDismissCreate) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun WomenOnlyCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(Spacing.l),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Shield, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                stringResource(Res.string.invites_women_only),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun LockedCard(status: Status.Locked) {
    val streak by animateFloatAsState(status.currentStreak.toFloat() / InviteEligibility.REQUIRED_STREAK, label = "streak")
    val days by animateFloatAsState(status.distinctDays.toFloat() / InviteEligibility.REQUIRED_DISTINCT_DAYS, label = "days")
    Card(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(Res.string.invites_locked_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(Res.string.invites_locked_body), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                stringResource(Res.string.invites_progress_streak, minOf(status.currentStreak, 3)),
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(progress = { streak.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text(
                stringResource(Res.string.invites_progress_days, minOf(status.distinctDays, 5)),
                style = MaterialTheme.typography.labelLarge,
            )
            LinearProgressIndicator(progress = {
                days.coerceIn(0f, 1f)
            }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun UnlockedCard(status: Status.Unlocked, canCreate: Boolean, creating: Boolean, onCreate: () -> Unit) {
    Card(modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Text(
                if (status.unlimited) {
                    stringResource(
                        Res.string.invites_unlimited,
                    )
                } else {
                    stringResource(Res.string.invites_used, status.used)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            if (!status.unlimited) {
                LinearProgressIndicator(progress = {
                    status.used / InviteEligibility.MAX_INVITES.toFloat()
                }, modifier = Modifier.fillMaxWidth())
                if (status.remaining ==
                    0
                ) {
                    Text(stringResource(Res.string.invites_limit_reached), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            PrimaryButton(
                stringResource(Res.string.invites_create),
                onCreate,
                Modifier.fillMaxWidth(),
                enabled = canCreate,
                loading = creating,
                icon = AppIcons.PersonAdd,
            )
        }
    }
}

@Composable
private fun InviteRow(invite: Invite, onShare: (Invite) -> Unit, onRevoke: (Invite) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(InviteCode.format(invite.code), style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace))
                val expires = invite.expiresAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                Text(
                    stringResource(Res.string.invites_expires, expires.toString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AssistChip(onClick = {}, label = { Text(stringResource(invite.state.label)) })
            if (invite.state == InviteState.PENDING) {
                IconButton(onClick = { onShare(invite) }) { Icon(Icons.Filled.Share, stringResource(Res.string.invites_share)) }
                TextButton(onClick = { onRevoke(invite) }) { Text(stringResource(Res.string.invites_revoke)) }
            }
        }
    }
}

@Preview
@Composable
private fun InvitesLockedPreview() {
    WomenRiskMapTheme { InvitesContent(InvitesUiState(loading = false, status = Status.Locked(2, 3)), {}, {}, {}, {}, {}, {}, {}) }
}
