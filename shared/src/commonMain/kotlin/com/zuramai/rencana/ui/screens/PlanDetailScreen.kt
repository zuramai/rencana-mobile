package com.zuramai.rencana.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.zuramai.rencana.AppContainer
import com.zuramai.rencana.data.ApiException
import com.zuramai.rencana.data.CostDto
import com.zuramai.rencana.data.ItemDto
import com.zuramai.rencana.data.PlanDto
import com.zuramai.rencana.data.SummaryDto
import com.zuramai.rencana.data.TicketDto
import com.zuramai.rencana.domain.CostPreset
import com.zuramai.rencana.domain.formatPeriod
import com.zuramai.rencana.i18n.LocalLanguage
import com.zuramai.rencana.i18n.S
import com.zuramai.rencana.i18n.t
import com.zuramai.rencana.platform.formatMoney
import com.zuramai.rencana.ui.LocalAppContainer
import com.zuramai.rencana.ui.LocalSnackbar
import com.zuramai.rencana.i18n.text
import androidx.compose.runtime.rememberCoroutineScope
import com.zuramai.rencana.ui.RColors
import com.zuramai.rencana.ui.RencanaType
import com.zuramai.rencana.ui.components.EmptyState
import com.zuramai.rencana.ui.components.MascotPose
import com.zuramai.rencana.ui.components.PageHeader
import com.zuramai.rencana.ui.components.PillButton
import com.zuramai.rencana.ui.components.PlanCover
import com.zuramai.rencana.ui.components.RCard
import com.zuramai.rencana.ui.components.apiErrorKey
import com.zuramai.rencana.ui.screens.detail.CostFormSheet
import com.zuramai.rencana.ui.screens.detail.CostsTab
import com.zuramai.rencana.ui.screens.detail.ExportSheet
import com.zuramai.rencana.ui.screens.detail.Holder
import com.zuramai.rencana.ui.screens.detail.ItemFormSheet
import com.zuramai.rencana.ui.screens.detail.ItineraryTab
import com.zuramai.rencana.ui.screens.detail.Segmented
import com.zuramai.rencana.ui.screens.detail.TicketFormSheet
import com.zuramai.rencana.ui.screens.detail.TicketViewerDialog
import com.zuramai.rencana.ui.screens.detail.TicketsTab
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class DetailLoad { Loading, Ready, NotFound, Error }

/** Holds the plan and its items, costs, tickets and budget summary; the tabs refresh pieces after mutations. */
class PlanDetailViewModel(val container: AppContainer, val planId: String) : ViewModel() {
    private val _load = MutableStateFlow(DetailLoad.Loading)
    val load: StateFlow<DetailLoad> = _load
    private val _plan = MutableStateFlow<PlanDto?>(null)
    val plan: StateFlow<PlanDto?> = _plan
    private val _items = MutableStateFlow<List<ItemDto>>(emptyList())
    val items: StateFlow<List<ItemDto>> = _items
    private val _costs = MutableStateFlow<List<CostDto>>(emptyList())
    val costs: StateFlow<List<CostDto>> = _costs
    private val _tickets = MutableStateFlow<List<TicketDto>>(emptyList())
    val tickets: StateFlow<List<TicketDto>> = _tickets
    private val _summary = MutableStateFlow<SummaryDto?>(null)
    val summary: StateFlow<SummaryDto?> = _summary
    private val _error = MutableStateFlow<S?>(null)
    val error: StateFlow<S?> = _error

    init { load() }

    fun load() {
        _load.value = DetailLoad.Loading
        viewModelScope.launch {
            try {
                val p = container.plans
                val plan = async { p.get(planId) }
                val items = async { p.items(planId) }
                val summary = async { p.summary(planId) }
                val costs = async { p.costs(planId) }
                val tickets = async { p.tickets(planId) }
                _plan.value = plan.await()
                _items.value = items.await()
                _summary.value = summary.await()
                _costs.value = costs.await()
                _tickets.value = tickets.await()
                _load.value = DetailLoad.Ready
            } catch (e: Exception) {
                _error.value = apiErrorKey(e)
                _load.value = if (e is ApiException && (e.status == 404)) DetailLoad.NotFound else DetailLoad.Error
            }
        }
    }

    private fun refresh(block: suspend () -> Unit) {
        viewModelScope.launch { try { block() } catch (_: Exception) { } }
    }

    fun refreshSummary() = refresh { _summary.value = container.plans.summary(planId) }

    fun itemsChanged() = refresh {
        _items.value = container.plans.items(planId)
        _summary.value = container.plans.summary(planId)
    }

    fun costsChanged() = refresh {
        _costs.value = container.plans.costs(planId)
        _summary.value = container.plans.summary(planId)
    }

    fun ticketsChanged() = refresh { _tickets.value = container.plans.tickets(planId) }
}

