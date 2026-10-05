package com.womenriskmap.feature.map.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.RiskBadge
import com.womenriskmap.core.designsystem.components.SecondaryButton
import com.womenriskmap.core.designsystem.components.label
import com.womenriskmap.core.designsystem.components.relativeTime
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.feature.map.ui.screens.ZoneSheetState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Spec §4 Ecrã 4: slides up from the bottom when a coloured zone is tapped. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneSheet(
    sheet: ZoneSheetState,
    canConfirm: (Report) -> Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Report) -> Unit,
    onFlag: (Report, FlagReason) -> Unit,
    onSave: () -> Unit,
    onReportHere: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)) {
        AnimatedContent(targetState = sheet.detail == null, transitionSpec = {
            fadeIn() togetherWith fadeOut()
        }, label = "zone") { loading ->
            if (loading) {
                Box(Modifier.fillMaxWidth().padding(Spacing.xxl), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                ZoneSheetContent(sheet, canConfirm, onConfirm, onFlag, onSave, onReportHere)
            }
        }
    }
}

@Composable
fun ZoneSheetContent(
    sheet: ZoneSheetState,
    canConfirm: (Report) -> Boolean,
    onConfirm: (Report) -> Unit,
    onFlag: (Report, FlagReason) -> Unit,
    onSave: () -> Unit,
    onReportHere: () -> Unit,
) {
    val detail = sheet.detail ?: return
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = Spacing.l)) {
        Text(detail.name ?: stringResource(Res.string.zone_unknown_name), style = MaterialTheme.typography.headlineSmall)
        Row(
            Modifier.padding(vertical = Spacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s),
        ) {
            RiskBadge(detail.risk)
            Text(
                pluralStringResource(Res.plurals.zone_reports, detail.totalReports, detail.totalReports) + " · " +
                    pluralStringResource(Res.plurals.zone_confirmed_by, detail.totalConfirmations, detail.totalConfirmations),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), modifier = Modifier.padding(vertical = Spacing.s)) {
            SecondaryButton(
                text = stringResource(if (sheet.isSaved) Res.string.zone_saved else Res.string.zone_save),
                onClick = onSave,
                enabled = !sheet.isSaved,
                icon = if (sheet.isSaved) AppIcons.Bookmark else AppIcons.BookmarkBorder,
                modifier = Modifier.weight(1f),
            )
            PrimaryButton(stringResource(Res.string.zone_report_here), onReportHere, Modifier.weight(1f), icon = AppIcons.Campaign)
        }
        LazyColumn(
            Modifier.heightIn(max = 460.dp),
            verticalArrangement = Arrangement.spacedBy(Spacing.s),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = Spacing.l),
        ) {
            items(detail.reports, key = { it.id }) { report ->
                ReportCard(
                    report = report,
                    canConfirm = canConfirm(report),
                    pulse = sheet.pulseReportId == report.id,
                    onConfirm = { onConfirm(report) },
                    onFlag = { reason -> onFlag(report, reason) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun ReportCard(
    report: Report,
    canConfirm: Boolean,
    pulse: Boolean,
    onConfirm: () -> Unit,
    onFlag: (FlagReason) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    var flagging by remember { mutableStateOf(false) }
    val heart = remember { Animatable(1f) }
    LaunchedEffect(pulse, report.confirmations) {
        if (pulse) {
            heart.animateTo(1.35f, spring(dampingRatio = Spring.DampingRatioHighBouncy))
            heart.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
        }
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(report.type.label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${stringResource(report.occurredWhen.label)} · ${relativeTime(report.createdAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AssistChip(
                    onClick = {},
                    label = { Text(stringResource(report.dayPeriod.label)) },
                    leadingIcon = {
                        Icon(
                            if (report.dayPeriod ==
                                DayPeriod.NIGHT
                            ) {
                                AppIcons.Night
                            } else {
                                AppIcons.Day
                            },
                            null,
                            Modifier.size(16.dp),
                        )
                    },
                )
                if (!report.isMine) {
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(Res.string.zone_more_actions)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.zone_flag)) },
                                leadingIcon = { Icon(AppIcons.Flag, null) },
                                onClick = {
                                    menu = false
                                    flagging = true
                                },
                            )
                        }
                    }
                }
            }
            report.description?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = Spacing.s))
            }
            Row(Modifier.padding(top = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (report.confirmedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp).scale(heart.value),
                )
                Text(
                    pluralStringResource(Res.plurals.report_confirmations, report.confirmations, report.confirmations),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = Spacing.xs).weight(1f),
                )
                when {
                    report.isMine -> Text(
                        stringResource(Res.string.zone_yours),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    else -> FilledTonalButton(onClick = onConfirm, enabled = canConfirm) { Text(stringResource(Res.string.zone_also_felt)) }
                }
            }
        }
    }
    if (flagging) {
        FlagDialog(onDismiss = { flagging = false }, onConfirm = { reason ->
            flagging = false
            onFlag(reason)
        })
    }
}

@Composable
private fun FlagDialog(onDismiss: () -> Unit, onConfirm: (FlagReason) -> Unit) {
    var selected by remember { mutableStateOf(FlagReason.FALSE_INFORMATION) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.flag_title)) },
        text = {
            Column {
                FlagReason.entries.forEach { reason ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = selected == reason, role = Role.RadioButton) {
                            selected = reason
                        }.padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == reason, onClick = null)
                        Text(stringResource(reason.label), modifier = Modifier.padding(start = Spacing.s))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text(stringResource(Res.string.flag_submit)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
    )
}
