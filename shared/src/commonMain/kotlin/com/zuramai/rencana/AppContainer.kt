package com.zuramai.rencana

import com.zuramai.rencana.data.AuthRepository
import com.zuramai.rencana.data.FilesRepository
import com.zuramai.rencana.data.PlansRepository
import com.zuramai.rencana.data.SessionManager
import com.zuramai.rencana.platform.createPrefs
import com.zuramai.rencana.platform.createSecureStore
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual DI root: one [SessionManager] and the repositories that follow its current `ApiClient`.
 * Obtain it in composables with `LocalAppContainer.current`; create ViewModels with
 * `viewModel { MyViewModel(container) }`.
 *
 * @param engine optional Ktor engine override for tests.
 */
class AppContainer(engine: HttpClientEngine? = null) {
    /** Process-lifetime scope for session work (ViewModels use `viewModelScope` for their own). */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Backend URL, tokens and the session state machine. */
    val session = SessionManager(createPrefs(), createSecureStore(), scope, engine = engine)

    /** Login/profile endpoints. Prefer `session.login/...` for flows that change session state. */
    val auth = AuthRepository(session::requireApi)

    /** Plans, items, costs, rates and tickets. */
    val plans = PlansRepository(session::requireApi)

    /** File bytes, image URLs and exports. */
    val files = FilesRepository(session::requireApi)

    init {
        scope.launch { session.start() }
    }
}

/** The app-wide container (created on first use, i.e. when `App()` first composes). */
object AppGraph {
    val container: AppContainer by lazy { AppContainer() }
}
