package com.zuramai.rencana.data

import com.zuramai.rencana.platform.OAuthBrowser
import com.zuramai.rencana.platform.PlatformInfo
import com.zuramai.rencana.platform.Prefs
import com.zuramai.rencana.platform.SecureStore
import com.zuramai.rencana.platform.timeZoneId
import io.ktor.client.engine.HttpClientEngine
import io.ktor.http.Url
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the root navigation observes. */
sealed interface SessionState {
    /** Startup check in progress. */
    data object Loading : SessionState

    /** No backend URL stored and no debug default: show the Server screen. */
    data object NeedsServer : SessionState

    /** Show Login. [expired] is true when a previous session ended involuntarily (show a snackbar). */
    data class LoggedOut(val expired: Boolean = false) : SessionState

    /** Authenticated; [me] is the fresh profile. */
    data class LoggedIn(val me: MeDto) : SessionState

    /** Startup could not reach the server; offer a retry via [SessionManager.start]. */
    data object Error : SessionState
}

/** Result of [SessionManager.googleSignIn]. [Cancelled] must show nothing; [Failed] shows `S.ERRORS_GOOGLE`. */
enum class GoogleOutcome { Success, Cancelled, Failed }

/**
 * Owns the backend URL, the current [ApiClient], the token store and the [SessionState] machine.
 * Repositories obtain the client through [requireApi], so they transparently follow server changes.
 */
