package com.zuramai.rencana.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes

/**
 * White card: radius 20, 16 dp padding, soft shadow. Pass [onClick] to make it clickable
 * (give it `Modifier.testTag(...)` via [modifier]).
 */
@Composable
fun RCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Surface(
            onClick = onClick, modifier = modifier, shape = RShapes.Card, color = RColors.Surface,
            shadowElevation = 6.dp, tonalElevation = 0.dp
        ) { body() }
    } else {
        Surface(
            modifier = modifier, shape = RShapes.Card, color = RColors.Surface,
            shadowElevation = 6.dp, tonalElevation = 0.dp
        ) { body() }
    }
}
