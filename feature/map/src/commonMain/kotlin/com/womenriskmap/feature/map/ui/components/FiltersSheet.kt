package com.womenriskmap.feature.map.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.SecondaryButton
import com.womenriskmap.core.designsystem.components.SectionTitle
import com.womenriskmap.core.designsystem.components.label
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.domain.model.DayPeriodFilter
import com.womenriskmap.core.domain.model.FilterPeriod
import com.womenriskmap.core.domain.model.ReportFilter
import com.womenriskmap.core.domain.model.ReportType
import org.jetbrains.compose.resources.stringResource

/** Spec §4 Ecrã 6: tipo (multi), período, período do dia, "Aplicar" / "Limpar filtros". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersSheet(draft: ReportFilter, onChange: (ReportFilter) -> Unit, onApply: () -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { FiltersContent(draft, onChange, onApply, onClear) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FiltersContent(draft: ReportFilter, onChange: (ReportFilter) -> Unit, onApply: () -> Unit, onClear: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(
            rememberScrollState(),
        ).padding(horizontal = Spacing.l, vertical = Spacing.s),
    ) {
        Text(stringResource(Res.string.filters_title), style = MaterialTheme.typography.headlineSmall)
        SectionTitle(stringResource(Res.string.filters_type))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            ReportType.entries.forEach { type ->
                val selected = type in draft.types
                FilterChip(
                    selected = selected,
                    onClick = { onChange(draft.copy(types = if (selected) draft.types - type else draft.types + type)) },
                    label = { Text(stringResource(type.label)) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, null) }
                    } else {
                        null
                    },
                )
            }
        }
        SectionTitle(stringResource(Res.string.filters_period))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            FilterPeriod.entries.forEachIndexed { i, period ->
                SegmentedButton(
                    selected = draft.period == period,
                    onClick = { onChange(draft.copy(period = period)) },
                    shape = SegmentedButtonDefaults.itemShape(i, FilterPeriod.entries.size),
                    label = {
                        Text(
                            stringResource(period.label),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }
        SectionTitle(stringResource(Res.string.filters_day_period))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            DayPeriodFilter.entries.forEachIndexed { i, period ->
                SegmentedButton(
                    selected = draft.dayPeriod == period,
                    onClick = { onChange(draft.copy(dayPeriod = period)) },
                    shape = SegmentedButtonDefaults.itemShape(i, DayPeriodFilter.entries.size),
                    label = { Text(stringResource(period.label)) },
                )
            }
        }
        Row(Modifier.padding(vertical = Spacing.xl), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            SecondaryButton(stringResource(Res.string.filters_clear), onClear, Modifier.weight(1f))
            PrimaryButton(stringResource(Res.string.filters_apply), onApply, Modifier.weight(1f))
        }
    }
}
