package com.zuramai.rencana.data

import io.ktor.http.HttpMethod

/** Supplies the current [ApiClient] (it is replaced when the user changes server). */
typealias ApiProvider = () -> ApiClient

private fun <T> enc(serializer: kotlinx.serialization.KSerializer<T>, value: T) = ApiJson.encodeToString(serializer, value)

/** Auth and profile endpoints (plan section 1.8). Token persistence is done by `SessionManager`, not here. */
class AuthRepository(private val api: ApiProvider) {
    /** `POST api/mobile/auth/login` */
    suspend fun login(input: LoginInput): AuthResponse = api().call(HttpMethod.Post, "api/mobile/auth/login", input)

    /** `POST api/mobile/auth/register` */
    suspend fun register(input: RegisterInput): AuthResponse = api().call(HttpMethod.Post, "api/mobile/auth/register", input)

    /** `POST api/mobile/auth/google` (PKCE code exchange). */
    suspend fun googleExchange(input: GoogleExchange): AuthResponse = api().call(HttpMethod.Post, "api/mobile/auth/google", input)

    /** `POST api/mobile/auth/refresh`. Normally unnecessary: `ApiClient` refreshes transparently. */
    suspend fun refresh(input: RefreshInput): RefreshResponse = api().call(HttpMethod.Post, "api/mobile/auth/refresh", input)

    /** `POST api/mobile/auth/logout` (Bearer). */
    suspend fun logout(): OkResponse = api().call(HttpMethod.Post, "api/mobile/auth/logout")

    /** `GET api/me` */
    suspend fun me(): MeDto = api().get("api/me")

    /** `PATCH api/me`; returns the updated [SessionUser]. */
    suspend fun updateMe(input: MeInput): SessionUser = api().call(HttpMethod.Patch, "api/me", input)
}

/** Export kinds for [FilesRepository.exportPath]. */
enum class ExportKind(val wire: String) {
    ItineraryPdf("itinerary-pdf"),
    ItineraryXlsx("itinerary-xlsx"),
    TicketsPdf("tickets-pdf")
}

/** Plans, items, costs, rates and tickets (plan section 1.8). All ids are UUID strings. */
class PlansRepository(private val api: ApiProvider) {
    // ---- plans ----
    /** `GET api/plans` */
    suspend fun list(): List<PlanListItem> = api().get("api/plans")

    /** `GET api/plans/:id` */
    suspend fun get(id: String): PlanDto = api().get("api/plans/$id")

    /** `POST api/plans` multipart; [cover] is sent as part `image` when non-null. */
    suspend fun create(input: PlanInput, cover: UploadFile?): IdResponse =
        api().multipart(HttpMethod.Post, "api/plans", enc(PlanInput.serializer(), input), "image", listOfNotNull(cover), IdResponse.serializer())

    /** `PATCH api/plans/:id` multipart; set `input.removeCoverImage` to drop the cover. */
    suspend fun update(id: String, input: PlanInput, cover: UploadFile?): IdResponse =
        api().multipart(HttpMethod.Patch, "api/plans/$id", enc(PlanInput.serializer(), input), "image", listOfNotNull(cover), IdResponse.serializer())

    /** `DELETE api/plans/:id` */
    suspend fun delete(id: String): OkResponse = api().call(HttpMethod.Delete, "api/plans/$id")

    /** `GET api/plans/:id/summary` */
    suspend fun summary(id: String): SummaryDto = api().get("api/plans/$id/summary")

    /** `PUT api/plans/:id/rates/:currency`; a null [rate] resets to the automatic rate. */
    suspend fun setRate(id: String, currency: String, rate: Double?): OkResponse =
        api().call(HttpMethod.Put, "api/plans/$id/rates/$currency", RateInput(rate))

    // ---- items ----
    /** `GET api/plans/:id/items` (sorted by start time). */
    suspend fun items(id: String): List<ItemDto> = api().get("api/plans/$id/items")

    /** `POST api/plans/:id/items` multipart; [image] sent as part `image`. */
    suspend fun createItem(id: String, input: ItemInput, image: UploadFile?): ItemDto =
        api().multipart(HttpMethod.Post, "api/plans/$id/items", enc(ItemInput.serializer(), input), "image", listOfNotNull(image), ItemDto.serializer())

    /** `PATCH api/plans/:id/items/:itemId` multipart; set `input.removeImage` to drop the image. */
    suspend fun updateItem(id: String, itemId: String, input: ItemInput, image: UploadFile?): ItemDto =
        api().multipart(HttpMethod.Patch, "api/plans/$id/items/$itemId", enc(ItemInput.serializer(), input), "image", listOfNotNull(image), ItemDto.serializer())

    /** `DELETE api/plans/:id/items/:itemId` */
    suspend fun deleteItem(id: String, itemId: String): OkResponse = api().call(HttpMethod.Delete, "api/plans/$id/items/$itemId")

    // ---- costs ----
    /** `GET api/plans/:id/costs` */
    suspend fun costs(id: String): List<CostDto> = api().get("api/plans/$id/costs")

    /** `POST api/plans/:id/costs` */
    suspend fun createCost(id: String, input: CostInput): IdResponse = api().call(HttpMethod.Post, "api/plans/$id/costs", input)

    /** `PATCH api/plans/:id/costs/:costId` */
    suspend fun updateCost(id: String, costId: String, input: CostInput): IdResponse =
        api().call(HttpMethod.Patch, "api/plans/$id/costs/$costId", input)

    /** `DELETE api/plans/:id/costs/:costId` */
    suspend fun deleteCost(id: String, costId: String): OkResponse = api().call(HttpMethod.Delete, "api/plans/$id/costs/$costId")

    // ---- tickets ----
    /** `GET api/plans/:id/tickets` */
    suspend fun tickets(id: String): List<TicketDto> = api().get("api/plans/$id/tickets")

    /** `POST api/plans/:id/tickets` multipart; every file is part `files`. */
    suspend fun createTicket(id: String, input: TicketInput, files: List<UploadFile>): TicketDto =
        api().multipart(HttpMethod.Post, "api/plans/$id/tickets", enc(TicketInput.serializer(), input), "files", files, TicketDto.serializer())

    /** `PATCH api/plans/:id/tickets/:ticketId` multipart; `input.removeFileIds` drops existing files. */
    suspend fun updateTicket(id: String, ticketId: String, input: TicketInput, files: List<UploadFile>): TicketDto =
        api().multipart(HttpMethod.Patch, "api/plans/$id/tickets/$ticketId", enc(TicketInput.serializer(), input), "files", files, TicketDto.serializer())

    /** `DELETE api/plans/:id/tickets/:ticketId` */
    suspend fun deleteTicket(id: String, ticketId: String): OkResponse = api().call(HttpMethod.Delete, "api/plans/$id/tickets/$ticketId")
}

/** Binary endpoints: file bytes and exports. Use [FileOpener] to save and open them. */
class FilesRepository(private val api: ApiProvider) {
    /** `GET api/files/:id[?size=thumb]` as bytes. */
    suspend fun file(id: String, thumb: Boolean = false): Download =
        api().download("api/files/$id" + if (thumb) "?size=thumb" else "")

    /** `GET api/plans/:id/export/:kind` as bytes (attachment). */
    suspend fun export(planId: String, kind: ExportKind): Download = api().download("api/plans/$planId/export/${kind.wire}")

    /** Absolute image URL of a file, for Coil's authenticated loader (`AuthImage`). */
    fun fileUrl(id: String, thumb: Boolean = true): String =
        "${api().baseUrl}/api/files/$id" + if (thumb) "?size=thumb" else ""
}

