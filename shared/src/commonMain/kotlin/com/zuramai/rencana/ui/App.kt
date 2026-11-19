package com.zuramai.rencana.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.compose.LocalPlatformContext
import com.zuramai.rencana.AppContainer
import com.zuramai.rencana.AppGraph
import com.zuramai.rencana.data.SessionState
import com.zuramai.rencana.data.createAuthImageLoader
import com.zuramai.rencana.data.createPublicImageLoader
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.text
import com.zuramai.rencana.platform.exposeTestTags
import com.zuramai.rencana.ui.components.BottomTab
import com.zuramai.rencana.ui.components.LocalAuthImageLoader
import com.zuramai.rencana.ui.components.LocalPublicImageLoader
import com.zuramai.rencana.ui.components.RencanaBottomNav
import com.zuramai.rencana.ui.screens.BudgetScreen
import com.zuramai.rencana.ui.screens.LoginScreen
import com.zuramai.rencana.ui.screens.PlanDetailScreen
import com.zuramai.rencana.ui.screens.PlanFormScreen
import com.zuramai.rencana.ui.screens.RegisterScreen
import com.zuramai.rencana.ui.screens.ServerScreen
import com.zuramai.rencana.ui.screens.SettingsScreen
import com.zuramai.rencana.ui.screens.StartupScreen
import com.zuramai.rencana.ui.screens.TravelListScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * App root. Wires the theme, language, image loaders, snackbar, navigation and the session-driven back-stack
 * reset (NeedsServer -> Server, LoggedOut -> Login, LoggedIn -> TravelList).
 */
@Composable
fun App(container: AppContainer = AppGraph.container) {
    val state by container.session.state.collectAsStateWithLifecycle()
    val pendingLanguage by container.session.pendingLanguage.collectAsStateWithLifecycle()
    val apiClient by container.session.apiClient.collectAsStateWithLifecycle()
    val language = (state as? SessionState.LoggedIn)?.me?.locale ?: pendingLanguage
    val context = LocalPlatformContext.current
    val authLoader = remember(apiClient) { apiClient?.let { createAuthImageLoader(context, it) } }
    val publicLoader = remember { createPublicImageLoader(context) }
    val snackbar = remember { SnackbarHostState() }
    val navController = rememberNavController()

    // Authenticated images must not outlive the session.
    LaunchedEffect(state is SessionState.LoggedIn, authLoader) {
        if (state !is SessionState.LoggedIn) authLoader?.memoryCache?.clear()
    }

    RencanaTheme {
        CompositionLocalProvider(
            LocalAppContainer provides container,
            LocalLanguage provides language,
            LocalAuthImageLoader provides authLoader,
            LocalPublicImageLoader provides publicLoader,
            LocalSnackbar provides snackbar
        ) {
            SessionNavigation(state, navController, snackbar, language)
            val entry by navController.currentBackStackEntryAsState()
            val destination = entry?.destination
            val tab = when {
                destination == null -> null
                destination.hasRoute<TravelList>() || destination.hasRoute<PlanDetail>() -> BottomTab.Travel
                destination.hasRoute<Budget>() -> BottomTab.Budget
                else -> null
            }
            Scaffold(
                modifier = Modifier.fillMaxSize().exposeTestTags(),
                containerColor = RColors.Background,
                snackbarHost = { SnackbarHost(snackbar) },
                bottomBar = {
                    if (tab != null) {
                        RencanaBottomNav(tab, { selected ->
                            val route: Any = if (selected == BottomTab.Travel) TravelList else Budget
                            if (selected != tab) navController.navigate(route) { popUpTo<TravelList>(); launchSingleTop = true }
                        }, Modifier.navigationBarsPadding())
                    }
                }
            ) { inner ->
                Box(Modifier.fillMaxSize().padding(inner).background(RColors.Background)) {
                    AppNavHost(navController, container)
                }
            }
        }
    }
}

/** Resets the back stack whenever the session kind changes. */
@Composable
private fun SessionNavigation(state: SessionState, nav: NavHostController, snackbar: SnackbarHostState, language: String) {
    val kind = when (state) {
        SessionState.Loading -> 0
        SessionState.Error -> 1
        SessionState.NeedsServer -> 2
        is SessionState.LoggedOut -> 3
        is SessionState.LoggedIn -> 4
    }
    LaunchedEffect(kind) {
        val target: Any = when (state) {
            SessionState.Loading, SessionState.Error -> Splash
            SessionState.NeedsServer -> Server
            is SessionState.LoggedOut -> Login
            is SessionState.LoggedIn -> TravelList
        }
        // The NavHost sets its graph during its own first composition, which can run after this
        // effect starts (it does on iOS): wait for the first back stack entry before navigating.
        nav.currentBackStackEntryFlow.first()
        nav.navigate(target) { popUpTo(nav.graph.id) { inclusive = true } }
    }
    LaunchedEffect(state) {
        if (state is SessionState.LoggedOut && state.expired) snackbar.showSnackbar(S.AUTH_SESSION_EXPIRED.text(language))
    }
}

@Composable
private fun AppNavHost(nav: NavHostController, container: AppContainer) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val state by container.session.state.collectAsStateWithLifecycle()
    NavHost(nav, startDestination = Splash) {
        composable<Splash> {
            StartupScreen(failed = state is SessionState.Error, onRetry = { scope.launch { container.session.start() } })
        }
        composable<Server> {
            val canGoBack = state !is SessionState.NeedsServer
            ServerScreen(
                onSaved = { if (state is SessionState.LoggedOut) nav.popBackStack(Login, inclusive = false) },
                onBack = if (canGoBack) ({ nav.popBackStack() }) else null
            )
        }
        composable<Login> {
            LoginScreen(onRegister = { nav.navigate(Register) }, onServer = { nav.navigate(Server) })
        }
        composable<Register> { RegisterScreen(onLogin = { nav.popBackStack() }) }
        composable<TravelList> {
            TravelListScreen(
                onOpenPlan = { nav.navigate(PlanDetail(it)) },
                onCreate = { nav.navigate(PlanNew) },
                onEdit = { nav.navigate(PlanEdit(it, fromList = true)) },
                onOpenSettings = { nav.navigate(Settings) }
            )
        }
        composable<PlanNew> {
            PlanFormScreen(
                planId = null, fromList = true,
                onDone = { id -> nav.popBackStack(); if (id != null) nav.navigate(PlanDetail(id)) },
                onBack = { nav.popBackStack() }
            )
        }
        composable<PlanEdit> { entry ->
            val route = entry.toRoute<PlanEdit>()
            PlanFormScreen(
                planId = route.id, fromList = route.fromList,
                onDone = { id ->
                    if (id == null) nav.popBackStack(TravelList, inclusive = false) else nav.popBackStack()
                },
                onBack = { nav.popBackStack() }
            )
        }
        composable<PlanDetail> { entry ->
            val route = entry.toRoute<PlanDetail>()
            PlanDetailScreen(
                planId = route.id, initialTab = route.tab,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(PlanEdit(it, fromList = false)) }
            )
        }
        composable<Budget> { BudgetScreen(onOpenSettings = { nav.navigate(Settings) }) }
        composable<Settings> {
            SettingsScreen(onBack = { nav.popBackStack() }, onOpenServer = { nav.navigate(Server) })
        }
    }
}
