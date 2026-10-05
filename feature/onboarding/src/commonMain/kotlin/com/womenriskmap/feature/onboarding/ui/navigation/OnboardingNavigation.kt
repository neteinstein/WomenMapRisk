package com.womenriskmap.feature.onboarding.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.womenriskmap.feature.onboarding.ui.screens.WelcomeScreen
import kotlinx.serialization.Serializable

@Serializable
data object WelcomeRoute

fun NavGraphBuilder.welcomeScreen(onCreateAccount: () -> Unit, onExplore: () -> Unit, onLogin: () -> Unit) {
    composable<WelcomeRoute> { WelcomeScreen(onCreateAccount = onCreateAccount, onExplore = onExplore, onLogin = onLogin) }
}