/** Plan detail (design 06/07 + tabs 09 to 14). [initialTab] is `itinerary`, `costs` or `tickets`. */
@OptIn(ExperimentalTime::class)
@Composable
fun PlanDetailScreen(planId: String, initialTab: String, onBack: () -> Unit, onEdit: (String) -> Unit) {
    val container = LocalAppContainer.current
    val vm = viewModel(key = planId) { PlanDetailViewModel(container, planId) }
    val lang = LocalLanguage.current
    val load by vm.load.collectAsStateWithLifecycle()
    val plan by vm.plan.collectAsStateWithLifecycle()
    val items by vm.items.collectAsStateWithLifecycle()
    val costs by vm.costs.collectAsStateWithLifecycle()
    val tickets by vm.tickets.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableStateOf(if (initialTab in listOf("itinerary", "costs", "tickets")) initialTab else "itinerary") }
    var itemSheet by remember { mutableStateOf<Holder<ItemDto>?>(null) }
    var costSheet by remember { mutableStateOf<Pair<Holder<CostDto>, CostPreset?>?>(null) }
    var viewing by remember { mutableStateOf<TicketDto?>(null) }
    var ticketSheet by remember { mutableStateOf<Holder<TicketDto>?>(null) }
    var exportOpen by remember { mutableStateOf(false) }
    val snack = LocalSnackbar.current
    val screenScope = rememberCoroutineScope()
    fun toast(key: S) { screenScope.launch { snack.showSnackbar(key.text(lang)) } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader(onBack, actions = {
                if (plan != null) {
                    HeaderAction(Lucide.Download, t(S.EXPORT_TITLE), "plan_export") { exportOpen = true }
                    HeaderAction(Lucide.Pencil, t(S.PLAN_EDIT_TRIP), "plan_edit") { onEdit(planId) }
                }
            })
            val p = plan
            when {
                load == DetailLoad.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = RColors.Primary) }
                p == null || load != DetailLoad.Ready -> EmptyState(
                    MascotPose.Construction, t(error ?: S.COMMON_SOMETHING_WRONG), "",
                    action = { PillButton(t(S.COMMON_RETRY), { vm.load() }) }
                )
                else -> Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HeaderCard(p, summary, lang)
                    Segmented(
                        listOf("itinerary" to t(S.PLAN_TABS_ITINERARY), "costs" to t(S.PLAN_TABS_COSTS), "tickets" to t(S.PLAN_TABS_TICKETS)),
                        tab, { tab = it }, tag = { "tab_$it" }
                    )
                    when (tab) {
                        "itinerary" -> ItineraryTab(items, plan?.timeZone, { itemSheet = Holder(it) }, { itemSheet = Holder(null) })
                        "costs" -> summary?.let { s ->
                            CostsTab(planId, costs, s, { cost, preset -> costSheet = Holder(cost) to preset }, vm::refreshSummary)
                        }
                        else -> TicketsTab(tickets, { viewing = it }, { ticketSheet = Holder(null) })
                    }
                }
            }
        }
        if (plan != null && load == DetailLoad.Ready && tab == "itinerary") {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp)
                    .shadow(8.dp, CircleShape, spotColor = RColors.Coral).clip(CircleShape).background(RColors.Coral)
                    .clickable(role = Role.Button) { itemSheet = Holder(null) }.testTag("item_add"),
                contentAlignment = Alignment.Center
            ) { Icon(Lucide.Plus, t(S.ITINERARY_ADD_ACTIVITY), tint = Color.White, modifier = Modifier.size(28.dp)) }
        }
    }

    val p = plan ?: return
    itemSheet?.let { h ->
        val last = items.lastOrNull()
        val day = last?.let { (it.endsAt ?: it.startsAt).take(10) } ?: Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()
        ItemFormSheet(planId, p.budgetCurrency, h.value, "${day}T09:00", { itemSheet = null }, { vm.itemsChanged(); toast(it) })
    }
    costSheet?.let { (h, preset) ->
        CostFormSheet(planId, p.budgetCurrency, summary?.tripDays ?: 1, h.value, preset, { costSheet = null }, vm::costsChanged)
    }
    viewing?.let { tk ->
        // Keep the viewer in sync with refreshed ticket data.
        val current = tickets.firstOrNull { it.id == tk.id } ?: tk
        TicketViewerDialog(
            planId, current, { viewing = null },
            onEdit = { viewing = null; ticketSheet = Holder(current) },
            onDeleted = { viewing = null; vm.ticketsChanged(); toast(S.TICKETS_DELETED) }
        )
    }
    ticketSheet?.let { h -> TicketFormSheet(planId, h.value, { ticketSheet = null }, { vm.ticketsChanged(); toast(it) }) }
    if (exportOpen) ExportSheet(planId, tickets.isNotEmpty()) { exportOpen = false }
}

@Composable
private fun HeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, tag: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(RColors.Surface).clickable(role = Role.Button, onClick = onClick).testTag(tag),
        contentAlignment = Alignment.Center
    ) { Icon(icon, description, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun HeaderCard(plan: PlanDto, summary: SummaryDto?, lang: String) {
    val type = RencanaType.current
    RCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            PlanCover(plan.coverType, plan.coverEmoji, plan.coverFileId, plan.coverColor, size = 72.dp, radius = 20.dp)
            Column(Modifier.weight(1f)) {
                Text(plan.name, style = type.title)
                Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Lucide.Calendar, null, tint = RColors.Muted, modifier = Modifier.size(14.dp))
                    Text(
                        formatPeriod(plan.startYear, plan.startMonth, plan.endYear, plan.endMonth, lang) ?: t(S.TRAVEL_NO_DATE),
                        style = type.caption
                    )
                }
            }
        }
        val budget = summary?.budgetAmount
        if (summary != null && budget != null) {
            val ratio = if (budget > 0) summary.grandTotal / budget else 0.0
            val over = ratio > 1
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    t(S.PLAN_PLANNED, "spent" to formatMoney(summary.grandTotal, summary.currency, lang), "budget" to formatMoney(budget, summary.currency, lang)),
                    style = type.caption.copy(color = if (over) RColors.Coral else RColors.Muted)
                )
                ProgressBar(ratio.coerceIn(0.0, 1.0).toFloat(), if (over) RColors.Coral else RColors.Primary)
            }
        }
    }
}

@Composable
internal fun ProgressBar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color(0xFFF3ECE3))) {
        Box(Modifier.fillMaxWidth(fraction).height(10.dp).clip(CircleShape).background(color))
    }
}
