package com.zuramai.rencana.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Shared JSON configuration. `explicitNulls` stays true (the backend schemas expect present `null` keys for
 * nullable fields) and `encodeDefaults` stays false so optional input fields with a `null`/default value are
 * omitted (zod `.optional()` rejects explicit `null`).
 */
val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = true
}

@Serializable
data class SessionUser(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    /** `en` or `id` */
    val locale: String,
    val defaultCurrency: String
)

@Serializable
data class MeDto(
    val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val locale: String,
    val defaultCurrency: String,
    val username: String?,
    val email: String?
)

@Serializable
data class TokenPair(
    val accessToken: String,
    /** ISO-8601 instant */
    val accessTokenExpiresAt: String,
    val refreshToken: String,
    val refreshTokenExpiresAt: String
)

@Serializable
data class AuthResponse(val user: SessionUser, val tokens: TokenPair)

@Serializable
data class RefreshResponse(val tokens: TokenPair)

@Serializable
data class PlanListItem(
    val id: String,
    val name: String,
    /** `emoji` or `image` */
    val coverType: String,
    /** Iconify name such as `fluent-emoji-flat:airplane` */
    val coverEmoji: String?,
    val coverFileId: String?,
    /** `#RRGGBB` */
    val coverColor: String,
    val startYear: Int?,
    val startMonth: Int?,
    val endYear: Int?,
    val endMonth: Int?
)

@Serializable
data class PlanDto(
    val id: String,
    val name: String,
    val coverType: String,
    val coverEmoji: String?,
    val coverFileId: String?,
    val coverColor: String,
    val startYear: Int?,
    val startMonth: Int?,
    val endYear: Int?,
    val endMonth: Int?,
    val budgetCurrency: String,
    /** Decimal string such as `"25000000.00"` */
    val budgetAmount: String?,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ItemDto(
    val id: String,
    /** One of the `ITEM_TYPES` wire values */
    val type: String,
    val description: String,
    val allDay: Boolean,
    /** `YYYY-MM-DDTHH:mm` destination wall clock */
    val startsAt: String,
    val endsAt: String?,
    val currency: String?,
    val price: String?,
    val note: String?,
    val link: String?,
    val imageFileId: String?
)

@Serializable
data class CostDto(
    val id: String,
    val name: String,
    /** `one_time` or `daily` */
    val frequency: String,
    val currency: String,
    val amount: String,
    val days: Int?
)

@Serializable
data class TicketFileDto(val id: String, val contentType: String, val originalName: String)

@Serializable
data class TicketDto(
    val id: String,
    /** One of the `TICKET_KINDS` wire values */
    val kind: String,
    val title: String,
    val description: String?,
    val locationText: String?,
    val locationUrl: String?,
    val files: List<TicketFileDto>
)

@Serializable
data class FxRateDto(
    val currency: String,
    val rate: Double,
    /** `auto` or `manual` */
    val source: String,
    val fetchedAt: String
)

@Serializable
data class BudgetCostLine(
    val id: String,
    val days: Int?,
    val total: Double,
    val currency: String,
    val converted: Double?
)

@Serializable
data class SummaryDto(
    val currency: String,
    val tripDays: Int,
    val itineraryTotal: Double,
    val byType: Map<String, Double>,
    val costs: List<BudgetCostLine>,
    val otherCostsTotal: Double,
    val grandTotal: Double,
    val budgetAmount: Double?,
    val remaining: Double?,
    val missingRates: List<String>,
    val rates: List<FxRateDto>
)

/** `{id}` response of plan/cost creation and updates. */
@Serializable
data class IdResponse(val id: String)

/** `{ok:true}` response of deletes and logout. */
@Serializable
data class OkResponse(val ok: Boolean)

@Serializable
data class HealthResponse(val ok: Boolean = false, val service: String = "")

// ---- Inputs ----

/** Plan create/update body (multipart field `data`). `removeCoverImage` only on update. */
@Serializable
data class PlanInput(
    val name: String,
    /** `emoji` or `image` */
    val coverType: String,
    val coverEmoji: String? = null,
    val coverColor: String,
    val removeCoverImage: Boolean? = null,
    val startYear: Int?,
    val startMonth: Int?,
    val endYear: Int?,
    val endMonth: Int?,
    val budgetCurrency: String,
    /** Decimal string, e.g. from `normalizeAmount` */
    val budgetAmount: String?
)

/** Itinerary item body (multipart field `data`). `removeImage` only on update. */
@Serializable
data class ItemInput(
    val type: String,
    val description: String,
    val allDay: Boolean,
    val startsAt: String,
    val endsAt: String?,
    val currency: String?,
    val price: String?,
    val note: String? = null,
    val link: String? = null,
    val removeImage: Boolean? = null
)

@Serializable
data class CostInput(
    val name: String,
    /** `one_time` or `daily` */
    val frequency: String,
    val currency: String,
    val amount: String,
    val days: Int?
)

/** Ticket body (multipart field `data`). `removeFileIds` only on update. */
@Serializable
data class TicketInput(
    val kind: String,
    val title: String,
    val description: String? = null,
    val locationText: String? = null,
    val locationUrl: String? = null,
    val removeFileIds: List<String>? = null
)

/** `rate` null resets the manual override back to automatic. */
@Serializable
data class RateInput(val rate: Double?)

@Serializable
data class MeInput(
    val displayName: String? = null,
    val locale: String? = null,
    val defaultCurrency: String? = null
)

@Serializable
data class LoginInput(val username: String, val password: String)

@Serializable
data class RegisterInput(
    val username: String,
    val displayName: String,
    val password: String,
    val timezone: String? = null,
    val locale: String
)

@Serializable
data class GoogleExchange(val code: String, val codeVerifier: String)

@Serializable
data class RefreshInput(val refreshToken: String)

/** Error wire format: `{ statusCode, statusMessage: "<code>", data? }`. */
@Serializable
internal data class ErrorBody(val statusCode: Int? = null, val statusMessage: String? = null)

/** One file to upload in a multipart request. */
class UploadFile(val name: String, val contentType: String, val bytes: ByteArray)
