package com.womenriskmap.feature.onboarding.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.womenriskmap.core.designsystem.components.LogoMark
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.SecondaryButton
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import org.jetbrains.compose.resources.stringResource

/** Spec §4 Ecrã 1: logo, one sentence, "Criar conta" / "Explorar sem conta", privacy notice. */
@Composable
fun WelcomeScreen(onCreateAccount: () -> Unit, onExplore: () -> Unit, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    val logoScale = remember { Animatable(0.6f) }
    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showContent = true
        logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = Spacing.xl, vertical = Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            LogoMark(modifier = Modifier.scale(logoScale.value), size = 112.dp)
            Spacer(Modifier.height(Spacing.xl))
            AnimatedVisibility(
                showContent,
                enter =
                fadeIn(tween(600, delayMillis = 150)) + slideInVertically(tween(600, delayMillis = 150)) { it / 3 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 420.dp)) {
                    Text(
                        stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(Spacing.m))
                    Text(
                        stringResource(Res.string.welcome_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(
                showContent,
                enter =
                fadeIn(tween(600, delayMillis = 350)) + slideInVertically(tween(600, delayMillis = 350)) { it / 2 },
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PrimaryButton(stringResource(Res.string.welcome_create_account), onCreateAccount, Modifier.fillMaxWidth())
                    SecondaryButton(stringResource(Res.string.welcome_explore), onExplore, Modifier.fillMaxWidth())
                    TextButton(onClick = onLogin) { Text(stringResource(Res.string.welcome_have_account)) }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Icon(
                            AppIcons.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            stringResource(Res.string.welcome_privacy),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    Text(
                        stringResource(Res.string.welcome_invite_only),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun WelcomePreview() {
    WomenRiskMapTheme { WelcomeScreen({}, {}, {}) }
}
