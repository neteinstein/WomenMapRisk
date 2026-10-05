package com.womenriskmap.feature.moderation.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.womenriskmap.feature.moderation.ui.screens.ModerationScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object ModerationRoute

fun NavGraphBuilder.moderationScreen(onBack: () -> Unit) {
    composable<ModerationRoute> { ModerationScreen(viewModel = koinViewModel(), onBack = onBack) }
}
