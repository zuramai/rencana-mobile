package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.domain.itemType
import com.zuramai.rencana.domain.ticketKind
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.platform.exposeTestTags

internal val Tint = Color(0xFFE0F2F1)
internal val SoftBg = Color(0xFFFAF6F0)

/** Bottom sheet with a title, scrollable [content] and a pinned [footer]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormSheet(
    title: String,
    onDismiss: () -> Unit,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = RColors.Background,
        shape = com.zuramai.rencana.ui.RShapes.Sheet
    ) {
        // Sheets are separate windows: test tags must be exposed again at their root.
        Column(Modifier.fillMaxWidth().exposeTestTags().imePadding().navigationBarsPadding()) {
            Text(title, style = RencanaType.current.title, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) { content() }
            if (footer != null) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { footer() }
            }
        }
    }
}

/** Pill selector (web SegmentedControl). */
@Composable
internal fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier, tag: (T) -> String? = { null }) {
    Row(
        modifier.fillMaxWidth().clip(CircleShape).background(RColors.Surface).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for ((value, label) in options) {
            val on = value == selected
            Box(
                Modifier.weight(1f).height(36.dp).clip(CircleShape)
                    .background(if (on) RColors.Primary else Color.Transparent)
                    .clickable { onSelect(value) }
                    .let { m -> tag(value)?.let { m.testTag(it) } ?: m },
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = RencanaType.current.bodySemi.copy(fontSize = 14.sp, color = if (on) Color.White else RColors.Muted))
            }
        }
    }
}

/** Round coloured type icon (web TypeChip). */
@Composable
internal fun TypeChip(type: String, size: androidx.compose.ui.unit.Dp = 36.dp) {
    val meta = itemType(type)
    Box(Modifier.size(size).background(meta.color.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(meta.icon, contentDescription = null, tint = meta.color, modifier = Modifier.size(size / 2))
    }
}

/** Ticket kind pill (web KindChip). */
@Composable
internal fun KindChip(kind: String, onDark: Boolean = false) {
    val k = ticketKind(kind)
    Row(
        Modifier.background(Tint, CircleShape).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(k.icon, null, tint = RColors.PrimaryDark, modifier = Modifier.size(14.dp))
        Text(t(k.label), style = RencanaType.current.caption.copy(fontSize = 12.sp, color = RColors.PrimaryDark))
    }
}

/** Red PDF placeholder tile showing the file name. */
@Composable
internal fun PdfTile(name: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color(0xFFFDECEC)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Lucide.FileText, null, tint = RColors.Danger, modifier = Modifier.size(32.dp))
        Text(
            name, style = RencanaType.current.caption.copy(fontSize = 12.sp), textAlign = TextAlign.Center,
            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp)
        )
    }
}

/** Selectable pill (cost presets, ticket kinds). */
@Composable
internal fun ChoicePill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) Tint else RColors.Surface,
        border = BorderStroke(1.5.dp, if (selected) RColors.Primary else RColors.Border)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp).height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) Icon(icon, null, tint = if (selected) RColors.PrimaryDark else RColors.Muted, modifier = Modifier.size(16.dp))
            Text(text, style = RencanaType.current.bodySemi.copy(fontSize = 14.sp, color = if (selected) RColors.PrimaryDark else RColors.Text))
        }
    }
}

/** Label + switch row. */
@Composable
internal fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, sub: String? = null, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f)) {
            Text(label, style = RencanaType.current.bodySemi)
            if (sub != null) Text(sub, style = RencanaType.current.caption)
        }
        Switch(
            checked, onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = RColors.Primary, checkedThumbColor = Color.White)
        )
    }
}


/** Wraps an optional edit target so "no sheet" (null holder) differs from "new item" (holder of null). */
class Holder<T>(val value: T?)

/** Caption label above [content]. */
@Composable
internal fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = RencanaType.current.caption)
        content()
    }
}
