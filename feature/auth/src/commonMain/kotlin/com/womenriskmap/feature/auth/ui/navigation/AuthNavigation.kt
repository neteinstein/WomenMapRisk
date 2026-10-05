package com.womenriskmap.feature.auth.ui.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.womenriskmap.feature.auth.ui.screens.AuthMode
import com.womenriskmap.feature.auth.ui.screens.AuthScreen
import com.womenriskmap.feature.auth.ui.screens.CheckEmailScreen
import com.womenriskmap.feature.auth.ui.screens.InviteGateScreen
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** [invite] pre-fills the code (web links `?invite=CODE`). */
@Serializable
data class AuthRoute(val login: Boolean = false, val invite: String? = null)

@Serializable
data class CheckEmailRoute(val email: String)

@Serializable
data object InviteGateRoute

fun NavGraphBuilder.authScreens(
    onBack: () -> Unit,
    onConfirmEmail: (String) -> Unit,
    onSignedIn: () -> Unit,
    onEmailConfirmedContinue: () -> Unit,
) {
    composable<AuthRoute> { entry ->
        val route = entry.toRoute<AuthRoute>()
        val mode = if (route.login) AuthMode.LOGIN else AuthMode.SIGN_UP
        AuthScreen(
            viewModel = koinViewModel { parametersOf(mode, route.invite) },
            onBack = onBack,
            onConfirmEmail = onConfirmEmail,
            onSignedIn = onSignedIn,
        )
    }
    composable<CheckEmailRoute> { entry ->
        val route = entry.toRoute<CheckEmailRoute>()
        CheckEmailScreen(viewModel = koinViewModel { parametersOf(route.email) }, onContinue = onEmailConfirmedContinue)
    }
    composable<InviteGateRoute> { InviteGateScreen(viewModel = koinViewModel()) }
}
