package com.womenriskmap.feature.map.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.womenriskmap.core.designsystem.icons.AppIcons
import com.womenriskmap.core.designsystem.resources.*
import com.womenriskmap.core.designsystem.theme.Spacing
import com.womenriskmap.core.domain.model.Place
import com.womenriskmap.feature.map.ui.screens.SearchState
import org.jetbrains.compose.resources.stringResource

/** Spec §4 Ecrã 3: search bar on top, filters button next to it. */
@Composable
fun MapSearchBar(
    search: SearchState,
    filtersActive: Boolean,
    onQueryChange: (String) -> Unit,
    onPlaceSelected: (Place) -> Unit,
    onFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focus = LocalFocusManager.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Surface(
                shape = CircleShape,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f),
            ) {
                TextField(
                    value = search.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(Res.string.map_search_hint), maxLines = 1) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = if (search.query.isNotEmpty()) {
                        { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Clear, stringResource(Res.string.cancel)) } }
                    } else {
                        null
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Surface(shape = CircleShape, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                IconButton(onClick = onFilters, modifier = Modifier.size(56.dp)) {
                    BadgedBox(badge = { if (filtersActive) Badge() }) {
                        Icon(
                            AppIcons.Tune,
                            contentDescription = stringResource(
                                if (filtersActive) Res.string.map_filters_active else Res.string.map_filters,
                            ),
                        )
                    }
                }
            }
        }
        AnimatedVisibility(search.query.isNotBlank(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Surface(shape = MaterialTheme.shapes.large, shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column {
                    if (search.searching) LinearProgressIndicator(Modifier.fillMaxWidth())
                    if (!search.searching && search.results.isEmpty()) {
                        Text(
                            stringResource(Res.string.map_no_results),
                            modifier = Modifier.padding(Spacing.l),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(search.results) { place ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    focus.clearFocus()
                                    onPlaceSelected(place)
                                }.padding(horizontal = Spacing.l, vertical = Spacing.m),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
                            ) {
                                Icon(Icons.Filled.Place, null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(place.name, style = MaterialTheme.typography.bodyLarge)
                                    place.detail?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
