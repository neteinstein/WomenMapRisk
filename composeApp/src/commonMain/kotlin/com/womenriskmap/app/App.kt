package com.womenriskmap.app

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.womenriskmap.app.ui.navigation.AppNavHost
import com.womenriskmap.core.designsystem.components.LogoMark
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme
import org.koin.compose.viewmodel.koinViewModel

/** Root composable shared by Android, iOS and web. */
@Composable
fun App() {
    WomenRiskMapTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val viewModel: AppViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResume() }
            Crossfade(targetState = state.gate == AppGate.LOADING, label = "splash") { loading ->
                if (loading) {
                    SplashContent()
                } else {
                    AppNavHost(
                        gate = state.gate,
                        session = state.session,
                        onWelcomeDone = viewModel::onWelcomeDone,
                    )
                }
            }
        }
    }
}

@Composable
private fun SplashContent() {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "scale",
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { LogoMark(Modifier.scale(pulse)) }
}
