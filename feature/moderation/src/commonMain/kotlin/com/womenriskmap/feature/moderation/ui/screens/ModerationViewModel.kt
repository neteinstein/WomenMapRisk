package com.womenriskmap.feature.moderation.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.womenriskmap.core.domain.model.profileOrNull
import com.womenriskmap.core.domain.repository.SessionRepository
import com.womenriskmap.feature.moderation.domain.ChainLink
import com.womenriskmap.feature.moderation.domain.ModerationAction
import com.womenriskmap.feature.moderation.domain.ModerationCounters
import com.womenriskmap.feature.moderation.domain.ModerationItem
import com.womenriskmap.feature.moderation.domain.ModerationRepository
import com.womenriskmap.feature.moderation.domain.QueueKind
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PendingAction(val item: ModerationItem, val action: ModerationAction, val reason: String = "") {
    val canConfirm: Boolean get() = reason.isNotBlank()
}

data class ChainState(val item: ModerationItem, val links: List<ChainLink>? = null)

data class ModerationUiState(
    val isModerator: Boolean = false,
    val loading: Boolean = true,
    val counters: ModerationCounters? = null,
    val items: List<ModerationItem> = emptyList(),
    val tab: QueueKind = QueueKind.FLAGGED,
    val pending: PendingAction? = null,
    val chain: ChainState? = null,
    val acting: Boolean = false,
) {
    val visibleItems: List<ModerationItem> get() = items.filter { it.kind == tab }
}

sealed interface ModerationEffect {
    data object Done : ModerationEffect
    data class Error(val error: Throwable) : ModerationEffect
}

/** Spec §4 Ecrã 10 (internal use). The server re-checks the moderator role on every call. */
class ModerationViewModel(private val repo: ModerationRepository, private val sessions: SessionRepository) : ViewModel() {
    private val _state = MutableStateFlow(ModerationUiState())
    val state: StateFlow<ModerationUiState> = _state.asStateFlow()
    private val _effects = Channel<ModerationEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            sessions.session.collect { s ->
                val moderator = s.profileOrNull?.isModerator == true
                _state.update { it.copy(isModerator = moderator, loading = moderator) }
                if (moderator) refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val counters = async { repo.counters() }
            val queue = async { repo.queue() }
            val c = counters.await()
            val q = queue.await()
            _state.update { it.copy(loading = false, counters = c.getOrNull() ?: it.counters, items = q.getOrDefault(it.items)) }
            (c.exceptionOrNull() ?: q.exceptionOrNull())?.let { _effects.send(ModerationEffect.Error(it)) }
        }
    }

    fun onTab(kind: QueueKind) = _state.update { it.copy(tab = kind) }

    fun onAction(item: ModerationItem, action: ModerationAction) = _state.update { it.copy(pending = PendingAction(item, action)) }
    fun onReasonChange(reason: String) = _state.update { s -> s.copy(pending = s.pending?.copy(reason = reason.take(500))) }
    fun onDismissAction() = _state.update { it.copy(pending = null) }

    fun confirmAction() {
        val pending = _state.value.pending ?: return
        if (!pending.canConfirm) return
        viewModelScope.launch {
            _state.update { it.copy(acting = true) }
            repo.act(pending.item.report.id, pending.action, pending.reason)
                .onSuccess {
                    _state.update { it.copy(pending = null, acting = false) }
                    _effects.send(ModerationEffect.Done)
                    refresh()
                }
                .onFailure { e ->
                    _state.update { it.copy(acting = false) }
                    _effects.send(ModerationEffect.Error(e))
                }
        }
    }

    fun showChain(item: ModerationItem) {
        _state.update { it.copy(chain = ChainState(item)) }
        viewModelScope.launch {
            repo.inviterChain(item.report.id)
                .onSuccess { links -> _state.update { s -> s.copy(chain = s.chain?.copy(links = links)) } }
                .onFailure { _effects.send(ModerationEffect.Error(it)) }
        }
    }

    fun hideChain() = _state.update { it.copy(chain = null) }
}
