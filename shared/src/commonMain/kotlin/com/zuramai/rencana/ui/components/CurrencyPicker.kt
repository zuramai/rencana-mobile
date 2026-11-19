package com.zuramai.rencana.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.platform.currencyOptions
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.platform.exposeTestTags

/**
 * Searchable currency list in a `ModalBottomSheet`. Matches code or localised name. [onSelect] gets the ISO code
 * and the sheet is dismissed through [onDismiss] afterwards (call it yourself or rely on this: it is invoked after select).
 * Test tags: `currency_search`, `currency_<CODE>`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val lang = LocalLanguage.current
    val options = remember(lang) { currencyOptions(lang) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, options) {
        val q = query.trim()
        if (q.isEmpty()) options else options.filter { it.first.contains(q, ignoreCase = true) || it.second.contains(q, ignoreCase = true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = RColors.Background) {
        Column(Modifier.exposeTestTags().padding(horizontal = 20.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RencanaTextField(query, { query = it }, Modifier.testTag("currency_search"), placeholder = "${t(S.COMMON_SEARCH)}: IDR, JPY…")
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                items(filtered, key = { it.first }) { (code, name) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(code); onDismiss() }.padding(vertical = 12.dp).testTag("currency_$code"),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(code, style = if (code == selected) RencanaType.current.bodySemi.copy(color = RColors.Primary) else RencanaType.current.bodySemi, modifier = Modifier.padding(end = 4.dp))
                        Text(name, style = RencanaType.current.body.copy(color = RColors.Muted))
                    }
                }
                if (filtered.isEmpty()) item { Text(t(S.COMMON_NO_RESULTS), style = RencanaType.current.body.copy(color = RColors.Muted), modifier = Modifier.padding(vertical = 16.dp)) }
            }
        }
    }
}
