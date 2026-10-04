package com.womenriskmap.feature.map.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.womenriskmap.core.designsystem.components.LogoMark
import com.womenriskmap.core.designsystem.components.PrimaryButton
import com.womenriskmap.core.designsystem.components.SecondaryButton
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import org.jetbrains.compose.resources.stringResource

/** Spec §3: visitors see the map but cannot report or confirm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorPrompt(onSignUp: () -> Unit, onLogin: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            LogoMark(size = 64.dp)
            Text(stringResource(Res.string.visitor_prompt_title), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                stringResource(Res.string.visitor_prompt_body),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(stringResource(Res.string.visitor_prompt_signup), onSignUp, Modifier.fillMaxWidth())
            SecondaryButton(stringResource(Res.string.visitor_prompt_login), onLogin, Modifier.fillMaxWidth())
        }
    }
}
