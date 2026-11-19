package com.zuramai.rencana.ui.screens.planform

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.zuramai.rencana.domain.COVER_COLORS
import com.zuramai.rencana.domain.COVER_EMOJIS
import com.zuramai.rencana.domain.parseHexColor
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.components.PillButton
import com.zuramai.rencana.ui.components.PillStyle
import com.zuramai.rencana.ui.components.PlanCover
import com.zuramai.rencana.ui.components.RCard

/** Cover editor state (web `CoverValue`); [previewBytes] is a newly picked, not yet uploaded photo. */
data class CoverValue(
    val coverType: String = "emoji",
    val coverEmoji: String,
    val coverColor: String = "#D6ECFF",
    val coverFileId: String? = null,
    val previewBytes: ByteArray? = null
)

/** Emoji / photo tabs, live preview, emoji grid or photo button, and colour swatches. */
@Composable
fun CoverPicker(value: CoverValue, onChange: (CoverValue) -> Unit, onChoosePhoto: () -> Unit, modifier: Modifier = Modifier) {
    RCard(modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegmentedControl(
                listOf("emoji" to t(S.PLAN_EMOJI), "image" to t(S.PLAN_PHOTO)),
                value.coverType, { onChange(value.copy(coverType = it)) }, tag = "cover"
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanCover(value.coverType, value.coverEmoji, value.coverFileId, value.coverColor, size = 104.dp, radius = 24.dp, previewBytes = value.previewBytes)
                if (value.coverType == "emoji") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in COVER_EMOJIS.entries.chunked(4)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for ((name, glyph) in row) {
                                    val sel = value.coverEmoji == name
                                    Box(
                                        Modifier.size(38.dp)
                                            .background(if (sel) Color(0xFFE0F2F1) else Color(0xFFFAF6F0), RoundedCornerShape(12.dp))
                                            .let { if (sel) it.border(2.dp, RColors.Primary, RoundedCornerShape(12.dp)) else it }
                                            .clickable { onChange(value.copy(coverEmoji = name)) }
                                            .testTag("cover_emoji_${name.substringAfter(':')}"),
                                        contentAlignment = Alignment.Center
                                    ) { Text(glyph, fontSize = 22.sp) }
                                }
                            }
                        }
                    }
                } else {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PillButton(
                            t(S.PLAN_CHOOSE_PHOTO), onChoosePhoto, Modifier.testTag("cover_choose_photo"),
                            style = PillStyle.Secondary,
                            leading = { Icon(Lucide.ImagePlus, contentDescription = null, tint = RColors.Primary, modifier = Modifier.size(20.dp)) }
                        )
                    }
                }
            }
            if (value.coverType == "emoji") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (hex in COVER_COLORS) {
                        Swatch(parseHexColor(hex), value.coverColor.equals(hex, ignoreCase = true), { onChange(value.copy(coverColor = hex)) }, Modifier.testTag("cover_color_$hex"))
                    }
                }
            }
        }
    }
}
