package com.womenriskmap.feature.auth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.LogoMark
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun InviteGateScreen(viewModel: InviteGateViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InviteGateContent(state, viewModel::onCodeChange, viewModel::submit, viewModel::exploreWithoutAccount)
}

@Composable
fun InviteGateContent(state: InviteGateUiState, onCodeChange: (String) -> Unit, onSubmit: () -> Unit, onLater: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            LogoMark(size = 80.dp)
            Text(stringResource(Res.string.invite_gate_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                stringResource(Res.string.invite_gate_body),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.invites_women_only),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
            OutlinedTextField(
                value = state.code,
                onValueChange = onCodeChange,
                label = { Text(stringResource(Res.string.auth_invite_code)) },
                placeholder = { Text(stringResource(Res.string.auth_invite_code_hint)) },
                singleLine = true,
                isError = state.error != null,
                supportingText = state.error?.let { { Text(stringResource(it.messageRes())) } },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                stringResource(Res.string.invite_gate_submit),
                onSubmit,
                Modifier.fillMaxWidth(),
                enabled = state.canSubmit,
                loading = state.busy,
            )
            TextButton(onClick = onLater) { Text(stringResource(Res.string.invite_gate_later)) }
        }
    }
}

@Preview
@Composable
private fun InviteGatePreview() {
    WomenRiskMapTheme { InviteGateContent(InviteGateUiState(code = "ABCD-EFGH"), {}, {}, {}) }
}
