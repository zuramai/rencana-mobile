package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.CalendarPlus
import com.composables.icons.lucide.Link
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.data.ItemDto
import com.zuramai.rencana.domain.formatDayLabel
import com.zuramai.rencana.domain.googleCalendarUrl
import com.zuramai.rencana.domain.toCalendarItem
import com.zuramai.rencana.domain.groupByDay
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.platform.formatMoney
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.AuthImage
import com.zuramai.rencana.ui.components.EmptyState
import com.zuramai.rencana.ui.components.MascotPose
import com.zuramai.rencana.ui.components.PillButton
import com.zuramai.rencana.ui.components.RCard

/** Day-grouped itinerary (web ItineraryList). */
@Composable
internal fun ItineraryTab(items: List<ItemDto>, timeZone: String?, onOpen: (ItemDto) -> Unit, onAdd: () -> Unit) {
    val lang = LocalLanguage.current
    if (items.isEmpty()) {
        EmptyState(
            MascotPose.Map, t(S.ITINERARY_EMPTY_TITLE), t(S.ITINERARY_EMPTY_TEXT), mascotSize = 136.dp,
            action = { PillButton(t(S.ITINERARY_ADD_ACTIVITY), onAdd, Modifier.width(220.dp)) }
        )
        return
    }
    val type = RencanaType.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (day in groupByDay(items) { it.startsAt }) {
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(t(S.ITINERARY_DAY, "n" to day.dayNumber), style = type.bodySemi.copy(fontSize = 14.sp, color = RColors.PrimaryDark))
                Text("·", style = type.bodySemi.copy(fontSize = 14.sp, color = RColors.Muted))
                Text(formatDayLabel(day.date, lang), style = type.bodySemi.copy(fontSize = 14.sp, color = RColors.Muted))
            }
            for (item in day.items) ItemCard(item, lang, timeZone) { onOpen(item) }
        }
    }
}

@Composable
private fun ItemCard(item: ItemDto, lang: String, timeZone: String?, onClick: () -> Unit) {
    val type = RencanaType.current
    val startTime = item.startsAt.drop(11)
    val end = item.endsAt
    val endLabel = when {
        end == null || item.allDay -> null
        end.take(10) == item.startsAt.take(10) -> end.drop(11)
        else -> "→ " + formatDayLabel(end.take(10), lang).substringAfter(", ")
    }
    val uri = LocalUriHandler.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.width(50.dp).padding(top = 14.dp)) {
            Text(if (item.allDay) t(S.ITINERARY_ALL_DAY) else startTime, style = type.caption.copy(color = RColors.Text))
            if (endLabel != null) Text(endLabel, style = type.caption.copy(fontSize = 12.sp))
        }
        RCard(Modifier.weight(1f).testTag("item_card"), onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                TypeChip(item.type)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.description, style = type.bodySemi)
                    val price = item.price?.toDoubleOrNull()
                    if ((price != null && item.currency != null) || item.link != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (price != null && item.currency != null) {
                                Text(
                                    formatMoney(price, item.currency, lang),
                                    style = type.caption.copy(fontSize = 12.sp, color = RColors.PrimaryDark),
                                    modifier = Modifier.background(Tint, RoundedCornerShape(percent = 50)).padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (item.link != null) {
                                Row(
                                    Modifier.clickable { runCatching { uri.openUri(item.link) } },
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Lucide.Link, null, tint = RColors.Primary, modifier = Modifier.size(14.dp))
                                    Text(t(S.ITINERARY_LINK), style = type.caption.copy(fontSize = 12.sp, color = RColors.Primary))
                                }
                            }
                        }
                    }
                    Row(
                        Modifier.clickable { runCatching { uri.openUri(googleCalendarUrl(item.toCalendarItem(), timeZone)) } }.testTag("item_gcal_${item.id}"),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Lucide.CalendarPlus, null, tint = RColors.Primary, modifier = Modifier.size(14.dp))
                        Text(t(S.CALENDAR_ADD_TO_GOOGLE), style = type.caption.copy(fontSize = 12.sp, color = RColors.Primary))
                    }
                    if (!item.note.isNullOrBlank()) {
                        Text(item.note, style = type.body.copy(fontSize = 14.sp, color = RColors.Muted), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (item.imageFileId != null) {
                    AuthImage(item.imageFileId, Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)), thumb = true)
                }
            }
        }
    }
}
