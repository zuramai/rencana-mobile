package com.zuramai.rencana.ui.screens.travel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Trash2
import com.zuramai.rencana.data.PlanListItem
import com.zuramai.rencana.domain.formatPeriod
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.PlanCover
import com.zuramai.rencana.ui.components.RCard

/** Plan row card (web `PlanCard`): cover, name, period, edit and delete actions. Tag `plan_card`. */
@Composable
fun PlanCard(plan: PlanListItem, onOpen: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val lang = LocalLanguage.current
    val label = formatPeriod(plan.startYear, plan.startMonth, plan.endYear, plan.endMonth, lang) ?: t(S.TRAVEL_NO_DATE)
    RCard(modifier.fillMaxWidth().testTag("plan_card"), onClick = onOpen, contentPadding = PaddingValues(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlanCover(plan)
            Column(Modifier.weight(1f)) {
                Text(plan.name, style = RencanaType.current.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(label, style = RencanaType.current.caption.copy(color = RColors.Muted))
            }
            Row {
                IconButton(onEdit, Modifier.size(36.dp).testTag("plan_edit")) {
                    Icon(Lucide.Pencil, contentDescription = t(S.COMMON_EDIT), tint = RColors.Muted, modifier = Modifier.size(18.dp))
                }
                IconButton(onDelete, Modifier.size(36.dp).testTag("plan_delete")) {
                    Icon(Lucide.Trash2, contentDescription = t(S.COMMON_DELETE), tint = RColors.Muted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
