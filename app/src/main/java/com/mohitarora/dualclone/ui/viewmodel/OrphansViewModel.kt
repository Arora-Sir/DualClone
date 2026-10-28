package com.mohitarora.dualclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.common.UiEventChannel
import com.mohitarora.dualclone.data.repository.OrphanRepository
import com.mohitarora.dualclone.domain.model.BulkItemResult
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.OrphanEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrphansUiState(
    val isScanning: Boolean = false,
    val items: List<OrphanEntry> = emptyList(),
    /** Keys are userId and package, joined by a slash. */
    val selected: Set<String> = emptySet(),
    /** User ids whose scan failed, so an empty list for them does not mean clean. */
    val failedUsers: Set<Int> = emptySet(),
    val hasScanned: Boolean = false,
    val purgeResults: List<BulkItemResult> = emptyList()
)

fun OrphanEntry.key(): String = "$userId/$packageName"

@HiltViewModel
class OrphansViewModel @Inject constructor(
    private val repository: OrphanRepository
) : ViewModel() {

    private val eventChannel = UiEventChannel()
    val events: Flow<UiEvent> = eventChannel.events

    private val _state = MutableStateFlow(OrphansUiState())
    val state: StateFlow<OrphansUiState> = _state.asStateFlow()

    fun scan() {
        if (_state.value.isScanning) return
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true, purgeResults = emptyList()) }
            val all = repository.detectAll()
            val items = all.values.flatMap { (it as? ListResult.Loaded)?.value.orEmpty() }
            val failed = all.filterValues { it is ListResult.Unavailable }.keys
            _state.value = OrphansUiState(
                items = items,
                // NOTE: Only clones DualClone created are pre selected. Apps installed natively in a profile never are.
                selected = items.filter { it.selectedByDefault }.map { it.key() }.toSet(),
                failedUsers = failed,
                hasScanned = true
            )
        }
    }

    fun toggle(entry: OrphanEntry) {
        _state.update { s ->
            val k = entry.key()
            s.copy(selected = if (k in s.selected) s.selected - k else s.selected + k)
        }
    }

    /** Removes the selected orphans per profile with the verified removal path. */
    fun purgeSelected() {
        val current = _state.value
        val chosen = current.items.filter { it.key() in current.selected }
        if (chosen.isEmpty() || current.isScanning) return
        viewModelScope.launch {
            _state.update { it.copy(isScanning = true) }
            val results = chosen.groupBy { it.userId }.flatMap { (userId, list) ->
                repository.purge(list.map { it.packageName }, userId)
            }
            eventChannel.send(UiEvent(Messages.bulkSummary("Removed", results.map { it.packageName to it.result })))
            val removed = results.filter { it.result is OpResult.Success }.map { it.packageName }.toSet()
            _state.update { s ->
                s.copy(
                    isScanning = false,
                    items = s.items.filterNot { it.packageName in removed && it in chosen },
                    selected = emptySet(),
                    purgeResults = results
                )
            }
        }
    }
}
