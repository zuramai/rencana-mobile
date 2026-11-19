package com.zuramai.rencana.ui

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.SnackbarHostState
import com.zuramai.rencana.AppContainer
import kotlinx.serialization.Serializable

/** Startup placeholder (Loading/Error). Never navigate here manually. */
@Serializable data object Splash

/** Backend address screen. Root while `NeedsServer`; also pushed from Login and Settings. */
@Serializable data object Server

@Serializable data object Login

@Serializable data object Register

@Serializable data object TravelList

/** Create a plan. */
@Serializable data object PlanNew

/** Edit a plan. [fromList] is true when opened from the list (go back to the list after saving/deleting). */
@Serializable data class PlanEdit(val id: String, val fromList: Boolean = true)

/** Plan detail. [tab] is `itinerary`, `costs` or `tickets`. */
@Serializable data class PlanDetail(val id: String, val tab: String = "itinerary")

@Serializable data object Budget

@Serializable data object Settings

/** The app container; provided by `App()`. */
val LocalAppContainer = compositionLocalOf<AppContainer> { error("LocalAppContainer not provided") }

/**
 * Snackbar host shown by the root scaffold. Use:
 * `val snack = LocalSnackbar.current; scope.launch { snack.showSnackbar(message) }`.
 */
val LocalSnackbar = compositionLocalOf<SnackbarHostState> { error("LocalSnackbar not provided") }
