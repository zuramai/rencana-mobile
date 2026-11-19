package com.zuramai.rencana.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.content.TextContent
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * An API failure. [status] is the HTTP status, or 0 for transport failures. [code] is the backend's
 * `statusMessage` (e.g. `invalid_credentials`), or `network` / `http_<status>`.
 */
class ApiException(val status: Int, val code: String) : Exception("API $status $code") {
    /** True when the request never reached the server (offline, DNS, TLS, timeout). */
    val isNetwork: Boolean get() = status == 0
}

/** Result of [ApiClient.download]: body plus the server-suggested file name. */
class Download(val bytes: ByteArray, val contentType: String?, val fileName: String?)

/**
 * The only HTTP client for the Rencana backend. Owns one Ktor client with JSON, timeouts, bearer auth with
 * automatic refresh, and error mapping.
 *
 * SECURITY: the bearer token is attached only to requests whose host matches [baseUrl]. Always call with paths
 * relative to the base (`"api/plans"`), which [send] resolves against [baseUrl]; never pass external absolute URLs (avatars use a separate public client).
 *
 * @param baseUrl backend base URL without trailing slash.
 * @param tokenStore tokens bound to [baseUrl].
 * @param onSessionLost invoked when refreshing fails (the user must log in again); tokens are already cleared.
 * @param language supplies the current UI language for `Accept-Language`.
 * @param engine optional engine override (tests inject Ktor's `MockEngine`).
 * @param debug install request logging (URL and status only).
 */
