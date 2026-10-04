package com.womenriskmap.feature.saved.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.womenriskmap.core.domain.model.SavedZone
import com.womenriskmap.feature.saved.ui.screens.SavedScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object SavedRoute

fun NavGraphBuilder.savedScreen(contentPadding: () -> PaddingValues, onOpenZone: (SavedZone) -> Unit) {
    composable<SavedRoute> { SavedScreen(viewModel = koinViewModel(), contentPadding = contentPadding(), onOpenZone = onOpenZone) }
}
