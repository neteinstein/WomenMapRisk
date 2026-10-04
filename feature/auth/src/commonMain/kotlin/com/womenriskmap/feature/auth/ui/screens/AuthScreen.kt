package com.womenriskmap.feature.auth.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.core.designsystem.components.CountryField
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.SecondaryButton
import com.womenriskmap.core.designsystem.components.messageRes
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import com.womenriskmap.feature.auth.domain.SignUpError
import com.womenriskmap.feature.auth.ui.components.CheckRow
import com.womenriskmap.feature.auth.ui.components.PasswordField
import org.jetbrains.compose.resources.stringResource

/** Spec §4 Ecrã 2: registo e login. Stateful entry: wires the ViewModel to [AuthContent]. */
@Composable
fun AuthScreen(viewModel: AuthViewModel, onBack: () -> Unit, onConfirmEmail: (String) -> Unit, onSignedIn: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AuthEffect.ConfirmEmail -> onConfirmEmail(effect.email)
                AuthEffect.SignedIn -> onSignedIn()
            }
        }
    }
    AuthContent(
        state = state,
        onBack = onBack,
        onModeChange = viewModel::onModeChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePassword = viewModel::onTogglePasswordVisibility,
        onCountryChange = viewModel::onCountryChange,
        onInviteChange = viewModel::onInviteCodeChange,
        onTermsChange = viewModel::onAcceptTermsChange,
        onDeclareChange = viewModel::onDeclareWomanChange,
        onSubmit = viewModel::submit,
        onGoogle = viewModel::signInWithGoogle,
        onResend = viewModel::resendConfirmation,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthContent(
    state: AuthUiState,
    onBack: () -> Unit,
    onModeChange: (AuthMode) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onCountryChange: (String) -> Unit,
    onInviteChange: (String) -> Unit,
    onTermsChange: (Boolean) -> Unit,
    onDeclareChange: (Boolean) -> Unit,
    onSubmit: () -> Unit,
    onGoogle: () -> Unit,
    onResend: () -> Unit,
) {
    var showTerms by remember { mutableStateOf(false) }
    val signUp = state.mode == AuthMode.SIGN_UP
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(Res.string.back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.xl)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                AnimatedContent(targetState = state.mode, label = "title") { mode ->
                    Text(
                        stringResource(if (mode == AuthMode.SIGN_UP) Res.string.auth_title_signup else Res.string.auth_title_login),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                if (signUp) WomenOnlyNotice()

                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = { Text(stringResource(Res.string.auth_email)) },
                    singleLine = true,
                    isError = SignUpError.INVALID_EMAIL in state.fieldErrors,
                    supportingText = if (SignUpError.INVALID_EMAIL in state.fieldErrors) {
                        { Text(stringResource(Res.string.error_email_invalid)) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
                PasswordField(
                    value = state.password,
                    onValueChange = onPasswordChange,
                    visible = state.passwordVisible,
                    onToggleVisible = onTogglePassword,
                    isError = signUp && SignUpError.WEAK_PASSWORD in state.fieldErrors,
                    supportingText = if (signUp) {
                        stringResource(
                            if (SignUpError.WEAK_PASSWORD in
                                state.fieldErrors
                            ) {
                                Res.string.error_weak_password
                            } else {
                                Res.string.auth_password_hint
                            },
                        )
                    } else {
                        null
                    },
                )
                AnimatedVisibility(signUp) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        CountryField(state.country, onCountryChange, Modifier.fillMaxWidth())
                        OutlinedTextField(
                            value = state.inviteCode,
                            onValueChange = onInviteChange,
                            label = { Text(stringResource(Res.string.auth_invite_code)) },
                            placeholder = { Text(stringResource(Res.string.auth_invite_code_hint)) },
                            singleLine = true,
                            isError = SignUpError.INVALID_INVITE in state.fieldErrors,
                            supportingText = if (SignUpError.INVALID_INVITE in state.fieldErrors) {
                                { Text(stringResource(Res.string.error_invalid_invite)) }
                            } else {
                                null
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        CheckRow(
                            checked = state.declaredWoman,
                            onCheckedChange = onDeclareChange,
                            label = stringResource(Res.string.auth_declare_woman),
                            isError = SignUpError.DECLARATION_REQUIRED in state.fieldErrors,
                        )
                        CheckRow(
                            checked = state.acceptedTerms,
                            onCheckedChange = onTermsChange,
                            label = stringResource(Res.string.auth_accept_terms),
                            isError = SignUpError.TERMS_REQUIRED in state.fieldErrors,
                        )
                        TextButton(onClick = { showTerms = true }) { Text(stringResource(Res.string.auth_view_terms)) }
                    }
                }

                state.error?.let { error ->
                    ErrorCard(message = stringResource(error.messageRes()), onResend = if (state.canResendConfirmation) onResend else null)
                }

                PrimaryButton(
                    text = stringResource(if (signUp) Res.string.auth_signup_button else Res.string.auth_login_button),
                    onClick = onSubmit,
                    loading = state.submitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f))
                    Text(
                        stringResource(Res.string.auth_or),
                        modifier = Modifier.padding(horizontal = Spacing.m),
                        style = MaterialTheme.typography.labelMedium,
                    )
                    HorizontalDivider(Modifier.weight(1f))
                }
                SecondaryButton(stringResource(Res.string.auth_google), onGoogle, Modifier.fillMaxWidth(), enabled = !state.submitting)
                TextButton(
                    onClick = { onModeChange(if (signUp) AuthMode.LOGIN else AuthMode.SIGN_UP) },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(if (signUp) Res.string.auth_switch_to_login else Res.string.auth_switch_to_signup))
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
    if (showTerms) TermsDialog(onDismiss = { showTerms = false })
}

@Composable
private fun WomenOnlyNotice() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(
            Modifier.padding(Spacing.m),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(AppIcons.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(
                stringResource(Res.string.auth_women_only_notice),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
internal fun ErrorCard(message: String, onResend: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(Spacing.m)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(20.dp),
                )
                Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
            }
            if (onResend != null) TextButton(onClick = onResend) { Text(stringResource(Res.string.auth_resend)) }
        }
    }
}

@Composable
fun TermsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.terms_title)) },
        text = { Text(stringResource(Res.string.terms_body), modifier = Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ok)) } },
    )
}

@Preview
@Composable
private fun AuthSignUpPreview() {
    WomenRiskMapTheme {
        AuthContent(AuthUiState(fieldErrors = setOf(SignUpError.WEAK_PASSWORD)), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    }
}
