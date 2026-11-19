package com.zuramai.rencana.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.BedDouble
import com.composables.icons.lucide.Bus
import com.composables.icons.lucide.CircleDot
import com.composables.icons.lucide.Landmark
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PartyPopper
import com.composables.icons.lucide.Plane
import com.composables.icons.lucide.Ship
import com.composables.icons.lucide.ShoppingBag
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Ticket
import com.composables.icons.lucide.TrainFront
import com.composables.icons.lucide.Utensils
import com.zuramai.rencana.i18n.S

/** Itinerary item category. [value] is the wire value; [label] the localised name key. */
data class ItemTypeInfo(val value: String, val icon: ImageVector, val color: Color, val label: S)

/** Ticket category. [value] is the wire value; [label] the localised name key. */
data class TicketKindInfo(val value: String, val icon: ImageVector, val label: S)

/** Port of `ITEM_TYPES` (shared/utils/item-types.ts). */
val ITEM_TYPES: List<ItemTypeInfo> = listOf(
    ItemTypeInfo("attraction", Lucide.Landmark, Color(0xFF8B5CF6), S.ITEM_TYPES_ATTRACTION),
    ItemTypeInfo("transportation", Lucide.Plane, Color(0xFF3B82F6), S.ITEM_TYPES_TRANSPORTATION),
    ItemTypeInfo("food_beverage", Lucide.Utensils, Color(0xFFF97316), S.ITEM_TYPES_FOOD_BEVERAGE),
    ItemTypeInfo("accommodation", Lucide.BedDouble, Color(0xFF14B8A6), S.ITEM_TYPES_ACCOMMODATION),
    ItemTypeInfo("activity", Lucide.Sparkles, Color(0xFF22C55E), S.ITEM_TYPES_ACTIVITY),
    ItemTypeInfo("shopping", Lucide.ShoppingBag, Color(0xFFEC4899), S.ITEM_TYPES_SHOPPING),
    ItemTypeInfo("event", Lucide.PartyPopper, Color(0xFFEAB308), S.ITEM_TYPES_EVENT),
    ItemTypeInfo("other", Lucide.CircleDot, Color(0xFF78716C), S.ITEM_TYPES_OTHER)
)

/** Info for an item type wire value; falls back to `other`. */
fun itemType(value: String): ItemTypeInfo = ITEM_TYPES.firstOrNull { it.value == value } ?: ITEM_TYPES.last()

/** Port of `TICKET_KINDS`. */
val TICKET_KINDS: List<TicketKindInfo> = listOf(
    TicketKindInfo("flight", Lucide.Plane, S.TICKET_KINDS_FLIGHT),
    TicketKindInfo("train", Lucide.TrainFront, S.TICKET_KINDS_TRAIN),
    TicketKindInfo("bus", Lucide.Bus, S.TICKET_KINDS_BUS),
    TicketKindInfo("ferry", Lucide.Ship, S.TICKET_KINDS_FERRY),
    TicketKindInfo("attraction", Lucide.Landmark, S.TICKET_KINDS_ATTRACTION),
    TicketKindInfo("event", Lucide.PartyPopper, S.TICKET_KINDS_EVENT),
    TicketKindInfo("accommodation", Lucide.BedDouble, S.TICKET_KINDS_ACCOMMODATION),
    TicketKindInfo("other", Lucide.Ticket, S.TICKET_KINDS_OTHER)
)

/** Info for a ticket kind wire value; falls back to `other`. */
fun ticketKind(value: String): TicketKindInfo = TICKET_KINDS.firstOrNull { it.value == value } ?: TICKET_KINDS.last()

/** Plan cover background colours (hex strings, as the API stores them). */
val COVER_COLORS: List<String> =
    listOf("#FFE3D3", "#FFF1B8", "#D9F5E5", "#D6ECFF", "#E8DDFF", "#FFD9E8", "#E0F2F1", "#F1F1F1")

/** Parses `#RRGGBB` into a [Color]. */
fun parseHexColor(hex: String): Color {
    val v = hex.removePrefix("#").toLong(16)
    return Color(0xFF000000 or v)
}

/** Iconify name (as stored by the API) of the default cover emoji. */
const val DEFAULT_COVER_EMOJI = "fluent-emoji-flat:airplane"

/** Cover emoji choices in display order: iconify name (API value) to Unicode glyph. */
val COVER_EMOJIS: Map<String, String> = linkedMapOf(
    "fluent-emoji-flat:airplane" to "✈\uFE0F",
    "fluent-emoji-flat:luggage" to "\uD83E\uDDF3",
    "fluent-emoji-flat:world-map" to "\uD83D\uDDFA\uFE0F",
    "fluent-emoji-flat:compass" to "\uD83E\uDDED",
    "fluent-emoji-flat:beach-with-umbrella" to "\uD83C\uDFD6\uFE0F",
    "fluent-emoji-flat:desert-island" to "\uD83C\uDFDD\uFE0F",
    "fluent-emoji-flat:mount-fuji" to "\uD83D\uDDFB",
    "fluent-emoji-flat:snow-capped-mountain" to "\uD83C\uDFD4\uFE0F",
    "fluent-emoji-flat:sunrise-over-mountains" to "\uD83C\uDF04",
    "fluent-emoji-flat:national-park" to "\uD83C\uDFDE\uFE0F",
    "fluent-emoji-flat:camping" to "\uD83C\uDFD5\uFE0F",
    "fluent-emoji-flat:cityscape-at-dusk" to "\uD83C\uDF06",
    "fluent-emoji-flat:japanese-castle" to "\uD83C\uDFEF",
    "fluent-emoji-flat:ferris-wheel" to "\uD83C\uDFA1",
    "fluent-emoji-flat:sushi" to "\uD83C\uDF63",
    "fluent-emoji-flat:camera" to "\uD83D\uDCF7"
)

/** Glyph for a stored cover emoji name; falls back to the default. */
fun coverEmojiGlyph(name: String?): String = COVER_EMOJIS[name] ?: COVER_EMOJIS.getValue(DEFAULT_COVER_EMOJI)

/** Quick-add cost preset: [label] key and wire [frequency] (`daily` or `one_time`). */
data class CostPreset(val key: String, val frequency: String, val label: S)

/** Port of `COST_PRESETS`. */
val COST_PRESETS: List<CostPreset> = listOf(
    CostPreset("meals", "daily", S.COST_PRESETS_MEALS),
    CostPreset("localTransport", "daily", S.COST_PRESETS_LOCAL_TRANSPORT),
    CostPreset("tips", "daily", S.COST_PRESETS_TIPS),
    CostPreset("esim", "one_time", S.COST_PRESETS_ESIM),
    CostPreset("insurance", "one_time", S.COST_PRESETS_INSURANCE),
    CostPreset("emergencyFund", "one_time", S.COST_PRESETS_EMERGENCY_FUND),
    CostPreset("visa", "one_time", S.COST_PRESETS_VISA),
    CostPreset("souvenirs", "one_time", S.COST_PRESETS_SOUVENIRS)
)
