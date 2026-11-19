package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.data.ItemDto
import com.zuramai.rencana.data.ItemInput
import com.zuramai.rencana.data.UploadFile
import com.zuramai.rencana.domain.ITEM_TYPES
import com.zuramai.rencana.domain.amountToInput
import com.zuramai.rencana.domain.normalizeAmount
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.AuthImage
import com.zuramai.rencana.ui.components.ConfirmDeleteDialog
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.PillStyle
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.RencanaTextField
import com.zuramai.rencana.ui.components.apiErrorKey
import com.zuramai.rencana.ui.components.publicImageLoader
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

/** Create/edit/delete an itinerary item (web ItineraryItemForm). [defaultStart] is `YYYY-MM-DDTHH:mm`. */
@Composable
internal fun ItemFormSheet(
    planId: String,
    budgetCurrency: String,
    item: ItemDto?,
    defaultStart: String,
    onDismiss: () -> Unit,
    onSaved: (S) -> Unit
) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var type by remember { mutableStateOf(item?.type ?: "activity") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var allDay by remember { mutableStateOf(item?.allDay ?: false) }
    var startDate by remember { mutableStateOf((item?.startsAt ?: defaultStart).take(10)) }
    var startTime by remember { mutableStateOf(if (item == null || item.allDay) "09:00" else item.startsAt.drop(11)) }
    var hasEnd by remember { mutableStateOf(item?.endsAt != null) }
    var endDate by remember { mutableStateOf((item?.endsAt ?: item?.startsAt ?: defaultStart).take(10)) }
    var endTime by remember { mutableStateOf(if (item?.endsAt == null || item.allDay) "10:00" else item.endsAt.drop(11)) }
    var currency by remember { mutableStateOf(item?.currency ?: budgetCurrency) }
    var amount by remember { mutableStateOf(amountToInput(item?.price)) }
    var note by remember { mutableStateOf(item?.note ?: "") }
    var link by remember { mutableStateOf(item?.link ?: "") }
    var picked by remember { mutableStateOf<UploadFile?>(null) }
    var removeImage by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<S?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val picker = rememberFilePickerLauncher(type = FileKitType.Image) { file ->
        if (file != null) scope.launch {
            try {
                picked = file.toJpegUpload()
                removeImage = false
            } catch (e: Exception) {
                error = S.COMMON_SOMETHING_WRONG
            }
        }
    }

    fun wall(date: String, time: String) = if (allDay) "${date}T00:00" else "${date}T$time"

    fun save() {
        submitted = true
        val price = normalizeAmount(amount)
        val starts = wall(startDate, startTime)
        val ends = if (hasEnd) wall(endDate, endTime) else null
        val priceInvalid = amount.isNotBlank() && price == null
        if (description.isBlank() || priceInvalid || (ends != null && ends < starts)) {
            error = S.ERRORS_VALIDATION
            return
        }
        error = null
        val input = ItemInput(
            type = type, description = description.trim(), allDay = allDay, startsAt = starts, endsAt = ends,
            currency = if (price == null) null else currency, price = price,
            note = note.trim(), link = link.trim(),
            removeImage = if (item != null && removeImage) true else null
        )
        pending = true
        scope.launch {
            try {
                if (item != null) container.plans.updateItem(planId, item.id, input, picked)
                else container.plans.createItem(planId, input, picked)
                onSaved(S.ITINERARY_SAVED)
                onDismiss()
            } catch (e: Exception) {
                error = apiErrorKey(e)
                pending = false
            }
        }
    }

    fun delete() {
        if (item == null) return
        pending = true
        scope.launch {
            try {
                container.plans.deleteItem(planId, item.id)
                confirmDelete = false
                onSaved(S.ITINERARY_DELETED)
                onDismiss()
            } catch (e: Exception) {
                confirmDelete = false
                error = apiErrorKey(e)
                pending = false
            }
        }
    }

    FormSheet(
        title = t(if (item != null) S.ITINERARY_EDIT_ACTIVITY else S.ITINERARY_NEW_ACTIVITY),
        onDismiss = { if (!pending) onDismiss() },
        footer = {
            FullPillButton(t(S.COMMON_SAVE), ::save, Modifier.testTag("item_save"), loading = pending)
            if (item != null) FullPillButton(t(S.COMMON_DELETE), { confirmDelete = true }, Modifier.testTag("item_delete"), style = PillStyle.Secondary, enabled = !pending)
        }
    ) {
        error?.let { ErrorText(t(it)) }
        Labeled(t(S.ITINERARY_TYPE)) {
            val rows = ITEM_TYPES.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (row in rows) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (ty in row) {
                        val on = type == ty.value
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                .background(if (on) Tint else SoftBg)
                                .clickable { type = ty.value }.padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TypeChip(ty.value)
                            Text(t(ty.label), style = RencanaType.current.caption.copy(color = if (on) RColors.Text else RColors.Muted))
                        }
                    }
                }
            }
        }
        RencanaTextField(
            description, { description = it.take(200) }, Modifier.testTag("item_description"),
            label = t(S.ITINERARY_DESCRIPTION), placeholder = t(S.ITINERARY_DESCRIPTION_PLACEHOLDER),
            isError = submitted && description.isBlank(), capitalization = KeyboardCapitalization.Sentences
        )
        SwitchRow(t(S.ITINERARY_ALL_DAY), allDay, { allDay = it })
        Labeled(t(S.ITINERARY_STARTS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(startDate, { startDate = it }, Modifier.weight(1f).testTag("item_start_date"))
                if (!allDay) TimeField(startTime, { startTime = it }, Modifier.weight(0.7f).testTag("item_start_time"))
            }
        }
        SwitchRow(t(S.ITINERARY_ADD_END_TIME), hasEnd, { hasEnd = it })
        if (hasEnd) Labeled(t(S.ITINERARY_ENDS)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateField(endDate, { endDate = it }, Modifier.weight(1f).testTag("item_end_date"))
                if (!allDay) TimeField(endTime, { endTime = it }, Modifier.weight(0.7f).testTag("item_end_time"))
            }
            if (submitted && wall(endDate, endTime) < wall(startDate, startTime)) {
                Text(t(S.ERRORS_VALIDATION), style = RencanaType.current.caption.copy(color = RColors.Danger))
            }
        }
        Labeled(t(S.ITINERARY_PRICE)) {
            MoneyField(
                currency, amount, { currency = it }, { amount = it }, "item_price",
                placeholder = t(S.COMMON_OPTIONAL), isError = submitted && amount.isNotBlank() && normalizeAmount(amount) == null
            )
        }
        RencanaTextField(
            note, { note = it.take(2000) }, Modifier.testTag("item_note"),
            label = t(S.ITINERARY_NOTE), singleLine = false, minLines = 2, capitalization = KeyboardCapitalization.Sentences
        )
        RencanaTextField(
            link, { link = it }, Modifier.testTag("item_link"),
            label = t(S.ITINERARY_LINK_LABEL), placeholder = "https://", keyboardType = KeyboardType.Uri
        )
        Labeled(t(S.ITINERARY_IMAGE)) {
            ImageField(picked, item?.imageFileId?.takeIf { !removeImage }, { picker.launch() }, { picked = null; removeImage = true })
        }
    }

    if (confirmDelete && item != null) {
        ConfirmDeleteDialog(
            t(S.ITINERARY_DELETE_TITLE), t(S.ITINERARY_DELETE_TEXT, "name" to item.description),
            onConfirm = ::delete, onDismiss = { confirmDelete = false }, loading = pending
        )
    }
}

