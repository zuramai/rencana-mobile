package com.zuramai.rencana.ui.screens.planform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.platform.exposeTestTags
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.RencanaTextField
import kotlinx.datetime.TimeZone

private val allZoneIds: List<String> by lazy { TimeZone.availableZoneIds.sorted() }

/** Destination time zone field (tag `plan_timezone`); opens [TimeZonePickerSheet]. */
@Composable
fun TimeZoneField(value: String?, onChange: (String?) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth()) {
        RencanaTextField(
            value ?: t(S.PLAN_TIME_ZONE_NONE), {}, Modifier,
            label = t(S.PLAN_TIME_ZONE), supportingText = t(S.PLAN_TIME_ZONE_HINT), readOnly = true,
            trailing = { Icon(Lucide.ChevronDown, null, tint = RColors.Muted, modifier = Modifier.size(18.dp)) }
        )
        Box(Modifier.matchParentSize().clickable { open = true }.testTag("plan_timezone"))
    }
    if (open) TimeZonePickerSheet(value, onChange, { open = false })
}

/** Searchable IANA zone list plus a "not set" row. Tags: `timezone_search`, `timezone_none`, `timezone_<id with / as _>`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeZonePickerSheet(selected: String?, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().replace(' ', '_')
        if (q.isEmpty()) allZoneIds else allZoneIds.filter { it.contains(q, ignoreCase = true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = RColors.Background) {
        Column(Modifier.exposeTestTags().padding(horizontal = 20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RencanaTextField(query, { query = it }, Modifier.testTag("timezone_search"), placeholder = "${t(S.COMMON_SEARCH)}: Asia/Tokyo…")
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                if (query.isBlank()) item {
                    Text(
                        t(S.PLAN_TIME_ZONE_NONE),
                        style = if (selected == null) RencanaType.current.bodySemi.copy(color = RColors.Primary) else RencanaType.current.bodySemi,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(null); onDismiss() }.padding(vertical = 12.dp).testTag("timezone_none")
                    )
                }
                items(filtered, key = { it }) { id ->
                    Text(
                        id,
                        style = if (id == selected) RencanaType.current.bodySemi.copy(color = RColors.Primary) else RencanaType.current.body,
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(id); onDismiss() }.padding(vertical = 12.dp).testTag("timezone_${id.replace('/', '_')}")
                    )
                }
                if (filtered.isEmpty()) item { Text(t(S.COMMON_NO_RESULTS), style = RencanaType.current.body.copy(color = RColors.Muted), modifier = Modifier.padding(vertical = 16.dp)) }
            }
        }
    }
}