class ApiClient(
    val baseUrl: String,
    private val tokenStore: TokenStore,
    private val onSessionLost: () -> Unit,
    private val language: () -> String = { "en" },
    engine: HttpClientEngine? = null,
    private val debug: Boolean = false
) {
    private val baseHost = Url(baseUrl).host
    private val baseProtocol = Url(baseUrl).protocol

    private fun configure(config: HttpClientConfig<*>) = with(config) {
        expectSuccess = false
        install(ContentNegotiation) { json(ApiJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
        }
        defaultRequest {
            header(HttpHeaders.AcceptLanguage, language())
        }
        if (debug) {
            install(Logging) {
                logger = Logger.SIMPLE
                level = LogLevel.INFO
            }
        }
        install(Auth) {
            bearer {
                loadTokens { tokenStore.load()?.let { BearerTokens(it.accessToken, it.refreshToken) } }
                refreshTokens {
                    val refresh = oldTokens?.refreshToken
                    if (refresh == null) {
                        tokenStore.clear()
                        onSessionLost()
                        return@refreshTokens null
                    }
                    val response = client.post("$baseUrl/api/mobile/auth/refresh") {
                        markAsRefreshTokenRequest()
                        contentType(ContentType.Application.Json)
                        setBody(TextContent(ApiJson.encodeToString(RefreshInput.serializer(), RefreshInput(refresh)), ContentType.Application.Json))
                    }
                    if (!response.status.isSuccess()) {
                        tokenStore.clear()
                        onSessionLost()
                        return@refreshTokens null
                    }
                    val pair = ApiJson.decodeFromString(RefreshResponse.serializer(), response.bodyAsText()).tokens
                    tokenStore.save(pair)
                    BearerTokens(pair.accessToken, pair.refreshToken)
                }
                sendWithoutRequest { request -> request.url.host == baseHost && request.url.protocol == baseProtocol }
            }
        }
    }

    /** The underlying client; Coil's authenticated loader shares it. Relative-resolved or same-host URLs only. */
    val http: HttpClient = if (engine != null) HttpClient(engine) { configure(this) } else HttpClient { configure(this) }

    /** Drops the cached bearer tokens so the next request reloads them from the [TokenStore]. Call after login/logout. */
    fun resetAuthCache() {
        http.authProvider<BearerAuthProvider>()?.clearToken()
    }

    /** Releases the client. */
    fun close() = http.close()

    /**
     * Sends a request and returns the successful response. Non-2xx responses throw [ApiException] built from
     * the `{statusCode, statusMessage}` body; transport failures throw `ApiException(0, "network")`.
     */
    suspend fun send(method: HttpMethod, path: String, block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}): HttpResponse {
        val response = try {
            http.request("$baseUrl/${path.trimStart('/')}") {
                this.method = method
                block()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "network")
        }
        if (!response.status.isSuccess()) throw response.toApiException()
        return response
    }

    private suspend fun HttpResponse.toApiException(): ApiException {
        val body = runCatching { ApiJson.decodeFromString(ErrorBody.serializer(), bodyAsText()) }.getOrNull()
        return ApiException(status.value, body?.statusMessage ?: "http_${status.value}")
    }

    /** JSON request with an optional [body] encoded by [bodySerializer]; decodes the response with [responseSerializer]. */
    suspend fun <B, R> json(
        method: HttpMethod,
        path: String,
        responseSerializer: KSerializer<R>,
        body: B? = null,
        bodySerializer: KSerializer<B>? = null
    ): R {
        val response = send(method, path) {
            if (body != null && bodySerializer != null) {
                setBody(TextContent(ApiJson.encodeToString(bodySerializer, body), ContentType.Application.Json))
            }
        }
        return decode(response, responseSerializer)
    }

    private suspend fun <R> decode(response: HttpResponse, serializer: KSerializer<R>): R = try {
        ApiJson.decodeFromString(serializer, response.bodyAsText())
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        throw ApiException(response.status.value, "invalid_response")
    }

    /** `GET` with a decoded JSON response. */
    suspend inline fun <reified R> get(path: String): R = json<Unit, R>(HttpMethod.Get, path, serializer<R>())

    /** JSON `[method]` request with a JSON [body]. */
    suspend inline fun <reified B, reified R> call(method: HttpMethod, path: String, body: B): R =
        json(method, path, serializer<R>(), body, serializer<B>())

    /** Body-less `[method]` request (e.g. `DELETE`). */
    suspend inline fun <reified R> call(method: HttpMethod, path: String): R = json<Unit, R>(method, path, serializer<R>())

    /**
     * Multipart request: field `data` holds [dataJson]; each of [files] is a part named [field] (`image` or `files`).
     * Use [HttpMethod.Patch] for updates.
     */
    suspend fun <R> multipart(
        method: HttpMethod,
        path: String,
        dataJson: String,
        field: String,
        files: List<UploadFile>,
        responseSerializer: KSerializer<R>
    ): R {
        val parts = formData {
            append("data", dataJson)
            for (f in files) {
                append(field, f.bytes, Headers.build {
                    append(HttpHeaders.ContentType, f.contentType)
                    append(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.File.withParameter(ContentDisposition.Parameters.Name, field)
                            .withParameter(ContentDisposition.Parameters.FileName, f.name).toString()
                    )
                })
            }
        }
        val response = send(method, path) { setBody(MultiPartFormDataContent(parts)) }
        return decode(response, responseSerializer)
    }

    /** Downloads a binary response (exports, files). */
    suspend fun download(path: String): Download {
        val response = send(HttpMethod.Get, path)
        val bytes = try {
            response.bodyAsBytes()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            throw ApiException(0, "network")
        }
        val disposition = response.headers[HttpHeaders.ContentDisposition]
        return Download(bytes, response.headers[HttpHeaders.ContentType], disposition?.let(::filenameFrom))
    }

    private fun filenameFrom(disposition: String): String? {
        Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(disposition)?.let {
            return percentDecode(it.groupValues[1])
        }
        return Regex("filename=\"([^\"]*)\"", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.get(1)
    }

    private fun percentDecode(s: String): String {
        val out = ArrayList<Byte>(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val hex = if (c == '%' && i + 2 < s.length + 0) s.substring(i + 1, i + 3).toIntOrNull(16) else null
            if (hex != null) { out.add(hex.toByte()); i += 3 } else { out.addAll(c.toString().encodeToByteArray().toList()); i++ }
        }
        return out.toByteArray().decodeToString()
    }
}
