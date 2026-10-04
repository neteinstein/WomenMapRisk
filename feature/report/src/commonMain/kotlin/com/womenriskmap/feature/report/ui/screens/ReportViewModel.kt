package com.womenriskmap.feature.report.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.error.DomainException
import com.womenriskmap.core.domain.model.DayPeriod
import com.womenriskmap.core.domain.model.GeoPoint
import com.womenriskmap.core.domain.model.OccurredWhen
import com.womenriskmap.core.domain.model.PilotCity
import com.womenriskmap.core.domain.model.Place
import com.womenriskmap.core.domain.model.Report
import com.womenriskmap.core.domain.model.ReportDraft
import com.womenriskmap.core.domain.model.ReportStatus
import com.womenriskmap.core.domain.model.ReportType
import com.womenriskmap.core.domain.model.profileOrNull
import com.womenriskmap.core.domain.repository.GeocodingRepository
import com.womenriskmap.core.domain.repository.LocationProvider
import com.womenriskmap.core.domain.repository.ReportRepository
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.core.domain.rules.DescriptionGuard
import com.womenriskmap.core.domain.rules.EmergencyContacts
import com.womenriskmap.core.domain.rules.ReportPolicy
import com.womenriskmap.feature.report.domain.SubmitReportUseCase
import com.womenriskmap.feature.report.domain.defaultDayPeriod
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

enum class ReportStep { FORM, DONE }

data class ReportUiState(
    val location: GeoPoint = PilotCity.center,
    val locationReady: Boolean = false,
    val type: ReportType? = null,
    val occurredWhen: OccurredWhen = OccurredWhen.NOW,
    val dayPeriod: DayPeriod = DayPeriod.DAY,
    val description: String = "",
    val isEstablishment: Boolean = false,
    val findings: Set<DescriptionGuard.Finding> = emptySet(),
    val searchQuery: String = "",
    val searchResults: List<Place> = emptyList(),
    val submitting: Boolean = false,
    val error: Throwable? = null,
    val step: ReportStep = ReportStep.FORM,
    val submitted: Report? = null,
    val editingId: String? = null,
    val emergencyContacts: List<EmergencyContacts.Contact> = emptyList(),
) {
    val canSubmit: Boolean get() = type != null && !submitting && locationReady
    val isEdit: Boolean get() = editingId != null
    val showSupport: Boolean get() = step == ReportStep.DONE && submitted?.type == ReportType.ASSAULT
    val pendingReview: Boolean get() = submitted?.status == ReportStatus.PENDING
    val remainingChars: Int get() = ReportPolicy.MAX_DESCRIPTION_LENGTH - description.length
}

sealed interface ReportEffect {
    data class MoveCamera(val point: GeoPoint) : ReportEffect
    data object Closed : ReportEffect
}

class ReportViewModel(
    private val submitReport: SubmitReportUseCase,
    private val reports: ReportRepository,
    private val sessions: SessionRepository,
    private val location: LocationProvider,
    private val geocoding: GeocodingRepository,
    private val clock: Clock,
    near: GeoPoint? = null,
    editingId: String? = null,
) : ViewModel() {
    private val _state =
        MutableStateFlow(
            ReportUiState(location = near ?: PilotCity.center, dayPeriod = defaultDayPeriod(clock.now()), editingId = editingId),
        )
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    private val _effects = Channel<ReportEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch { if (editingId != null) loadForEdit(editingId) else locateDefault(near) }
    }

    /** Spec §4 Ecrã 5: default = current position; falls back to the map centre / pilot city. */
    private suspend fun locateDefault(near: GeoPoint?) {
        val here = location.currentLocation()
        val start = here ?: near ?: PilotCity.center
        _state.update { it.copy(location = start, locationReady = true) }
        _effects.send(ReportEffect.MoveCamera(start))
    }

    private suspend fun loadForEdit(id: String) {
        val report = reports.myReports().getOrNull()?.firstOrNull { it.id == id }
        if (report == null || !ReportPolicy.canEditOrDelete(report, clock.now())) {
            _state.update { it.copy(error = DomainException.EditWindowExpired(), locationReady = true) }
            return
        }
        _state.update {
            it.copy(
                location = report.location,
                locationReady = true,
                type = report.type,
                occurredWhen = report.occurredWhen,
                dayPeriod = report.dayPeriod,
                description = report.description.orEmpty(),
                isEstablishment = report.isEstablishment,
                findings = DescriptionGuard.inspect(report.description),
            )
        }
        _effects.send(ReportEffect.MoveCamera(report.location))
    }

    fun onLocationChanged(point: GeoPoint) = _state.update { it.copy(location = point, locationReady = true) }
    fun onTypeSelected(type: ReportType) = _state.update { it.copy(type = type, error = null) }
    fun onWhenSelected(value: OccurredWhen) = _state.update { it.copy(occurredWhen = value) }
    fun onDayPeriodSelected(value: DayPeriod) = _state.update { it.copy(dayPeriod = value) }
    fun onEstablishmentChange(value: Boolean) = _state.update { it.copy(isEstablishment = value) }

    fun onDescriptionChange(value: String) {
        val text = value.take(ReportPolicy.MAX_DESCRIPTION_LENGTH)
        _state.update { it.copy(description = text, findings = DescriptionGuard.inspect(text)) }
    }

    fun onSearchQueryChange(query: String) {
        _state.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(350.milliseconds)
            val results = geocoding.search(query, _state.value.location).getOrDefault(emptyList())
            _state.update { it.copy(searchResults = results) }
        }
    }

    fun onPlaceSelected(place: Place) {
        _state.update { it.copy(searchQuery = "", searchResults = emptyList(), location = place.location) }
        _effects.trySend(ReportEffect.MoveCamera(place.location))
    }

    fun submit() {
        val s = _state.value
        val type = s.type ?: return
        if (!s.canSubmit) return
        val draft = ReportDraft(s.location, type, s.occurredWhen, s.dayPeriod, s.description.ifBlank { null }, s.isEstablishment)
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            val result = if (s.editingId != null) reports.update(s.editingId, draft) else submitReport(draft)
            result
                .onSuccess { report ->
                    val country = sessions.session.value.profileOrNull?.country
                    _state.update {
                        it.copy(
                            submitting = false,
                            step = ReportStep.DONE,
                            submitted = report,
                            emergencyContacts = if (report.type ==
                                ReportType.ASSAULT
                            ) {
                                EmergencyContacts.forCountry(country)
                            } else {
                                emptyList()
                            },
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(submitting = false, error = e) } }
        }
    }

    /** Spec §4 Ecrã 5: delete within 24 h (edit mode only). */
    fun delete() {
        val id = _state.value.editingId ?: return
        viewModelScope.launch {
            reports.delete(id)
                .onSuccess { _effects.send(ReportEffect.Closed) }
                .onFailure { e -> _state.update { it.copy(error = e) } }
        }
    }
}
