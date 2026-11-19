package com.zuramai.rencana.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zuramai.rencana.AppContainer
import com.zuramai.rencana.data.PlanListItem
import com.zuramai.rencana.data.SessionState
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.i18n.text
import com.zuramai.rencana.i18n.tPlural
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.LocalSnackbar
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.ConfirmDeleteDialog
import com.zuramai.rencana.ui.components.EmptyState
import com.zuramai.rencana.ui.components.ErrorText
import com.zuramai.rencana.ui.components.FullPillButton
import com.zuramai.rencana.ui.components.MascotPose
import com.zuramai.rencana.ui.components.PillButton
import com.zuramai.rencana.ui.components.RencanaTopBar
import com.zuramai.rencana.ui.components.apiErrorKey
import com.zuramai.rencana.ui.screens.travel.PlanCard
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class TravelListViewModel(val container: AppContainer) : ViewModel() {
    var plans by mutableStateOf<List<PlanListItem>>(emptyList())
    var loaded by mutableStateOf(false)
    var refreshing by mutableStateOf(false)
    var error by mutableStateOf<S?>(null)
    var toDelete by mutableStateOf<PlanListItem?>(null)
    var deleting by mutableStateOf(false)

    fun refresh() {
        if (refreshing) return
        viewModelScope.launch {
            refreshing = true
            try {
                plans = container.plans.list()
                error = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = apiErrorKey(e)
            }
            loaded = true
            refreshing = false
        }
    }

    /** Deletes [toDelete]; [onResult] gets null on success or the error key. */
    fun confirmDelete(onResult: (S?) -> Unit) {
        val plan = toDelete ?: return
        if (deleting) return
        viewModelScope.launch {
            deleting = true
            try {
                container.plans.delete(plan.id)
                toDelete = null
                onResult(null)
                plans = container.plans.list()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(apiErrorKey(e))
            }
            deleting = false
        }
    }
}

/** Travel list (design 03/04). Cards tagged `plan_card`; create button `plan_create`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TravelListScreen(
    onOpenPlan: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val container = LocalAppContainer.current
    val vm = viewModel { TravelListViewModel(container) }
    val state by container.session.state.collectAsStateWithLifecycle()
    val me = (state as? SessionState.LoggedIn)?.me
    val snack = LocalSnackbar.current
    val lang = LocalLanguage.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }

    Column(Modifier.fillMaxSize()) {
        RencanaTopBar(me?.displayName, me?.avatarUrl, onOpenSettings)
        PullToRefreshBox(isRefreshing = vm.refreshing && vm.loaded, onRefresh = vm::refresh, modifier = Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.fillMaxSize()) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Text(t(S.TRAVEL_MY_TRIPS), style = RencanaType.current.display)
                            if (vm.plans.isNotEmpty()) {
                                Text(
                                    tPlural(S.TRAVEL_TRIP_COUNT_ZERO, S.TRAVEL_TRIP_COUNT_ONE, S.TRAVEL_TRIP_COUNT_OTHER, vm.plans.size),
                                    style = RencanaType.current.caption.copy(color = RColors.Muted)
                                )
                            }
                        }
                    }
                    if (vm.error != null && vm.plans.isEmpty()) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ErrorText(t(vm.error!!))
                                PillButton(t(S.COMMON_RETRY), vm::refresh)
                            }
                        }
                    } else if (vm.loaded && vm.plans.isEmpty()) {
                        item {
                            EmptyState(
                                MascotPose.Map, t(S.TRAVEL_NO_TRIPS), t(S.TRAVEL_NO_TRIPS_TEXT), Modifier.padding(top = 24.dp),
                                action = { PillButton(t(S.TRAVEL_CREATE_PLAN), onCreate, Modifier.testTag("plan_create_empty")) }
                            )
                        }
                    } else {
                        items(vm.plans, key = { it.id }) { plan ->
                            PlanCard(plan, { onOpenPlan(plan.id) }, { onEdit(plan.id) }, { vm.toDelete = plan })
                        }
                    }
                }
                if (vm.plans.isNotEmpty()) {
                    FullPillButton(
                        t(S.TRAVEL_CREATE_PLAN), onCreate,
                        Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 16.dp).testTag("plan_create")
                    )
                }
            }
        }
    }

    vm.toDelete?.let { plan ->
        ConfirmDeleteDialog(
            title = t(S.TRAVEL_DELETE_TITLE, "name" to plan.name),
            text = t(S.TRAVEL_DELETE_TEXT),
            onConfirm = {
                vm.confirmDelete { err ->
                    container.scope.launch { snack.showSnackbar((err ?: S.TRAVEL_TRIP_DELETED).text(lang)) }
                }
            },
            onDismiss = { vm.toDelete = null },
            loading = vm.deleting
        )
    }
}
