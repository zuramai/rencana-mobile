package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.zuramai.rencana.data.UploadFile
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RShapes
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.CurrencyPickerSheet
import com.zuramai.rencana.ui.components.RencanaTextField
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.datetime.LocalDate

/** Max PDF size accepted client-side (backend limit). */
internal const val MAX_PDF_BYTES = 15 * 1024 * 1024

/** Compresses a picked image to JPEG (max 2000 px, quality 90); this also converts iOS HEIC. */
internal suspend fun PlatformFile.toJpegUpload(): UploadFile {
    val bytes = FileKit.compressImage(readBytes(), imageFormat = ImageFormat.JPEG, quality = 90, maxWidth = 2000, maxHeight = 2000)
    val base = name.substringBeforeLast('.', "").ifEmpty { "photo" }
    return UploadFile("$base.jpg", "image/jpeg", bytes)
}

private const val DAY_MS = 86_400_000L

/** Read-only field that opens a picker when tapped. */
@Composable
private fun PickerField(text: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick, shape = RShapes.Field, color = RColors.Surface,
        border = BorderStroke(1.5.dp, RColors.Border), modifier = modifier.height(52.dp)
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = RencanaType.current.body)
        }
    }
}

/** Date field (`YYYY-MM-DD`) with an M3 date picker dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    PickerField(value, { open = true }, modifier)
    if (open) {
        val initial = runCatching { LocalDate.parse(value).toEpochDays() * DAY_MS }.getOrNull()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton({
                    state.selectedDateMillis?.let { onChange(LocalDate.fromEpochDays(it.floorDiv(DAY_MS)).toString()) }
                    open = false
                }) { Text(t(S.COMMON_SAVE)) }
            },
            dismissButton = { TextButton({ open = false }) { Text(t(S.COMMON_CANCEL)) } }
        ) { DatePicker(state) }
    }
}

/** Time field (`HH:mm`, 24 h) with an M3 time picker dialog. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    PickerField(value, { open = true }, modifier)
    if (open) {
        val state = rememberTimePickerState(
            initialHour = value.take(2).toIntOrNull() ?: 9,
            initialMinute = value.drop(3).take(2).toIntOrNull() ?: 0,
            is24Hour = true
        )
        BasicAlertDialog({ open = false }) {
            Surface(shape = RShapes.Dialog, color = RColors.Surface) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimePicker(state)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton({ open = false }) { Text(t(S.COMMON_CANCEL)) }
                        TextButton({
                            onChange(state.hour.toString().padStart(2, '0') + ":" + state.minute.toString().padStart(2, '0'))
                            open = false
                        }) { Text(t(S.COMMON_SAVE)) }
                    }
                }
            }
        }
    }
}

/**
 * Currency chip + amount field whose amount input carries [amountTag] (the chip carries `<amountTag>_currency`).
 */
@Composable
internal fun MoneyField(
    currency: String,
    amount: String,
    onCurrencyChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    amountTag: String,
    placeholder: String = "0",
    isError: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    var picking by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Surface(
            onClick = { picking = true }, shape = RShapes.Field, color = RColors.Surface,
            border = BorderStroke(1.5.dp, RColors.Border),
            modifier = Modifier.width(96.dp).height(52.dp).testTag("${amountTag}_currency")
        ) {
            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(currency, style = RencanaType.current.bodySemi)
                Icon(Lucide.ChevronDown, t(S.COMMON_CURRENCY), tint = RColors.Muted, modifier = Modifier.size(16.dp))
            }
        }
        RencanaTextField(
            value = amount,
            onValueChange = { v -> onAmountChange(v.filter { it.isDigit() || it == '.' || it == ',' || it == ' ' }) },
            modifier = Modifier.testTag(amountTag),
            containerModifier = Modifier.weight(1f),
            placeholder = placeholder, isError = isError, keyboardType = KeyboardType.Decimal, imeAction = imeAction
        )
    }
    if (picking) CurrencyPickerSheet(currency, onCurrencyChange, onDismiss = { picking = false })
}
