package com.zuramai.rencana.ui.screens.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.zuramai.rencana.data.TicketDto
import com.zuramai.rencana.data.TicketInput
import com.zuramai.rencana.data.UploadFile
import com.zuramai.rencana.domain.TICKET_KINDS
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.AuthImage
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.PillButton
import com.zuramai.rencana.ui.components.PillStyle
import com.zuramai.rencana.ui.components.RencanaTextField
import com.zuramai.rencana.ui.components.apiErrorKey
import com.zuramai.rencana.ui.components.publicImageLoader
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch

private class AddedFile(val key: Int, val file: UploadFile) {
    val isImage get() = file.contentType.startsWith("image/")
}

/** Create/edit a ticket (web TicketForm). Photos are compressed to JPEG; PDFs over 15 MB are rejected. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TicketFormSheet(planId: String, ticket: TicketDto?, onDismiss: () -> Unit, onSaved: (S) -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()

    var kind by remember { mutableStateOf(ticket?.kind ?: "flight") }
    var title by remember { mutableStateOf(ticket?.title ?: "") }
    var description by remember { mutableStateOf(ticket?.description ?: "") }
    var locationText by remember { mutableStateOf(ticket?.locationText ?: "") }
    var locationUrl by remember { mutableStateOf(ticket?.locationUrl ?: "") }
    val removed = remember { mutableStateListOf<String>() }
    val added = remember { mutableStateListOf<AddedFile>() }
    var seq by remember { mutableStateOf(0) }
    var pending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<S?>(null) }
    var submitted by remember { mutableStateOf(false) }

    val existing = ticket?.files?.filter { it.id !in removed } ?: emptyList()

    val photoPicker = rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Multiple()) { files ->
        if (!files.isNullOrEmpty()) scope.launch {
            try {
                for (f in files) added.add(AddedFile(seq++, f.toJpegUpload()))
                error = null
            } catch (e: Exception) {
                error = S.COMMON_SOMETHING_WRONG
            }
        }
    }
    val pdfPicker = rememberFilePickerLauncher(type = FileKitType.File(listOf("pdf")), mode = FileKitMode.Multiple()) { files ->
        if (!files.isNullOrEmpty()) scope.launch {
            try {
                for (f in files) {
                    val bytes = f.readBytes()
                    if (bytes.size > MAX_PDF_BYTES) { error = S.ERRORS_FILE_TOO_LARGE; continue }
                    added.add(AddedFile(seq++, UploadFile(f.name, "application/pdf", bytes)))
                }
            } catch (e: Exception) {
                error = S.COMMON_SOMETHING_WRONG
            }
        }
    }

    fun save() {
        submitted = true
        if (title.isBlank()) { error = S.ERRORS_VALIDATION; return }
        if (existing.size + added.size == 0) { error = S.ERRORS_FILES_REQUIRED; return }
        error = null
        val input = TicketInput(
            kind, title.trim(), description.trim(), locationText.trim(), locationUrl.trim(),
            removeFileIds = if (ticket != null) removed.toList() else null
        )
        pending = true
        scope.launch {
            try {
                val files = added.map { it.file }
                if (ticket != null) container.plans.updateTicket(planId, ticket.id, input, files)
                else container.plans.createTicket(planId, input, files)
                onSaved(S.TICKETS_SAVED)
                onDismiss()
            } catch (e: Exception) {
                error = apiErrorKey(e)
                pending = false
            }
        }
    }

    FormSheet(
        title = t(if (ticket != null) S.TICKETS_EDIT_TICKET else S.TICKETS_NEW_TICKET),
        onDismiss = { if (!pending) onDismiss() },
        footer = { FullPillButton(t(S.TICKETS_SAVE_TICKET), ::save, Modifier.testTag("ticket_save"), loading = pending) }
    ) {
        error?.let { ErrorText(t(it)) }
        Labeled(t(S.TICKETS_KIND)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (k in TICKET_KINDS) ChoicePill(t(k.label), kind == k.value, { kind = k.value }, icon = k.icon)
            }
        }
        RencanaTextField(
            title, { title = it.take(120) }, Modifier.testTag("ticket_title"),
            label = t(S.TICKETS_TITLE), placeholder = t(S.TICKETS_TITLE_PLACEHOLDER),
            isError = submitted && title.isBlank(), capitalization = KeyboardCapitalization.Sentences
        )
        RencanaTextField(
            description, { description = it.take(2000) }, Modifier.testTag("ticket_description"),
            label = t(S.TICKETS_DESCRIPTION), singleLine = false, minLines = 2, capitalization = KeyboardCapitalization.Sentences
        )
        RencanaTextField(locationText, { locationText = it.take(200) }, Modifier.testTag("ticket_location"), label = t(S.TICKETS_LOCATION))
        RencanaTextField(
            locationUrl, { locationUrl = it }, Modifier.testTag("ticket_location_url"),
            label = t(S.TICKETS_LOCATION_LINK), placeholder = "https://", keyboardType = KeyboardType.Uri
        )
        Labeled(t(S.TICKETS_FILES)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (f in existing) {
                    Thumb(onRemove = { removed.add(f.id) }) {
                        if (f.contentType.startsWith("image/")) AuthImage(f.id, Modifier.size(74.dp), thumb = true)
                        else PdfTile(f.originalName, Modifier.size(74.dp))
                    }
                }
                for (f in added.toList()) {
                    Thumb(onRemove = { added.remove(f) }) {
                        if (f.isImage) AsyncImage(f.file.bytes, null, modifier = Modifier.size(74.dp), imageLoader = publicImageLoader(), contentScale = ContentScale.Crop)
                        else PdfTile(f.file.name, Modifier.size(74.dp))
                    }
                }
            }
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AddButton(t(S.TICKETS_ADD_PHOTO), Modifier.weight(1f).testTag("tickets_add_photo")) { photoPicker.launch() }
                AddButton(t(S.TICKETS_ADD_PDF), Modifier.weight(1f).testTag("tickets_add_pdf")) { pdfPicker.launch() }
            }
        }
    }
}

@Composable
private fun Thumb(onRemove: () -> Unit, content: @Composable () -> Unit) {
    Box(Modifier.size(74.dp).clip(RoundedCornerShape(14.dp))) {
        content()
        Box(
            Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(Color(0xCC1C1917), CircleShape).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) { Icon(Lucide.X, t(S.COMMON_REMOVE), tint = Color.White, modifier = Modifier.size(12.dp)) }
    }
}

@Composable
private fun AddButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color(0xFFF0FDFA),
        border = BorderStroke(1.5.dp, RColors.Primary)
    ) {
        Row(
            Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Lucide.ImagePlus, null, tint = RColors.Primary, modifier = Modifier.size(20.dp))
            Text(text, style = RencanaType.current.bodySemi.copy(fontSize = 14.sp, color = RColors.Primary))
        }
    }
}
