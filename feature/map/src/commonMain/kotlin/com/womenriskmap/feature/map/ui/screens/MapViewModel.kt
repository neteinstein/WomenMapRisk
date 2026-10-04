package com.womenriskmap.feature.map.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.BoundingBox
import com.womenriskmap.core.domain.model.FlagReason
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.PilotCity
import com.womenriskmap.core.domain.model.Place
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportFilter
import com.womenriskmap.core.domain.model.SessionState
import com.womenriskmap.core.domain.model.Zone
import com.womenriskmap.core.domain.model.profileOrNull
import com.womenriskmap.core.domain.repository.ConnectivityMonitor
import com.womenriskmap.core.domain.repository.GeocodingRepository
import com.womenriskmap.core.domain.repository.LocationProvider
import com.womenriskmap.core.domain.repository.PreferencesRepository
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.repository.SavedZoneRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.ReportPolicy
import com.womenriskmap.core.domain.usecase.LoadZonesUseCase
import com.womenriskmap.feature.map.domain.ConfirmReportUseCase
import com.womenriskmap.feature.map.domain.FlagReportUseCase
import com.womenriskmap.feature.map.domain.LoadZoneDetailUseCase
import com.womenriskmap.feature.map.domain.ZoneDetail
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Optional deep link into the map (e.g. from Saved): centre here and open the zone sheet. */
data class MapFocus(val point: GeoPoint, val zoneId: String?)

data class ZoneSheetState(
    val zoneId: String,
    val detail: ZoneDetail? = null,
    val loading: Boolean = true,
    val isSaved: Boolean = false,
    val pulseReportId: String? = null,
)

data class SearchState(val query: String = "", val results: List<Place> = emptyList(), val searching: Boolean = false)

data class MapUiState(
    val zones: List<Zone> = emptyList(),
    val loading: Boolean = false,
    val loadedOnce: Boolean = false,
    val isStale: Boolean = false,
    val isOnline: Boolean = true,
    val filter: ReportFilter = ReportFilter(),
    val filterDraft: ReportFilter? = null,
    val sheet: ZoneSheetState? = null,
    val search: SearchState = SearchState(),
    val session: SessionState = SessionState.Loading,
    val locationDenied: Boolean = false,
) {
    val isVisitor: Boolean get() = session !is SessionState.SignedIn
    val emailUnconfirmed: Boolean get() = session.profileOrNull?.let { !it.emailConfirmed } == true

    /** Spec §3 Ecrã 3 empty state: no data in the visible area. */
    val showEmptyState: Boolean get() = loadedOnce && !loading && zones.isEmpty() && sheet == null && search.results.isEmpty()
    val reportEnabled: Boolean get() = isOnline
    val user get() = session.profileOrNull
}

sealed interface MapEffect {
    data class MoveCamera(val point: GeoPoint, val zoom: Double) : MapEffect
    data class OpenReport(val near: GeoPoint?) : MapEffect
    data object VisitorPrompt : MapEffect
    data class Message(val kind: MapMessage) : MapEffect
    data class Error(val error: Throwable) : MapEffect
    data object ConfirmedHaptic : MapEffect
}

enum class MapMessage { CONFIRMED, FLAGGED, ZONE_SAVED, OFFLINE_NO_REPORT, CONFIRM_EMAIL }

