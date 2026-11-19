package com.zuramai.rencana.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType
import androidx.compose.foundation.layout.fillMaxWidth

/** Visual style of [PillButton]. */
enum class PillStyle { Primary, Secondary, Danger }

/**
 * 52 dp pill button. [loading] shows a spinner and blocks clicks. [leading] is an optional icon slot before the
 * text. Give it `Modifier.testTag(...)` through [modifier].
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) {
    val container = when (style) {
        PillStyle.Primary -> RColors.Primary
        PillStyle.Secondary -> RColors.Surface
        PillStyle.Danger -> RColors.Danger
    }
    val content = when (style) {
        PillStyle.Secondary -> RColors.Text
        else -> Color.White
    }
    Surface(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RShapes.Pill,
        color = container,
        contentColor = content,
        border = if (style == PillStyle.Secondary) BorderStroke(1.5.dp, RColors.Border) else null,
        modifier = modifier.heightIn(min = 52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp).heightIn(min = 52.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(color = content, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                leading?.invoke()
                Text(text, style = RencanaType.current.bodySemi, color = content.copy(alpha = if (enabled) 1f else 0.6f))
            }
        }
    }
}

/** Convenience: full-width [PillButton]. */
@Composable
fun FullPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) = PillButton(text, onClick, modifier.fillMaxWidth(), style, enabled, loading, leading)
