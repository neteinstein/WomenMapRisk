package com.womenriskmap.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.feature.map.ui.navigation.MapRoute
import com.womenriskmap.feature.saved.ui.navigation.SavedRoute
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import kotlin.reflect.KClass

@Serializable
data object ProfileRoute

/** Spec §4 Ecrã 3: "Menu em baixo: Mapa · Guardados · Perfil". */
enum class TopLevelDestination(val route: Any, val routeClass: KClass<*>, val icon: ImageVector, val label: StringResource) {
    MAP(MapRoute(), MapRoute::class, AppIcons.Map, Res.string.nav_map),
    SAVED(SavedRoute, SavedRoute::class, AppIcons.Bookmark, Res.string.nav_saved),
    PROFILE(ProfileRoute, ProfileRoute::class, Icons.Filled.Person, Res.string.nav_profile),
}
