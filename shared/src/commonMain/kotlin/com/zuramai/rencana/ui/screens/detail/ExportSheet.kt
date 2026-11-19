package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.CalendarPlus
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.FileSpreadsheet
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Ticket
import com.zuramai.rencana.data.ExportKind
import com.zuramai.rencana.data.FileOpener
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.ErrorText
import kotlinx.coroutines.launch

private class ExportRow(val kind: ExportKind, val icon: ImageVector, val color: Color, val title: S, val sub: S, val file: String, val disabled: Boolean)

/** Export sheet (web ExportSheet): download via the API, then open with the system viewer. */
@Composable
internal fun ExportSheet(planId: String, hasTickets: Boolean, onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf<ExportKind?>(null) }
    var failed by remember { mutableStateOf(false) }
    val rows = listOf(
        ExportRow(ExportKind.ItineraryPdf, Lucide.FileText, Color(0xFFEF4444), S.EXPORT_ITINERARY_PDF, S.EXPORT_ITINERARY_PDF_SUB, "itinerary.pdf", false),
        ExportRow(ExportKind.ItineraryXlsx, Lucide.FileSpreadsheet, Color(0xFF16A34A), S.EXPORT_ITINERARY_XLSX, S.EXPORT_ITINERARY_XLSX_SUB, "itinerary.xlsx", false),
        ExportRow(ExportKind.TicketsPdf, Lucide.Ticket, Color(0xFF3B82F6), S.EXPORT_TICKETS_PDF, if (hasTickets) S.EXPORT_TICKETS_PDF_SUB else S.EXPORT_NO_TICKETS, "tickets.pdf", !hasTickets),
        ExportRow(ExportKind.CalendarIcs, Lucide.CalendarPlus, Color(0xFF0EA5E9), S.EXPORT_CALENDAR_ICS, S.EXPORT_CALENDAR_ICS_SUB, "itinerary.ics", false)
    )
    FormSheet(t(S.EXPORT_TITLE), onDismiss) {
        if (failed) ErrorText(t(S.COMMON_OPEN_FAILED))
        Column(Modifier.padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            for (row in rows) {
                Row(
                    Modifier.fillMaxWidth().alpha(if (row.disabled) 0.5f else 1f)
                        .background(SoftBg, RoundedCornerShape(16.dp))
                        .clickable(enabled = !row.disabled && busy == null) {
                            failed = false
                            busy = row.kind
                            scope.launch {
                                try {
                                    FileOpener.saveAndOpen(container.files.export(planId, row.kind), row.file)
                                } catch (e: Exception) {
                                    failed = true
                                } finally {
                                    busy = null
                                }
                            }
                        }.padding(12.dp).testTag("export_${row.kind.wire}"),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(44.dp).background(row.color.copy(alpha = 0.13f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Icon(row.icon, null, tint = row.color, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(t(row.title), style = RencanaType.current.bodySemi)
                        Text(t(row.sub), style = RencanaType.current.caption)
                    }
                    if (busy == row.kind) CircularProgressIndicator(Modifier.size(20.dp), color = RColors.Primary, strokeWidth = 2.dp)
                    else Icon(Lucide.Download, null, tint = RColors.Muted, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
