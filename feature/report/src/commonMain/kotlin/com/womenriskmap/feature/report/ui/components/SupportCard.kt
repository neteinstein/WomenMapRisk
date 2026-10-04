package com.womenriskmap.feature.report.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.womenriskmap.core.designsystem.components.label
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.domain.rules.EmergencyContacts
import org.jetbrains.compose.resources.stringResource

/** Spec §4 Ecrã 5: after an "agressão" report, show the country's emergency contacts and a support message. */
@Composable
fun SupportCard(contacts: List<EmergencyContacts.Contact>, modifier: Modifier = Modifier) {
    val uri = LocalUriHandler.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(Spacing.l), verticalArrangement = Arrangement.spacedBy(Spacing.m)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    stringResource(Res.string.support_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Text(
                stringResource(Res.string.support_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            contacts.forEach { contact ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(contact.service.label), style = MaterialTheme.typography.titleSmall)
                        Text(contact.number, style = MaterialTheme.typography.bodyMedium)
                    }
                    FilledTonalButton(onClick = { runCatching { uri.openUri("tel:${contact.number}") } }) {
                        Icon(Icons.Filled.Call, null)
                        Text(stringResource(Res.string.support_call, contact.number), modifier = Modifier.padding(start = Spacing.s))
                    }
                }
            }
        }
    }
}
