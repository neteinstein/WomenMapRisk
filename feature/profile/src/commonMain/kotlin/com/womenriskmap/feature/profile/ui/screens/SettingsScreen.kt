package com.womenriskmap.feature.profile.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.SectionTitle
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.platform.rememberLocationPermissionRequest
import com.womenriskmap.core.designsystem.platform.rememberOpenLanguageSettings
import com.womenriskmap.core.designsystem.platform.rememberShareText
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.feature.profile.ui.components.MenuRow
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onAccountDeleted: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val share = rememberShareText()
    val openLanguage = rememberOpenLanguageSettings()
    val requestPermission = rememberLocationPermissionRequest(viewModel::onPermissionResult)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermission() }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SettingsEffect.ShareExport -> share(effect.json)
                SettingsEffect.AccountDeleted -> onAccountDeleted()
                is SettingsEffect.Error -> snackbar.showSnackbar(getString(effect.error.messageRes()))
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                // Location permission, with the reason (spec §4 Ecrã 9).
                Card(Modifier.padding(Spacing.l).fillMaxWidth()) {
                    Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                            Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(Res.string.settings_location),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (state.locationGranted) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    stringResource(Res.string.settings_location_granted),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                        Text(
                            stringResource(Res.string.settings_location_why),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (!state.locationGranted) {
                            FilledTonalButton(onClick = requestPermission) { Text(stringResource(Res.string.settings_location_request)) }
                        }
                    }
                }
                MenuRow(
                    icon = AppIcons.MyLocation,
                    title = stringResource(Res.string.settings_history),
                    subtitle = stringResource(Res.string.settings_history_body),
                    onClick = { viewModel.onHistoryChange(!state.historyEnabled) },
                    trailing = { Switch(checked = state.historyEnabled, onCheckedChange = viewModel::onHistoryChange) },
                )
                MenuRow(
                    icon = AppIcons.Language,
                    title = stringResource(Res.string.settings_language),
                    subtitle = stringResource(Res.string.settings_language_body),
                    onClick = { openLanguage() },
                )
                Text(
                    stringResource(Res.string.settings_theme_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.l),
                )
                if (state.signedIn) {
                    HorizontalDivider(Modifier.padding(vertical = Spacing.m))
                    SectionTitle(stringResource(Res.string.settings_title), Modifier.padding(horizontal = Spacing.l))
                    MenuRow(
                        icon = AppIcons.Download,
                        title = stringResource(Res.string.settings_export),
                        subtitle = stringResource(Res.string.settings_export_body),
                        onClick = viewModel::export,
                        trailing = if (state.exporting) {
                            { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
                        } else {
                            null
                        },
                    )
                    MenuRow(
                        icon = Icons.Filled.Delete,
                        title = stringResource(Res.string.settings_delete),
                        onClick = viewModel::onDeleteTapped,
                        tint = MaterialTheme.colorScheme.error,
                        trailing = if (state.deleting) {
                            { CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissDelete,
            icon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(stringResource(Res.string.settings_delete_confirm_title)) },
            text = { Text(stringResource(Res.string.settings_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text(stringResource(Res.string.settings_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = viewModel::onDismissDelete) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}