class MapViewModel(
    private val loadZones: LoadZonesUseCase,
    private val loadZoneDetail: LoadZoneDetailUseCase,
    private val confirmReport: ConfirmReportUseCase,
    private val flagReport: FlagReportUseCase,
    private val sessions: SessionRepository,
    private val reports: ReportRepository,
    private val savedZones: SavedZoneRepository,
    private val geocoding: GeocodingRepository,
    private val location: LocationProvider,
    private val connectivity: ConnectivityMonitor,
    private val preferences: PreferencesRepository,
    private val clock: Clock,
    private val focus: MapFocus? = null,
    /** Polling fallback for realtime (spec §12: < 1 min). Null disables it (tests). */
    private val pollInterval: Duration? = 30.seconds,
) : ViewModel() {
    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    private val _effects = Channel<MapEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var viewport: BoundingBox? = null
    private var mapCenter: GeoPoint? = null
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { sessions.session.collect { s -> _state.update { it.copy(session = s) } } }
        viewModelScope.launch {
            connectivity.isOnline.collect { online ->
                val cameBack = online && !_state.value.isOnline
                _state.update { it.copy(isOnline = online) }
                if (cameBack) reload()
            }
        }
        // Spec §12: a published report appears on others' maps in < 1 min (realtime + polling fallback).
        viewModelScope.launch { reports.changes.collect { reload(debounce = true) } }
        pollInterval?.let { interval ->
            viewModelScope.launch {
                while (isActive) {
                    delay(interval)
                    if (_state.value.isOnline) reload(silent = true)
                }
            }
        }
        viewModelScope.launch {
            savedZones.savedZones.collect { list ->
                _state.update { s ->
                    s.copy(sheet = s.sheet?.let { sheet -> sheet.copy(isSaved = list.any { it.zoneId == sheet.zoneId }) })
                }
            }
        }
        viewModelScope.launch { centerInitially() }
    }

    /** Spec §7: location refused -> pilot city. Focus (from Saved) wins over everything. */
    private suspend fun centerInitially() {
        if (focus != null) {
            _effects.send(MapEffect.MoveCamera(focus.point, FOCUS_ZOOM))
            focus.zoneId?.let(::onZoneSelected)
            return
        }
        preferences.lastKnownLocation.value?.let { _effects.send(MapEffect.MoveCamera(it, USER_ZOOM)) }
        val here = location.currentLocation()
        if (here != null) {
            preferences.rememberLocation(here)
            _effects.send(MapEffect.MoveCamera(here, USER_ZOOM))
        } else if (preferences.lastKnownLocation.value == null) {
            _state.update { it.copy(locationDenied = !location.hasPermission()) }
            _effects.send(MapEffect.MoveCamera(PilotCity.center, PilotCity.DEFAULT_ZOOM))
        }
    }

    fun onViewportChanged(area: BoundingBox, center: GeoPoint) {
        viewport = area
        mapCenter = center
        reload(debounce = true)
    }

    private fun reload(debounce: Boolean = false, silent: Boolean = false) {
        val area = viewport ?: return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (debounce) delay(250.milliseconds)
            if (!silent) _state.update { it.copy(loading = true) }
            loadZones(area, _state.value.filter)
                .onSuccess { r -> _state.update { it.copy(zones = r.zones, isStale = r.isStale, loading = false, loadedOnce = true) } }
                .onFailure { e -> _state.update { it.copy(loading = false, loadedOnce = true, isStale = e is DomainException.Network) } }
            _state.value.sheet?.let { refreshSheet(it.zoneId) }
        }
    }

    // ---- Zone sheet (spec §4 Ecrã 4) ----

    fun onZoneSelected(zoneId: String) {
        val saved = savedZones.savedZones.value.any { it.zoneId == zoneId }
        _state.update { it.copy(sheet = ZoneSheetState(zoneId = zoneId, isSaved = saved)) }
        viewModelScope.launch { refreshSheet(zoneId) }
    }

    private suspend fun refreshSheet(zoneId: String) {
        loadZoneDetail(zoneId)
            .onSuccess { detail ->
                _state.update { s ->
                    if (s.sheet?.zoneId ==
                        zoneId
                    ) {
                        s.copy(sheet = s.sheet.copy(detail = detail, loading = false))
                    } else {
                        s
                    }
                }
            }
            .onFailure { e ->
                _state.update { s -> if (s.sheet?.zoneId == zoneId) s.copy(sheet = s.sheet.copy(loading = false)) else s }
                _effects.send(MapEffect.Error(e))
            }
    }

    fun onDismissZone() = _state.update { it.copy(sheet = null) }

    fun onConfirm(report: Report) {
        if (!guardContribution()) return
        viewModelScope.launch {
            confirmReport(report)
                .onSuccess {
                    // Optimistic +1 so the counter moves instantly; the realtime refresh reconciles.
                    _state.update { s ->
                        val sheet = s.sheet ?: return@update s
                        val detail = sheet.detail ?: return@update s
                        val updated = detail.reports.map {
                            if (it.id ==
                                report.id
                            ) {
                                it.copy(confirmations = it.confirmations + 1, confirmedByMe = true)
                            } else {
                                it
                            }
                        }
                        s.copy(sheet = sheet.copy(detail = detail.copy(reports = updated), pulseReportId = report.id))
                    }
                    _effects.send(MapEffect.ConfirmedHaptic)
                    _effects.send(MapEffect.Message(MapMessage.CONFIRMED))
                }
                .onFailure { _effects.send(MapEffect.Error(it)) }
        }
    }

    fun onFlag(report: Report, reason: FlagReason) {
        if (!guardContribution()) return
        viewModelScope.launch {
            flagReport(report, reason)
                .onSuccess { _effects.send(MapEffect.Message(MapMessage.FLAGGED)) }
                .onFailure { _effects.send(MapEffect.Error(it)) }
        }
    }

    fun onSaveZone() {
        val sheet = _state.value.sheet ?: return
        if (!guardContribution()) return
        val detail = sheet.detail ?: return
        viewModelScope.launch {
            savedZones.save(detail.zoneId, detail.center, detail.name ?: detail.zoneId)
                .onSuccess { _effects.send(MapEffect.Message(MapMessage.ZONE_SAVED)) }
                .onFailure { _effects.send(MapEffect.Error(it)) }
        }
    }

    fun canConfirm(report: Report): Boolean = ReportPolicy.canConfirm(report, _state.value.user)

    // ---- Report entry points (spec §5 Reportar) ----

    fun onReportTapped() = openReport(near = mapCenter)

    fun onReportHere() = openReport(near = _state.value.sheet?.detail?.center)

    private fun openReport(near: GeoPoint?) {
        if (!_state.value.isOnline) {
            _effects.trySend(MapEffect.Message(MapMessage.OFFLINE_NO_REPORT))
            return
        }
        if (!guardContribution()) return
        _effects.trySend(MapEffect.OpenReport(near))
    }

    /** Visitors get the sign-up prompt; unconfirmed emails get a nudge (spec §6 Contas). */
    private fun guardContribution(): Boolean {
        val s = _state.value
        when {
            s.isVisitor -> _effects.trySend(MapEffect.VisitorPrompt)
            s.emailUnconfirmed -> _effects.trySend(MapEffect.Message(MapMessage.CONFIRM_EMAIL))
            s.user?.isEnabled != true -> _effects.trySend(MapEffect.Error(DomainException.NotAllowed()))
            else -> return true
        }
        return false
    }

    // ---- Search (spec §4 Ecrã 3) ----

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(search = it.search.copy(query = query)) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(search = SearchState()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350.milliseconds)
            _state.update { it.copy(search = it.search.copy(searching = true)) }
            val results = geocoding.search(query, mapCenter).getOrDefault(emptyList())
            _state.update { it.copy(search = it.search.copy(results = results, searching = false)) }
        }
    }

    fun onPlaceSelected(place: Place) {
        _state.update { it.copy(search = SearchState()) }
        _effects.trySend(MapEffect.MoveCamera(place.location, FOCUS_ZOOM))
    }

    // ---- Filters (spec §4 Ecrã 6) ----

    fun onOpenFilters() = _state.update { it.copy(filterDraft = it.filter) }
    fun onFilterDraftChange(draft: ReportFilter) = _state.update { it.copy(filterDraft = draft) }
    fun onDismissFilters() = _state.update { it.copy(filterDraft = null) }

    fun onApplyFilters() {
        _state.update { it.copy(filter = it.filterDraft ?: it.filter, filterDraft = null) }
        reload()
    }

    fun onClearFilters() {
        _state.update { it.copy(filter = ReportFilter(), filterDraft = null) }
        reload()
    }

    // ---- Location (spec §4 Ecrã 3 "Centrar em mim") ----

    fun onCenterOnMe() {
        viewModelScope.launch {
            val here = location.currentLocation()
            if (here == null) {
                _state.update { it.copy(locationDenied = true) }
            } else {
                preferences.rememberLocation(here)
                _state.update { it.copy(locationDenied = false) }
                _effects.send(MapEffect.MoveCamera(here, USER_ZOOM))
            }
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) onCenterOnMe() else _state.update { it.copy(locationDenied = true) }
    }

    fun needsLocationPermission(): Boolean = !location.hasPermission()

    companion object {
        const val USER_ZOOM = 15.0
        const val FOCUS_ZOOM = 16.0
    }
}