class SessionManager(
    private val prefs: Prefs,
    private val secureStore: SecureStore,
    private val scope: CoroutineScope,
    private val allowHttp: Boolean = PlatformInfo.isDebug,
    private val defaultBackendUrl: String? = PlatformInfo.defaultBackendUrl,
    private val deviceLanguage: String = PlatformInfo.deviceLanguage,
    private val engine: HttpClientEngine? = null,
    private val browser: suspend (String) -> String? = { OAuthBrowser.authenticate(it) },
    private val wipeExports: suspend () -> Unit = { FileOpener.wipe() }
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)

    /** Current session state. */
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val _backendUrl = MutableStateFlow(prefs.getString(KEY_URL))

    /** Stored backend URL (null until the user saved one). */
    val backendUrl: StateFlow<String?> = _backendUrl.asStateFlow()

    private val languagePref = MutableStateFlow(prefs.getString(KEY_LANGUAGE) ?: deviceLanguage)

    private val _apiClient = MutableStateFlow<ApiClient?>(null)

    /** Current client (null until a backend is configured). Observed to rebuild Coil's authenticated loader. */
    val apiClient: StateFlow<ApiClient?> = _apiClient.asStateFlow()

    private var api: ApiClient?
        get() = _apiClient.value
        set(value) { _apiClient.value = value }
    private var tokenStore: TokenStore? = null

    /** The URL to prefill on the Server screen: stored, else the debug default, else empty. */
    val suggestedUrl: String get() = _backendUrl.value ?: defaultBackendUrl.orEmpty()

    /**
     * Effective UI language: the logged-in user's locale, else the stored preference, else the device language.
     * Read from composition via `LocalLanguage`; it is also sent as `Accept-Language`.
     */
    fun currentLanguage(): String = (_state.value as? SessionState.LoggedIn)?.me?.locale ?: languagePref.value

    /** Language used on screens without a user (auth screens). */
    val pendingLanguage: StateFlow<String> = languagePref.asStateFlow()

    /** Stores the language preference (auth-screen switch; also called after a Settings change). */
    fun setLanguagePref(lang: String) {
        prefs.putString(KEY_LANGUAGE, lang)
        languagePref.value = lang
    }

    /** The current client; throws when no server is configured. Pass `session::requireApi` to repositories. */
    fun requireApi(): ApiClient = api ?: error("No backend configured")

    private fun buildClient(url: String) {
        api?.close()
        val store = TokenStore(secureStore, url)
        tokenStore = store
        api = ApiClient(
            baseUrl = url,
            tokenStore = store,
            onSessionLost = ::onSessionLost,
            language = ::currentLanguage,
            engine = engine,
            debug = PlatformInfo.isDebug
        )
    }

    private fun onSessionLost() {
        scope.launch { wipeExports() }
        if (_state.value is SessionState.LoggedIn || _state.value is SessionState.Loading) {
            _state.value = SessionState.LoggedOut(expired = true)
        }
    }

    /**
     * Startup (and retry): resolves the backend URL, validates the stored session with `GET api/me`, and moves to
     * [SessionState.NeedsServer], [SessionState.LoggedOut], [SessionState.LoggedIn] or [SessionState.Error].
     */
    suspend fun start() {
        wipeExports()
        val url = _backendUrl.value ?: defaultBackendUrl
        if (url == null) {
            _state.value = SessionState.NeedsServer
            return
        }
        _state.value = SessionState.Loading
        if (api == null || api?.baseUrl != url) buildClient(url)
        if (tokenStore?.load() == null) {
            _state.value = SessionState.LoggedOut(false)
            return
        }
        try {
            _state.value = SessionState.LoggedIn(AuthRepository(::requireApi).me())
        } catch (e: ApiException) {
            _state.value = when {
                e.status == 401 -> SessionState.LoggedOut(expired = true)
                else -> SessionState.Error
            }
        }
    }

    private suspend fun adopt(response: AuthResponse) {
        tokenStore!!.save(response.tokens)
        requireApi().resetAuthCache()
        _state.value = SessionState.LoggedIn(
            MeDto(
                response.user.id, response.user.displayName, response.user.avatarUrl,
                response.user.locale, response.user.defaultCurrency, null, null
            )
        )
        // Fill username/email; failure is harmless (the profile reloads on next start).
        runCatching { refreshMe() }
    }

    /** `POST login`; throws [ApiException] on failure, otherwise the state becomes [SessionState.LoggedIn]. */
    suspend fun login(username: String, password: String) {
        adopt(AuthRepository(::requireApi).login(LoginInput(username, password)))
    }

    /** `POST register` (sends the device time zone and current language); same contract as [login]. */
    suspend fun register(username: String, displayName: String, password: String) {
        adopt(
            AuthRepository(::requireApi).register(
                RegisterInput(username, displayName, password, timeZoneId(), languagePref.value)
            )
        )
    }

    /** Reloads `GET api/me` into [state] (after profile edits). Throws [ApiException]. */
    suspend fun refreshMe() {
        _state.value = SessionState.LoggedIn(AuthRepository(::requireApi).me())
    }

    /**
     * Runs the Google PKCE flow in the system browser. Success moves to [SessionState.LoggedIn];
     * [GoogleOutcome.Cancelled] means the user dismissed the browser.
     */
    suspend fun googleSignIn(): GoogleOutcome {
        val base = requireApi().baseUrl
        val state = Pkce.randomToken()
        val verifier = Pkce.randomToken()
        val challenge = Pkce.challenge(verifier)
        val url = "$base/auth/google?client=mobile&code_challenge=$challenge&state=$state" +
            "&locale=${languagePref.value}&tz=${timeZoneId().encodeURLParameter()}"
        val callback = browser(url) ?: return GoogleOutcome.Cancelled
        val params = runCatching { Url(callback).parameters }.getOrNull() ?: return GoogleOutcome.Failed
        if (params["state"] != state || params["error"] != null) return GoogleOutcome.Failed
        val code = params["code"] ?: return GoogleOutcome.Failed
        return try {
            adopt(AuthRepository(::requireApi).googleExchange(GoogleExchange(code, verifier)))
            GoogleOutcome.Success
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            GoogleOutcome.Failed
        }
    }

    /** Best-effort server logout, then wipes tokens and the export cache and moves to [SessionState.LoggedOut]. */
    suspend fun logout() {
        if (api != null && tokenStore?.load() != null) {
            try {
                AuthRepository(::requireApi).logout()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // best effort
            }
        }
        tokenStore?.clear()
        api?.resetAuthCache()
        wipeExports()
        _state.value = SessionState.LoggedOut(false)
    }

    /**
     * Saves [url] (already validated by `BackendUrl.parse` and `BackendUrl.check`), signs out of the old
     * server, rebuilds the client and moves to [SessionState.LoggedOut].
     */
    suspend fun changeServer(url: String) {
        if (url != api?.baseUrl) logout()
        prefs.putString(KEY_URL, url)
        _backendUrl.value = url
        buildClient(url)
        _state.value = SessionState.LoggedOut(false)
    }

    /** Parses [input] with this build's http policy. */
    fun parseUrl(input: String): Result<String> = BackendUrl.parse(input, allowHttp)

    /** Health-checks [url] (see [BackendUrl.check]). */
    suspend fun checkUrl(url: String): Boolean = BackendUrl.check(url, engine)

    private companion object {
        const val KEY_URL = "backend_url"
        const val KEY_LANGUAGE = "language"
    }
}
