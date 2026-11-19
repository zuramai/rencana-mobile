package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MapPin
import com.zuramai.rencana.data.TicketDto
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.AuthImage
import com.zuramai.rencana.ui.components.EmptyState
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.MascotPose
import com.zuramai.rencana.ui.components.PillButton
import com.zuramai.rencana.ui.components.PillStyle
import com.zuramai.rencana.ui.components.RCard

/** Two-column ticket grid (web TicketGrid). */
@Composable
internal fun TicketsTab(tickets: List<TicketDto>, onOpen: (TicketDto) -> Unit, onAdd: () -> Unit) {
    if (tickets.isEmpty()) {
        EmptyState(
            MascotPose.Map, t(S.TICKETS_EMPTY_TITLE), t(S.TICKETS_EMPTY_TEXT), mascotSize = 136.dp,
            action = { PillButton(t(S.TICKETS_ADD_TICKET), onAdd, Modifier.width(220.dp).testTag("ticket_add")) }
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            for (c in 0..1) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tickets.filterIndexed { i, _ -> i % 2 == c }.forEach { TicketCard(it) { onOpen(it) } }
                }
            }
        }
        FullPillButton(t(S.TICKETS_ADD_TICKET), onAdd, Modifier.testTag("ticket_add"), style = PillStyle.Secondary)
    }
}

@Composable
private fun TicketCard(ticket: TicketDto, onClick: () -> Unit) {
    val type = RencanaType.current
    val first = ticket.files.firstOrNull()
    RCard(Modifier.fillMaxWidth().testTag("ticket_card"), onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Box(Modifier.fillMaxWidth().height(110.dp).background(Color(0xFFF1F1F1))) {
            when {
                first != null && first.contentType.startsWith("image/") -> AuthImage(first.id, Modifier.fillMaxSize(), thumb = true)
                first != null -> PdfTile(first.originalName, Modifier.fillMaxSize())
            }
        }
        Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            KindChip(ticket.kind)
            Text(ticket.title, style = type.bodySemi)
            if (!ticket.locationText.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Lucide.MapPin, null, tint = RColors.Muted, modifier = Modifier.size(14.dp))
                    Text(ticket.locationText, style = type.caption.copy(fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
