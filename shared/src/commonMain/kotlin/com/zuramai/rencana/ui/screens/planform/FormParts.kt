package com.zuramai.rencana.ui.screens.planform

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType

/** Pill segmented control (web `SegmentedControl`). Tags: `<tag>_<value>`. */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "segment"
) {
    Row(
        modifier.fillMaxWidth().clip(RShapes.Pill).background(Color(0xFFFAF6F0)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for ((value, label) in options) {
            val active = value == selected
            Box(
                Modifier.weight(1f).height(36.dp).clip(RShapes.Pill)
                    .background(if (active) RColors.Primary else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(value) }
                    .testTag("${tag}_$value"),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = RencanaType.current.caption.copy(color = if (active) Color.White else RColors.Muted))
            }
        }
    }
}

/** Select-like field: bordered 52 dp box with a dropdown menu of [options] (value to label). */
@Composable
fun <T> DropdownField(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    tag: String? = null
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { open = true },
            shape = RShapes.Field, color = RColors.Surface,
            border = BorderStroke(1.5.dp, RColors.Border),
            modifier = Modifier.fillMaxWidth().height(52.dp).let { if (tag != null) it.testTag(tag) else it }
        ) {
            Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(options.firstOrNull { it.first == selected }?.second ?: "", style = RencanaType.current.body, maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Lucide.ChevronDown, contentDescription = null, tint = RColors.Muted, modifier = Modifier.size(16.dp))
            }
        }
        DropdownMenu(open, { open = false }, containerColor = RColors.Surface) {
            for ((value, label) in options) {
                DropdownMenuItem(
                    text = { Text(label, style = if (value == selected) RencanaType.current.bodySemi.copy(color = RColors.Primary) else RencanaType.current.body) },
                    onClick = { onSelect(value); open = false }
                )
            }
        }
    }
}

/** Column with a caption label above [content]. */
@Composable
fun LabeledColumn(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = RencanaType.current.caption)
        content()
    }
}

/** Round swatch. */
@Composable
fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.size(26.dp).clip(CircleShape).background(color)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) RColors.Primary else RColors.Border, CircleShape)
            .clickable(onClick = onClick)
    )
}
