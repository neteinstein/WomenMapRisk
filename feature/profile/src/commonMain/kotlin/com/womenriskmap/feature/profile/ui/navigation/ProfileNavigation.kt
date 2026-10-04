package com.womenriskmap.feature.profile.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.feature.profile.ui.screens.InfoScreen
import com.womenriskmap.feature.profile.ui.screens.ProfileActions
import com.womenriskmap.feature.profile.ui.screens.ProfileScreen
import com.womenriskmap.feature.profile.ui.screens.SettingsScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object ProfileRoute

@Serializable
data object SettingsRoute

@Serializable
data object HelpRoute

@Serializable
data object TermsRoute

fun NavGraphBuilder.profileScreens(
    contentPadding: () -> PaddingValues,
    actions: ProfileActions,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    composable<ProfileRoute> { ProfileScreen(viewModel = koinViewModel(), contentPadding = contentPadding(), actions = actions) }
    composable<SettingsRoute> { SettingsScreen(viewModel = koinViewModel(), onBack = onBack, onAccountDeleted = onAccountDeleted) }
    composable<HelpRoute> { InfoScreen(Res.string.help_title, Res.string.help_body, onBack) }
    composable<TermsRoute> { InfoScreen(Res.string.terms_title, Res.string.terms_body, onBack) }
}
