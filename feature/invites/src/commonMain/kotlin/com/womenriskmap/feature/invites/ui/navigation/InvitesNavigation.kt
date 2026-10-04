package com.womenriskmap.feature.invites.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.womenriskmap.feature.invites.ui.screens.InvitesScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object InvitesRoute

fun NavGraphBuilder.invitesScreen(onBack: () -> Unit) {
    composable<InvitesRoute> { InvitesScreen(viewModel = koinViewModel(), onBack = onBack) }
}