/** Single optional image: stored file, newly picked file or none (web ImageField). */
@Composable
private fun ImageField(picked: UploadFile?, existingId: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    val type = RencanaType.current
    if (picked != null || existingId != null) {
        Surface(shape = RoundedCornerShape(16.dp), color = RColors.Surface, border = BorderStroke(1.5.dp, RColors.Border)) {
            Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val m = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                if (picked != null) AsyncImage(picked.bytes, null, modifier = m, imageLoader = publicImageLoader(), contentScale = ContentScale.Crop)
                else AuthImage(existingId!!, m, thumb = true)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (picked != null) Text(picked.name, style = type.bodySemi, maxLines = 1)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(t(S.COMMON_REPLACE), style = type.bodySemi.copy(fontSize = 14.sp, color = RColors.Primary), modifier = Modifier.clickable(onClick = onPick).testTag("item_image_replace"))
                        Text(t(S.COMMON_REMOVE), style = type.bodySemi.copy(fontSize = 14.sp, color = RColors.Danger), modifier = Modifier.clickable(onClick = onRemove).testTag("item_image_remove"))
                    }
                }
            }
        }
    } else {
        Surface(
            onClick = onPick, shape = RoundedCornerShape(16.dp), color = Color(0xFFF0FDFA),
            border = BorderStroke(1.5.dp, RColors.Primary), modifier = Modifier.fillMaxWidth().height(74.dp).testTag("item_image_add")
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                Icon(Lucide.ImagePlus, null, tint = RColors.Primary, modifier = Modifier.size(20.dp))
                Text(t(S.ITINERARY_ADD_IMAGE), style = type.bodySemi.copy(color = RColors.Primary))
            }
        }
    }
}
