package com.zuramai.rencana.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.URLProtocol
import io.ktor.http.isSuccess
import io.ktor.http.parseUrl
import io.ktor.serialization.kotlinx.json.json

/** Parsing and health-checking of the user-entered backend address. */
object BackendUrl {
    /**
     * Normalises user [input] to a base URL without trailing slash. Requires `https` (or `http` when
     * [allowHttp]), a non-empty host and no user info, query or fragment.
     */
    fun parse(input: String, allowHttp: Boolean): Result<String> {
        val trimmed = input.trim()
        fun fail(reason: String) = Result.failure<String>(IllegalArgumentException(reason))
        val explicitScheme = trimmed.startsWith("https://", ignoreCase = true) ||
            (allowHttp && trimmed.startsWith("http://", ignoreCase = true))
        if (!explicitScheme) return fail("scheme not allowed")
        val url = parseUrl(trimmed) ?: return fail("invalid url")
        if (url.protocol != URLProtocol.HTTPS && !(allowHttp && url.protocol == URLProtocol.HTTP)) return fail("scheme not allowed")
        if (url.host.isEmpty()) return fail("empty host")
        if (url.user != null || url.password != null) return fail("user info not allowed")
        if ('@' in trimmed.substringAfter("://").substringBefore('/')) return fail("user info not allowed")
        if ('?' in trimmed || '#' in trimmed) return fail("query/fragment not allowed")
        return Result.success(trimmed.trimEnd('/'))
    }

    /**
     * `GET {url}/healthz` with a plain (no auth) client and a 10 s timeout; true only for a 200 JSON body
     * whose `service` is `"rencana"`. Never throws. [engine] is injectable for tests.
     */
    suspend fun check(url: String, engine: HttpClientEngine? = null): Boolean {
        val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
            }
            install(ContentNegotiation) { json(ApiJson) }
        }
        val client = if (engine != null) HttpClient(engine, configure) else HttpClient(configure)
        return try {
            val response = client.get("$url/healthz")
            response.status.isSuccess() && runCatching {
                ApiJson.decodeFromString(HealthResponse.serializer(), response.bodyAsText())
            }.getOrNull()?.service == "rencana"
        } catch (_: Exception) {
            false
        } finally {
            client.close()
        }
    }
}
