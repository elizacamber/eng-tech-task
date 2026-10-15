package com.userhub.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.userhub.domain.model.DataError
import com.userhub.domain.model.DataResult
import com.userhub.domain.model.User
import com.userhub.domain.repository.UsersResult
import com.userhub.domain.time.formatRelativeTime
import com.userhub.domain.usecase.DeleteUserUseCase
import com.userhub.domain.usecase.GetUsersUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

class UserFeedViewModel(
    private val getUsers: GetUsersUseCase,
    private val deleteUser: DeleteUserUseCase,
    private val appScope: CoroutineScope,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow<UserFeedUiState>(UserFeedUiState.Loading)
    val state: StateFlow<UserFeedUiState> = _state.asStateFlow()

    private val _errors = Channel<String>(Channel.BUFFERED)
    val errors: Flow<String> = _errors.receiveAsFlow()

    // Delete waiting for its Undo window. Only sent to the server when the window closes.
    // One at a time: starting a new delete sends the previous one right away.
    private var pendingDelete: PendingDelete? = null

    // Selected user's id: survives reloads and label changes. A deleted user can't be shown because it's no longer in the list.
    private val _selectedUserId = MutableStateFlow<Long?>(null)
    val selectedUserId: StateFlow<Long?> = _selectedUserId.asStateFlow()

    private data class PendingDelete(val index: Int, val user: UserUiModel)

    init {
        load()
    }

    fun load() {
        _state.value = UserFeedUiState.Loading
        viewModelScope.launch {
            when (val result = getUsers()) {
                is UsersResult.Success -> {
                    val now = clock.now()
                    val lastSyncLabel = result.lastSyncMillis?.let {
                        "Offline — last updated ${
                            formatRelativeTime(
                                Instant.fromEpochMilliseconds(it),
                                now
                            )
                        }"
                    }
                    _state.value = UserFeedUiState.Content(
                        result.users.toUiModels(result.createdAtMillis),
                        lastSyncLabel
                    )
                }

                UsersResult.NoInternet -> _state.value = UserFeedUiState.NoInternet
            }
        }
    }

    fun onUserCreated(user: User) {
        val content = _state.value as? UserFeedUiState.Content ?: return
        // the result can be delivered again if the screen is recreated before it's marked handled
        if (content.users.any { it.id == user.id }) return
        _state.value = content.copy(users = listOf(user.toNewUiModel(clock.now())) + content.users)
    }

    fun onDeleteConfirmed(user: UserUiModel) {
        val content = _state.value as? UserFeedUiState.Content ?: return
        val index = content.users.indexOf(user)
        if (index < 0) return
        // Free the slot: the previous user can no longer be undone, so send it now.
        pendingDelete?.let(::commit)
        pendingDelete = PendingDelete(index, user)
        _state.value = content.copy(users = content.users - user)
        // don't keep showing a user that was just deleted
        if (_selectedUserId.value == user.id) _selectedUserId.value = null
    }

    // Undo tapped: nothing was sent, so just put the row back.
    fun onUndo(user: UserUiModel) {
        val pending = takePending(user) ?: return
        restore(pending)
    }

    // Snackbar timed out or was dismissed: safe to delete on the server.
    fun onUndoWindowClosed(user: UserUiModel) {
        val pending = takePending(user) ?: return
        commit(pending)
    }

    override fun onCleared() {
        // the screen is gone. Nobody can tap Undo anymore
        pendingDelete?.let(::commit)
        pendingDelete = null
    }

    fun onUserSelected(user: UserUiModel) {
        _selectedUserId.value = user.id
    }

    fun onSelectionCleared() {
        _selectedUserId.value = null
    }

    // a late callback from an older snackbar must not touch a newer delete
    // Race scenario: deleting user B sends user A immediately, but A's snackbar only reports "dismissed" a frame later.
    // By then the slot holds B, so we match on user id to make sure A's late callback doesn't send or undo B.
    private fun takePending(user: UserUiModel): PendingDelete? {
        val pending = pendingDelete?.takeIf { it.user.id == user.id } ?: return null
        pendingDelete = null
        return pending
    }

    private fun commit(pending: PendingDelete) {
        // app scope, so the request isn't canceled when the screen closes
        appScope.launch {
            // the repository already counts a 404 (already gone) as success
            val result = deleteUser(pending.user.id)
            if (result is DataResult.Failure) {
                restore(pending)
                val message = if (result.error == DataError.Offline) {
                    "Couldn't delete ${pending.user.name}. Check your connection."
                } else {
                    "Couldn't delete ${pending.user.name}."
                }
                _errors.trySend(message)
            }
        }
    }

    // Can run on a background thread (from commit) while the UI also writes state,
    // so use update {} for an atomic read-modify-write.
    private fun restore(pending: PendingDelete) {
        _state.update { current ->
            val content = current as? UserFeedUiState.Content ?: return@update current
            if (pending.user in content.users) return@update current
            val users = content.users.toMutableList()
            users.add(pending.index.coerceAtMost(users.size), pending.user)
            content.copy(users = users)
        }
    }
}
