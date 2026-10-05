package com.womenriskmap.feature.auth.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.EmptyState
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun CheckEmailScreen(viewModel: CheckEmailViewModel, onContinue: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CheckEmailContent(state, onResend = viewModel::resend, onConfirmed = {
        viewModel.refresh()
        onContinue()
    })
}

@Composable
fun CheckEmailContent(state: CheckEmailUiState, onResend: () -> Unit, onConfirmed: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            EmptyState(icon = Icons.Filled.Email, message = stringResource(Res.string.auth_check_email_body, state.email))
            Text(
                stringResource(Res.string.auth_check_email_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            if (state.resent) Text(stringResource(Res.string.auth_resent), color = MaterialTheme.colorScheme.tertiary)
            state.error?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
            PrimaryButton(stringResource(Res.string.auth_confirmed_continue), onConfirmed, Modifier.fillMaxWidth(), loading = state.busy)
            TextButton(onClick = onResend, enabled = !state.busy) { Text(stringResource(Res.string.auth_resend)) }
        }
    }
}

@Preview
@Composable
private fun CheckEmailPreview() {
    WomenRiskMapTheme { CheckEmailContent(CheckEmailUiState("ana@example.com"), {}, {}) }
}
