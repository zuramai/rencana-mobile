package com.zuramai.rencana.data

import com.zuramai.rencana.platform.SecureStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.ByteArrayContent
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemoryStore : SecureStore {
    var value: String? = null
    override fun get() = value
    override fun put(value: String) { this.value = value }
    override fun clear() { value = null }
}

private const val BASE = "https://api.test"
private val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")

private fun pair(n: Int) = TokenPair("access$n", "2099-01-01T00:00:00Z", "refresh$n", "2099-02-01T00:00:00Z")

class ApiClientTest {
    private fun seeded(): Pair<MemoryStore, TokenStore> {
        val mem = MemoryStore()
        val store = TokenStore(mem, BASE)
        store.save(pair(1))
        return mem to store
    }

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK): HttpResponseData =
        respond(body, status, JSON_HEADERS)

    @Test
    fun refreshesOnceAndRetriesWithNewToken() = runTest {
        val (mem, store) = seeded()
        val seen = mutableListOf<String>()
        var refreshCalls = 0
        val engine = MockEngine { req: HttpRequestData ->
            seen += "${req.method.value} ${req.url.encodedPath} auth=${req.headers[HttpHeaders.Authorization]}"
            when {
                req.url.encodedPath.endsWith("api/mobile/auth/refresh") -> {
                    refreshCalls++
                    json("""{"tokens":{"accessToken":"access2","accessTokenExpiresAt":"2099-01-01T00:00:00Z","refreshToken":"refresh2","refreshTokenExpiresAt":"2099-02-01T00:00:00Z"}}""")
                }
                req.headers[HttpHeaders.Authorization] == "Bearer access2" -> json("""{"ok":true}""")
                else -> respond(
                    """{"statusCode":401,"statusMessage":"invalid_token"}""", HttpStatusCode.Unauthorized,
                    headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.WWWAuthenticate to listOf("Bearer error=\"invalid_token\""))
                )
            }
        }
        var lost = false
        val api = ApiClient(BASE, store, { lost = true }, engine = engine)
        val result = api.call<OkResponse>(HttpMethod.Get, "api/ping")
        assertTrue(result.ok)
        assertEquals(1, refreshCalls, seen.toString())
        assertEquals(3, seen.size, seen.toString())
        assertEquals("GET /api/ping auth=Bearer access1", seen[0])
        assertEquals("GET /api/ping auth=Bearer access2", seen[2])
        assertEquals("access2", store.load()?.accessToken)
        assertTrue(!lost)
        assertTrue(mem.value!!.contains("refresh2"))
    }

    @Test
    fun failedRefreshClearsStoreAndSignalsSessionLost() = runTest {
        val (mem, store) = seeded()
        val engine = MockEngine { req ->
            if (req.url.encodedPath.endsWith("api/mobile/auth/refresh")) {
                json("""{"statusCode":401,"statusMessage":"session_revoked"}""", HttpStatusCode.Unauthorized)
            } else {
                respond(
                    """{"statusCode":401,"statusMessage":"invalid_token"}""", HttpStatusCode.Unauthorized,
                    headersOf(HttpHeaders.ContentType to listOf("application/json"), HttpHeaders.WWWAuthenticate to listOf("Bearer error=\"invalid_token\""))
                )
            }
        }
        var lost = 0
        val api = ApiClient(BASE, store, { lost++ }, engine = engine)
        val e = assertFailsWith<ApiException> { api.call<OkResponse>(HttpMethod.Get, "api/ping") }
        assertEquals(401, e.status)
        assertEquals(1, lost)
        assertNull(mem.value)
    }

    @Test
    fun mapsErrorBodies() = runTest {
        val (_, store) = seeded()
        val engine = MockEngine { json("""{"statusCode":404,"statusMessage":"plan_not_found"}""", HttpStatusCode.NotFound) }
        val api = ApiClient(BASE, store, {}, engine = engine)
        val e = assertFailsWith<ApiException> { api.get<PlanDto>("api/plans/x") }
        assertEquals(404, e.status)
        assertEquals("plan_not_found", e.code)
    }

    @Test
    fun nonJsonErrorFallsBackToHttpCode() = runTest {
        val (_, store) = seeded()
        val engine = MockEngine { respond("boom", HttpStatusCode.BadGateway) }
        val e = assertFailsWith<ApiException> { ApiClient(BASE, store, {}, engine = engine).get<PlanDto>("api/plans/x") }
        assertEquals(502, e.status)
        assertEquals("http_502", e.code)
    }

    @Test
    fun tokenStoreIgnoresOtherBackend() {
        val mem = MemoryStore()
        TokenStore(mem, "https://a.test").save(pair(1))
        assertNull(TokenStore(mem, "https://b.test").load())
        assertNull(mem.value)
    }

    @Test
    fun tokenNotSentToOtherHost() = runTest {
        val (_, store) = seeded()
        var auth: String? = "unset"
        val engine = MockEngine { req -> auth = req.headers[HttpHeaders.Authorization]; json("""{"ok":true}""") }
        val api = ApiClient(BASE, store, {}, engine = engine)
        api.get<OkResponse>("api/ping")
        assertEquals("Bearer access1", auth)
        api.http.let { c ->
            c.get("https://evil.example/x")
        }
        assertNull(auth)
    }

    @Test
    fun sendsJsonBody() = runTest {
        val (_, store) = seeded()
        var body = ""
        var type: String? = null
        val engine = MockEngine { req ->
            body = (req.body as io.ktor.http.content.TextContent).text
            type = req.body.contentType?.toString()
            json("""{"ok":true}""")
        }
        val api = ApiClient(BASE, store, {}, engine = engine)
        api.call<LoginInput, OkResponse>(HttpMethod.Post, "api/x", LoginInput("a", "b"))
        assertEquals("""{"username":"a","password":"b"}""", body)
        assertTrue(type!!.startsWith("application/json"), type)
    }
}
