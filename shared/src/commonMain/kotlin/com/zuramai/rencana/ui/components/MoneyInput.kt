package com.zuramai.rencana.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType

/**
 * Currency chip (opens [CurrencyPickerSheet]) + decimal amount field. [amount] is the raw text the user typed
 * (digits, `.`, `,` and spaces only); convert it for the API with `normalizeAmount(amount)` at submit time, and
 * prefill from the API with `amountToInput(value)`. Test tags: `<tagPrefix>_currency`, `<tagPrefix>_amount`.
 */
@Composable
fun MoneyInput(
    currency: String,
    amount: String,
    onCurrencyChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String = "0",
    isError: Boolean = false,
    tagPrefix: String = "money",
    imeAction: ImeAction = ImeAction.Next
) {
    var picking by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) Text(label, style = RencanaType.current.caption)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Surface(
                onClick = { picking = true },
                shape = RShapes.Field, color = RColors.Surface,
                border = BorderStroke(1.5.dp, RColors.Border),
                modifier = Modifier.width(96.dp).height(52.dp).testTag("${tagPrefix}_currency")
            ) {
                Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(currency, style = RencanaType.current.bodySemi)
                    Icon(Lucide.ChevronDown, contentDescription = t(S.COMMON_CURRENCY), tint = RColors.Muted, modifier = Modifier.size(16.dp))
                }
            }
            RencanaTextField(
                value = amount,
                onValueChange = { v -> onAmountChange(v.filter { it.isDigit() || it == '.' || it == ',' || it == ' ' }) },
                modifier = Modifier.testTag("${tagPrefix}_amount"),
                containerModifier = Modifier.weight(1f),
                placeholder = placeholder,
                isError = isError,
                keyboardType = KeyboardType.Decimal,
                imeAction = imeAction
            )
        }
    }
    if (picking) CurrencyPickerSheet(currency, onCurrencyChange, onDismiss = { picking = false })
}
