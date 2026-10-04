package com.womenriskmap.feature.profile.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.CountryField
import com.womenriskmap.core.designsystem.components.LogoMark
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.SecondaryButton
import com.womenriskmap.core.designsystem.components.SectionTitle
import com.womenriskmap.core.designsystem.components.countryLabel
import com.womenriskmap.core.designsystem.components.label
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.components.relativeTime
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.feature.profile.ui.components.MenuRow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

data class ProfileActions(
    val onSettings: () -> Unit,
    val onHelp: () -> Unit,
    val onTerms: () -> Unit,
    val onInvite: () -> Unit,
    val onModeration: () -> Unit,
    val onEditReport: (String) -> Unit,
    val onSignUp: () -> Unit,
    val onLogin: () -> Unit,
)

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, contentPadding: PaddingValues, actions: ProfileActions) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.Saved -> snackbar.showSnackbar(getString(Res.string.profile_saved))
                is ProfileEffect.Error -> snackbar.showSnackbar(getString(effect.error.messageRes()))
            }
        }
    }
    Box(Modifier.fillMaxSize().padding(contentPadding)) {
        ProfileContent(
            state = state,
            actions = actions,
            canEdit = viewModel::canEdit,
            onEdit = viewModel::onEdit,
            onCancelEdit = viewModel::onCancelEdit,
            onPseudonymChange = viewModel::onPseudonymChange,
            onCountryChange = viewModel::onCountryChange,
            onSave = viewModel::save,
            onSignOut = viewModel::signOut,
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    actions: ProfileActions,
    canEdit: (Report) -> Boolean,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onPseudonymChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onSave: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
            Text(
                stringResource(Res.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(Spacing.l),
            )
            val profile = state.profile
            if (state.isVisitor || profile == null) {
                VisitorHeader(actions)
            } else {
                Card(
                    Modifier.padding(horizontal = Spacing.l).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    AnimatedContent(state.editing, label = "edit") { editing ->
                        if (!editing) {
                            Row(
                                Modifier.padding(Spacing.l),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.l),
                            ) {
                                Box(
                                    Modifier.size(56.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        (profile.pseudonym ?: profile.email).take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        profile.pseudonym ?: stringResource(Res.string.profile_pseudonym),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(profile.email, style = MaterialTheme.typography.bodyMedium)
                                    Text(stringResource(countryLabel(profile.country)), style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, stringResource(Res.string.edit)) }
                            }
                        } else {
                            Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                                OutlinedTextField(
                                    value = state.pseudonymDraft,
                                    onValueChange = onPseudonymChange,
                                    label = { Text(stringResource(Res.string.profile_pseudonym)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                CountryField(state.countryDraft, onCountryChange, Modifier.fillMaxWidth())
                                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                                    SecondaryButton(stringResource(Res.string.cancel), onCancelEdit, Modifier.weight(1f))
                                    PrimaryButton(stringResource(Res.string.save), onSave, Modifier.weight(1f), loading = state.saving)
                                }
                            }
                        }
                    }
                }
                SectionTitle(stringResource(Res.string.profile_my_reports), Modifier.padding(horizontal = Spacing.l))
                if (state.myReports.isEmpty()) {
                    Text(
                        stringResource(Res.string.profile_no_reports),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.l),
                    )
                }
                state.myReports.forEach { report -> MyReportRow(report, canEdit(report), actions.onEditReport) }
                HorizontalDivider(Modifier.padding(vertical = Spacing.m))
                if (profile.isEnabled) MenuRow(AppIcons.PersonAdd, stringResource(Res.string.profile_invite), actions.onInvite)
                if (profile.isModerator) MenuRow(AppIcons.Gavel, stringResource(Res.string.profile_moderation), actions.onModeration)
            }
            MenuRow(Icons.Filled.Settings, stringResource(Res.string.profile_settings), actions.onSettings)
            MenuRow(AppIcons.Help, stringResource(Res.string.profile_help), actions.onHelp)
            MenuRow(AppIcons.Policy, stringResource(Res.string.profile_terms), actions.onTerms)
            if (!state.isVisitor) {
                MenuRow(
                    AppIcons.Logout,
                    stringResource(
                        Res.string.profile_logout,
                    ),
                    onSignOut,
                    tint = MaterialTheme.colorScheme.error,
                    trailing = {
                    },
                )
            }
        }
    }
}

@Composable
private fun VisitorHeader(actions: ProfileActions) {
    Column(
        Modifier.fillMaxWidth().padding(Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        LogoMark(size = 72.dp)
        Text(stringResource(Res.string.profile_visitor_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            stringResource(Res.string.profile_visitor_body),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PrimaryButton(stringResource(Res.string.visitor_prompt_signup), actions.onSignUp, Modifier.fillMaxWidth())
        TextButton(onClick = actions.onLogin) { Text(stringResource(Res.string.visitor_prompt_login)) }
    }
}

/** Spec §4 Ecrã 8: "Os meus reportes" with date, type and status. Tap to edit within 24 h. */
@Composable
private fun MyReportRow(report: Report, editable: Boolean, onEdit: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = editable) { onEdit(report.id) }.padding(horizontal = Spacing.l, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(report.type.label), style = MaterialTheme.typography.bodyLarge)
            Text(
                relativeTime(report.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val statusColor = when (report.status) {
            ReportStatus.PUBLISHED -> MaterialTheme.colorScheme.tertiary
            ReportStatus.PENDING, ReportStatus.HIDDEN -> MaterialTheme.colorScheme.secondary
            ReportStatus.REMOVED -> MaterialTheme.colorScheme.error
        }
        AssistChip(onClick = {}, label = { Text(stringResource(report.status.label), color = statusColor) })
        if (editable) {
            Icon(
                Icons.Filled.Edit,
                stringResource(Res.string.edit),
                Modifier.padding(start = Spacing.s).size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Preview
@Composable
private fun ProfileVisitorPreview() {
    WomenRiskMapTheme {
        ProfileContent(ProfileUiState(), ProfileActions({}, {}, {}, {}, {}, {}, {}, {}), { false }, {}, {}, {}, {}, {}, {})
    }
}
