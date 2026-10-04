package com.womenriskmap.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.womenriskmap.app.AppGate
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.feature.auth.ui.navigation.AuthRoute
import com.womenriskmap.feature.auth.ui.navigation.CheckEmailRoute
import com.womenriskmap.feature.auth.ui.navigation.InviteGateRoute
import com.womenriskmap.feature.auth.ui.navigation.authScreens
import com.womenriskmap.feature.map.ui.navigation.MapRoute
import com.womenriskmap.feature.map.ui.navigation.mapScreen
import com.womenriskmap.feature.onboarding.ui.navigation.WelcomeRoute
import com.womenriskmap.feature.onboarding.ui.navigation.welcomeScreen
import com.womenriskmap.feature.report.ui.navigation.ReportRoute
import com.womenriskmap.feature.report.ui.navigation.reportScreen
import org.jetbrains.compose.resources.stringResource

/**
 * Single NavHost for the whole app. Features expose `NavGraphBuilder.xxx(...)` builders and talk to each
 * other only through the callbacks wired here (features never depend on each other).
 */
@Composable
fun AppNavHost(
    gate: AppGate,
    session: SessionState,
    onWelcomeDone: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val invite = remember { launchInviteCode() }
    val start: Any = remember {
        when {
            gate == AppGate.INVITE_GATE -> InviteGateRoute
            invite != null && session is SessionState.Visitor -> AuthRoute(invite = invite)
            gate == AppGate.WELCOME -> WelcomeRoute
            else -> MapRoute()
        }
    }

    // React to session-driven gate changes (sign-in, sign-out, invite redeemed).
    LaunchedEffect(gate) {
        when (gate) {
            AppGate.INVITE_GATE -> navController.navigate(InviteGateRoute) { popUpTo(0) { inclusive = true } }
            AppGate.MAIN -> {
                val current = navController.currentDestination
                if (current?.hasRoute<InviteGateRoute>() == true || current?.hasRoute<WelcomeRoute>() == true) {
                    navController.navigate(MapRoute()) { popUpTo(0) { inclusive = true } }
                }
            }
            else -> Unit
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { dest ->
        backStack?.destination?.hierarchy?.any { it.hasRoute(dest.routeClass) } ==
            true
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AnimatedVisibility(currentTopLevel != null, enter = slideInVertically { it }, exit = slideOutVertically { it }) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = dest == currentTopLevel,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.label)) },
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
            exitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
        ) {
            welcomeScreen(
                onCreateAccount = {
                    onWelcomeDone()
                    navController.navigate(AuthRoute())
                },
                onExplore = onWelcomeDone,
                onLogin = {
                    onWelcomeDone()
                    navController.navigate(AuthRoute(login = true))
                },
            )
            authScreens(
                onBack = { if (!navController.popBackStack()) navController.navigate(MapRoute()) { popUpTo(0) { inclusive = true } } },
                onConfirmEmail = { email -> navController.navigate(CheckEmailRoute(email)) { popUpTo<AuthRoute> { inclusive = true } } },
                onSignedIn = { navController.navigate(MapRoute()) { popUpTo(0) { inclusive = true } } },
                onEmailConfirmedContinue = { navController.navigate(MapRoute()) { popUpTo(0) { inclusive = true } } },
            )
            mapScreen(
                contentPadding = { contentPadding },
                onOpenReport = { near -> navController.navigate(ReportRoute(near?.latitude, near?.longitude)) },
                onSignUp = { navController.navigate(AuthRoute()) },
                onLogin = { navController.navigate(AuthRoute(login = true)) },
            )
            composable<SavedRoute> { Placeholder("Saved") }
            composable<ProfileRoute> { Placeholder("Profile") }
            reportScreen(onClose = { navController.popBackStack() })
        }
    }
}

@Composable
private fun Placeholder(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(name) }
}
