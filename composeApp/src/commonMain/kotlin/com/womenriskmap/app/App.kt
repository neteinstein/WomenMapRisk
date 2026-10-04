package com.womenriskmap.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.womenriskmap.core.designsystem.theme.WomenRiskMapTheme

/** Root composable shared by Android, iOS and web. */
@Composable
fun App() {
    WomenRiskMapTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(contentAlignment = Alignment.Center) { Text("Women Risk Map") }
        }
    }
}
