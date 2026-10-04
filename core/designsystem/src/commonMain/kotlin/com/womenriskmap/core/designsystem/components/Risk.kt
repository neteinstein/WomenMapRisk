package com.womenriskmap.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.resources.Res
import com.womenriskmap.core.designsystem.theme.LocalRiskColors
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.domain.model.RiskLevel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Shape + symbol + colour per risk level: perceivable without colour (spec §4 Ecrã 3, daltonismo). */
data class RiskStyle(
    val color: Color,
    val onColor: Color,
    val symbol: ImageVector,
    val shape: Shape,
    val label: StringResource,
    val description: StringResource,
)

private val TriangleShape = GenericShape { size, _ ->
    moveTo(size.width / 2f, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

@Composable
@ReadOnlyComposable
fun RiskLevel.style(): RiskStyle {
    val c = LocalRiskColors.current
    return when (this) {
        RiskLevel.GREEN -> RiskStyle(c.green, c.onGreen, Icons.Filled.Check, CircleShape, Res.string.legend_green, Res.string.risk_green_cd)
        RiskLevel.YELLOW -> RiskStyle(
            c.yellow,
            c.onYellow,
            Icons.Filled.Warning,
            TriangleShape,
            Res.string.legend_yellow,
            Res.string.risk_yellow_cd,
        )
        RiskLevel.RED -> RiskStyle(c.red, c.onRed, Icons.Filled.Close, CutCornerShape(30), Res.string.legend_red, Res.string.risk_red_cd)
    }
}

/** Coloured shape with a symbol inside: circle ✓ (green), triangle ! (yellow), octagon ✕ (red). */
@Composable
fun RiskSymbol(level: RiskLevel, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val style = level.style()
    val description = stringResource(style.description)
    Box(
        modifier = modifier.size(size).background(style.color, style.shape).semantics { contentDescription = description },
        contentAlignment = if (level == RiskLevel.YELLOW) Alignment.BottomCenter else Alignment.Center,
    ) {
        Icon(
            style.symbol,
            contentDescription = null,
            tint = style.onColor,
            modifier = Modifier.size(
                size * if (level == RiskLevel.YELLOW) 0.5f else 0.62f,
            ),
        )
    }
}

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    val style = level.style()
    Surface(modifier = modifier, shape = MaterialTheme.shapes.small, color = style.color.copy(alpha = 0.16f)) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.s, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            RiskSymbol(level, size = 16.dp)
            Text(stringResource(style.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** Map legend (spec §4 Ecrã 3). */
@Composable
fun RiskLegend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RiskLevel.entries.forEach { level ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    RiskSymbol(level, size = 16.dp)
                    Text(stringResource(level.style().label), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
