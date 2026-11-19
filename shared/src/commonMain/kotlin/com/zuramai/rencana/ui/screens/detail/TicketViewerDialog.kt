package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.composables.icons.lucide.X
import com.zuramai.rencana.data.FileOpener
import com.zuramai.rencana.data.TicketDto
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.AuthImage
import com.zuramai.rencana.ui.components.ConfirmDeleteDialog
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.PillButton
import com.zuramai.rencana.ui.components.PillStyle
import com.zuramai.rencana.ui.components.apiErrorKey
import com.zuramai.rencana.platform.exposeTestTags
import kotlinx.coroutines.launch

private val ViewerBg = Color(0xFF0C0A09)
private val ViewerMuted = Color(0xFFD6D3D1)

/** Full-screen ticket viewer (web TicketViewer). */
@Composable
internal fun TicketViewerDialog(planId: String, ticket: TicketDto, onDismiss: () -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val uri = LocalUriHandler.current
    val type = RencanaType.current
    val pager = rememberPagerState { ticket.files.size.coerceAtLeast(1) }
    var confirm by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<S?>(null) }

    fun openPdf(id: String, name: String) {
        scope.launch {
            try {
                FileOpener.saveAndOpen(container.files.file(id), name)
                error = null
            } catch (e: Exception) {
                error = S.COMMON_OPEN_FAILED
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().exposeTestTags().background(ViewerBg).safeDrawingPadding().testTag("ticket_viewer")) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable(onClick = onDismiss).testTag("ticket_close"),
                        contentAlignment = Alignment.Center
                    ) { Icon(Lucide.X, t(S.COMMON_CLOSE), tint = Color.White, modifier = Modifier.size(20.dp)) }
                    Box(Modifier.weight(1f))
                    if (ticket.files.isNotEmpty()) {
                        Text("${pager.currentPage + 1} / ${ticket.files.size}", style = type.bodySemi.copy(fontSize = 14.sp, color = Color.White))
                    }
                }
                HorizontalPager(pager, Modifier.fillMaxWidth().padding(top = 4.dp), pageSpacing = 0.dp) { page ->
                    val file = ticket.files.getOrNull(page)
                    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                        if (file != null) {
                            if (file.contentType == "application/pdf") {
                                PdfTile(
                                    file.originalName,
                                    Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(20.dp)).clickable { openPdf(file.id, file.originalName) }.testTag("ticket_pdf")
                                )
                            } else {
                                AuthImage(file.id, Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(20.dp)), thumb = false, contentScale = ContentScale.Fit, contentDescription = ticket.title)
                            }
                        }
                    }
                }
                if (ticket.files.size > 1) {
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                        for (i in ticket.files.indices) {
                            Box(Modifier.size(8.dp).background(Color.White.copy(alpha = if (i == pager.currentPage) 1f else 0.4f), CircleShape))
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    error?.let { ErrorText(t(it)) }
                    KindChip(ticket.kind)
                    Text(ticket.title, style = type.title.copy(color = Color.White))
                    if (!ticket.description.isNullOrBlank()) Text(ticket.description, style = type.body.copy(fontSize = 14.sp, color = ViewerMuted))
                    val url = ticket.locationUrl
                    if (!url.isNullOrBlank()) {
                        Row(
                            Modifier.clickable { runCatching { uri.openUri(url) } },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Lucide.MapPin, null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(16.dp))
                            Text(ticket.locationText?.ifBlank { null } ?: url, style = type.bodySemi.copy(fontSize = 14.sp, color = Color(0xFF2DD4BF)))
                            Icon(Lucide.ExternalLink, null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(14.dp))
                        }
                    } else if (!ticket.locationText.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Lucide.MapPin, null, tint = ViewerMuted, modifier = Modifier.size(16.dp))
                            Text(ticket.locationText, style = type.bodySemi.copy(fontSize = 14.sp, color = ViewerMuted))
                        }
                    }
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PillButton(t(S.COMMON_EDIT), onEdit, Modifier.weight(1f).testTag("ticket_edit"), style = PillStyle.Secondary)
                        PillButton(t(S.COMMON_DELETE), { confirm = true }, Modifier.weight(1f).testTag("ticket_delete"), style = PillStyle.Danger)
                    }
                }
            }
        }
    }

    if (confirm) {
        ConfirmDeleteDialog(
            t(S.TICKETS_DELETE_TITLE), t(S.TICKETS_DELETE_TEXT, "name" to ticket.title),
            onConfirm = {
                pending = true
                scope.launch {
                    try {
                        container.plans.deleteTicket(planId, ticket.id)
                        confirm = false
                        onDeleted()
                    } catch (e: Exception) {
                        confirm = false
                        error = apiErrorKey(e)
                        pending = false
                    }
                }
            },
            onDismiss = { confirm = false }, loading = pending
        )
    }
}
