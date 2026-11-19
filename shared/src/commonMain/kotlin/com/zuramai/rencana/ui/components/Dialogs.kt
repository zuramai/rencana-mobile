package com.zuramai.rencana.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Trash2
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.platform.exposeTestTags

/**
 * Delete confirmation dialog (design 15): red trash badge, [title], [text], Cancel / Delete.
 * Test tags: `confirm_delete` (the Delete button), `confirm_cancel`.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDeleteDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    loading: Boolean = false
) {
    BasicAlertDialog(onDismissRequest = { if (!loading) onDismiss() }) {
        Surface(Modifier.exposeTestTags(), shape = RShapes.Dialog, color = RColors.Surface) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(Modifier.size(56.dp).background(RColors.DangerSoft, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Lucide.Trash2, contentDescription = null, tint = RColors.Danger, modifier = Modifier.size(26.dp))
                }
                Text(title, style = RencanaType.current.title, textAlign = TextAlign.Center)
                Text(text, style = RencanaType.current.body.copy(color = RColors.Muted), textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(t(S.COMMON_CANCEL), onDismiss, Modifier.weight(1f).testTag("confirm_cancel"), style = PillStyle.Secondary, enabled = !loading)
                    PillButton(t(S.COMMON_DELETE), onConfirm, Modifier.weight(1f).testTag("confirm_delete"), style = PillStyle.Danger, loading = loading)
                }
            }
        }
    }
}

/**
 * Generic two-button confirmation (e.g. changing the server). [confirmLabel] defaults to "Save".
 * Test tags: `confirm_ok`, `confirm_cancel`.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ConfirmDialog(
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmLabel: String = t(S.COMMON_SAVE)
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(Modifier.exposeTestTags(), shape = RShapes.Dialog, color = RColors.Surface) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(text, style = RencanaType.current.body, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton(t(S.COMMON_CANCEL), onDismiss, Modifier.weight(1f).testTag("confirm_cancel"), style = PillStyle.Secondary)
                    PillButton(confirmLabel, onConfirm, Modifier.weight(1f).testTag("confirm_ok"))
                }
            }
        }
    }
}

/** Centered mascot + [title] + [text] empty state with an optional [action] slot below (design 04/07). */
@Composable
fun EmptyState(
    pose: MascotPose,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    mascotSize: Dp = 160.dp,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Mascot(pose, size = mascotSize)
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = RencanaType.current.title, textAlign = TextAlign.Center)
            Text(text, style = RencanaType.current.body.copy(color = RColors.Muted), textAlign = TextAlign.Center)
        }
        action?.invoke()
    }
}
